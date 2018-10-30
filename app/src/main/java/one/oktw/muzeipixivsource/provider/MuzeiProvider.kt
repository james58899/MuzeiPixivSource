package one.oktw.muzeipixivsource.provider

import android.content.SharedPreferences
import android.util.Log
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RECOMMEND
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_CLEANUP
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_NUMBER
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_ORIGIN
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
import org.jsoup.Jsoup
import java.io.InputStream

class MuzeiProvider : MuzeiArtProvider() {
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
        val fallback = preference.getBoolean(KEY_FETCH_FALLBACK, true)
        val pixiv = Pixiv(token = token, number = preference.getInt(KEY_FETCH_NUMBER, 30), fallback = fallback)

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
                Log.e("fetch", "fetch update fallback error", e2)

                if (e1 != e2) Crashlytics.logException(e2)
                throw e2
            }
        }
    }

    override fun openFile(artwork: Artwork): InputStream {
        return Request.Builder()
            .url(artwork.persistentUri.toString())
            .header("Referer", "https://app-api.pixiv.net/")
            .build()
            .let(httpClient::newCall)
            .execute()
            .body()!!.byteStream()
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
        var first = preference.getBoolean(KEY_FETCH_CLEANUP, true)
        val originImage = preference.getBoolean(KEY_FETCH_ORIGIN, false)
        val filterNSFW = preference.getBoolean(KEY_FILTER_SAFE, true)
        val filterSize = preference.getBoolean(KEY_FILTER_SIZE, true)
        val minView = preference.getInt(KEY_FILTER_VIEW, 0)
        val minBookmark = preference.getInt(KEY_FILTER_BOOKMARK, 0)

        list.forEach {
            if (filterNSFW && it.sanityLevel >= 4) return@forEach
            if (filterSize && it.height < 1000) return@forEach
            if (minView > it.totalView || minBookmark > it.totalBookmarks) return@forEach

            if (it.pageCount > 1) {
                it.metaPages.forEachIndexed { index, image ->
                    val imageUrl = if (originImage) {
                        image.imageUrls.original
                    } else {
                        image.imageUrls.large?.replace("/c/600x1200_90", "")
                    }?.toUri()

                    Artwork.Builder()
                        .title(it.title)
                        .byline(it.user.name)
                        .attribution(Jsoup.parse(it.caption).text())
                        .token("${it.id}_$index")
                        .webUri("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=${it.id}".toUri())
                        .persistentUri(imageUrl)
                        .build()
                        .let { artwork ->
                            if (first) {
                                setArtwork(artwork)
                                first = false
                            } else {
                                addArtwork(artwork)
                            }
                        }
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
                    .let { artwork ->
                        if (first) {
                            setArtwork(artwork)
                            first = false
                        } else {
                            addArtwork(artwork)
                        }
                    }
            }
        }
    }
}