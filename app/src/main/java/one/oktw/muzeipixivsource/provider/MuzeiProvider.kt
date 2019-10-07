package one.oktw.muzeipixivsource.provider

import android.content.SharedPreferences
import android.util.Log
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.google.android.apps.muzei.api.UserCommand
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okio.Pipe
import okio.buffer
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RECOMMEND
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_CLEANUP
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MIRROR
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_NUMBER
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_ORIGIN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_RANDOM
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_SAFE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_SIZE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_VIEW
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_ACCESS_TOKEN
import one.oktw.muzeipixivsource.pixiv.Pixiv
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.Monthly
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.valueOf
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.util.AppUtil
import org.jsoup.Jsoup
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit.SECONDS

class MuzeiProvider : MuzeiArtProvider() {
    companion object {
        private const val COMMAND_FETCH = 1
    }

    private val httpClient = OkHttpClient()
    private lateinit var preference: SharedPreferences
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(): Boolean {
        PreferenceManager.setDefaultValues(context, R.xml.prefragment, true)

        preference = PreferenceManager.getDefaultSharedPreferences(context)
        analytics = FirebaseAnalytics.getInstance(context!!)

        return super.onCreate()
    }

    override fun onLoadRequested(initial: Boolean) {
        runBlocking { updateToken() }

        val token: String? = preference.getString(KEY_PIXIV_ACCESS_TOKEN, null)
        val fallback = preference.getBoolean(KEY_FETCH_FALLBACK, false)
        val pixiv = Pixiv(token = token, number = preference.getInt(KEY_FETCH_NUMBER, 30))

        try {
            when (if (token == null) FETCH_MODE_FALLBACK else preference.getString(KEY_FETCH_MODE, "0")!!.toInt()) {
                FETCH_MODE_FALLBACK -> pixiv.getFallback().let(::publish)
                FETCH_MODE_RECOMMEND -> pixiv.getRecommend().let(::publish)
                FETCH_MODE_RANKING -> pixiv.getRanking(
                    valueOf(preference.getString(KEY_FETCH_MODE_RANKING, Monthly.name)!!)
                ).let(::publish)

                FETCH_MODE_BOOKMARK -> pixiv.getBookmark(
                    preference.getInt(SettingsFragment.KEY_PIXIV_USER_ID, -1),
                    preference.getBoolean(SettingsFragment.KEY_FETCH_MODE_BOOKMARK, false)
                ).let(::publish)
            }
        } catch (e1: Exception) {
            // TODO better except handle
            Log.e("fetch", "fetch update error", e1)
            Crashlytics.logException(e1)

            try {
                if (fallback) pixiv.getFallback().let(::publish) else throw e1
            } catch (e2: Exception) {
                if (fallback) Log.e("fetch", "fetch update fallback error", e2)

                if (e1 != e2) Crashlytics.logException(e2)
                throw e2
            }
        }
    }

    override fun openFile(artwork: Artwork): InputStream {
        val mirror = preference.getString(KEY_FETCH_MIRROR, "")
        val httpClient = if (mirror.isNullOrBlank()) AppUtil.httpClient else httpClient // Use normal client download image from mirror
        val uri = artwork.persistentUri!!.let { if (mirror.isNullOrBlank()) it.toString() else it.toString().replace(it.host!!, mirror) }
        val stream = Pipe(DEFAULT_BUFFER_SIZE.toLong()).apply {
            source.timeout().timeout(10, SECONDS)
            sink.timeout().timeout(10, SECONDS)
        }

        Request.Builder()
            .url(uri)
            .header("Referer", "https://app-api.pixiv.net/")
            .build()
            .let(httpClient::newCall)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                override fun onResponse(call: Call, response: Response) {
                    response.body?.source()?.use { source -> stream.sink.use { source.readAll(it) } }
                }
            })

        return stream.source.buffer().inputStream()
    }

    override fun getCommands(artwork: Artwork) = mutableListOf(
        UserCommand(COMMAND_FETCH, context?.getString(R.string.button_update))
    )

    override fun onCommand(artwork: Artwork, id: Int) = when (id) {
        COMMAND_FETCH -> onLoadRequested(false)
        else -> Unit
    }

    private suspend fun updateToken() {
        analytics.logEvent("update_token", null)

        try {
            PixivOAuth.refresh(
                preference.getString(SettingsFragment.KEY_PIXIV_DEVICE_TOKEN, null) ?: return,
                preference.getString(SettingsFragment.KEY_PIXIV_REFRESH_TOKEN, null) ?: return
            ).response?.let { PixivOAuth.save(preference, it) }
        } catch (e: Exception) {
            Log.e("update_token", "update token error", e)
            Crashlytics.logException(e)
        }
    }

    private fun publish(list: ArrayList<Illust>) {
        val cleanHistory = preference.getBoolean(KEY_FETCH_CLEANUP, true)
        val filterNSFW = preference.getBoolean(KEY_FILTER_SAFE, true)
        val random = preference.getBoolean(KEY_FETCH_RANDOM, false)
        val filterSize = preference.getInt(KEY_FILTER_SIZE, 0)
        val originImage = if (filterSize > 1200) true else preference.getBoolean(KEY_FETCH_ORIGIN, false)
        val minView = preference.getInt(KEY_FILTER_VIEW, 0)
        val minBookmark = preference.getInt(KEY_FILTER_BOOKMARK, 0)

        val artworkList = ArrayList<Artwork>()

        list.forEach {
            if (filterNSFW && it.sanityLevel >= 4) return@forEach
            if (filterSize > it.height && filterSize > it.width) return@forEach
            if (minView > it.totalView || minBookmark > it.totalBookmarks) return@forEach

            if (it.pageCount > 1) {
                it.metaPages.forEachIndexed { index, image ->
                    val imageUrl = if (originImage) image.imageUrls.original else image.imageUrls.large?.replace("/c/600x1200_90", "")

                    Artwork.Builder()
                        .title(it.title)
                        .byline(it.user.name)
                        .attribution(Jsoup.parse(it.caption).text())
                        .token("${it.id}_$index")
                        .webUri("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=${it.id}".toUri())
                        .persistentUri(imageUrl?.toUri())
                        .build()
                        .let(artworkList::add)
                }
            } else {
                val imageUrl = if (originImage) {
                    it.metaSinglePage.original_image_url
                } else {
                    it.image_urls.large?.replace("/c/600x1200_90", "")
                }?.toUri()

                Artwork.Builder()
                    .title(it.title)
                    .byline(it.user.name)
                    .attribution(Jsoup.parse(it.caption).text())
                    .token(it.id.toString())
                    .webUri("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=${it.id}".toUri())
                    .persistentUri(imageUrl)
                    .build()
                    .let(artworkList::add)
            }
        }

        if (cleanHistory) delete(contentUri, null, null)
        if (random) artworkList.shuffle()

        artworkList.forEach { addArtwork(it) }
    }
}
