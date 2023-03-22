package one.oktw.muzeipixivsource.provider

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.util.Log
import android.webkit.URLUtil
import androidx.core.app.RemoteActionCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import okio.Pipe
import okio.buffer
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.ShareActivity
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RECOMMEND
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_CLEANUP
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_DIRECT
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MIRROR
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_NUMBER
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_ORIGIN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_RANDOM
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_ILLUST
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_SAFE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_SIZE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_VIEW
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_ACCESS_TOKEN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_REFRESH_TOKEN
import one.oktw.muzeipixivsource.hack.MirrorOutputStream
import one.oktw.muzeipixivsource.pixiv.Pixiv
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.Monthly
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.valueOf
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustTypes.ILLUST
import one.oktw.muzeipixivsource.util.HttpUtils.httpClient
import one.oktw.muzeipixivsource.util.getPendingIntentFlag
import org.jsoup.Jsoup
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.SECONDS

class MuzeiProvider : MuzeiArtProvider(), CoroutineScope by CoroutineScope(CoroutineName("MuzeiProvider")) {
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val downloadLock = ConcurrentHashMap.newKeySet<Long>()
    private lateinit var preference: SharedPreferences
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(): Boolean {
        PreferenceManager.setDefaultValues(requireContext(), R.xml.prefragment, true)

        preference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        analytics = FirebaseAnalytics.getInstance(context!!)

        return super.onCreate()
    }

    override fun onLoadRequested(initial: Boolean) {
        runBlocking { updateToken() }

        val token: String? = preference.getString(KEY_PIXIV_ACCESS_TOKEN, null)
        val fallback = preference.getBoolean(KEY_FETCH_FALLBACK, false)
        val pixiv = Pixiv(token, preference.getInt(KEY_FETCH_NUMBER, 30), preference.getBoolean(KEY_FETCH_DIRECT, false))

        try {
            when (if (token == null) FETCH_MODE_FALLBACK else preference.getString(KEY_FETCH_MODE, "0")!!.toInt()) {
                FETCH_MODE_FALLBACK -> pixiv.getFallback().let(::publish)
                FETCH_MODE_RECOMMEND -> pixiv.getRecommend().let(::publish)
                FETCH_MODE_RANKING -> pixiv.getRanking(valueOf(preference.getString(KEY_FETCH_MODE_RANKING, Monthly.name)!!)).let(::publish)
                FETCH_MODE_BOOKMARK -> pixiv.getBookmark(
                    preference.getInt(SettingsFragment.KEY_PIXIV_USER_ID, -1),
                    preference.getBoolean(SettingsFragment.KEY_FETCH_MODE_BOOKMARK, false)
                ).let(::publish)
            }
        } catch (e1: Exception) {
            // TODO better except handle
            Log.e("fetch", "fetch update error: ${e1.printStackTrace()}", e1)
            crashlytics.recordException(e1)

            if (!fallback) throw e1

            try {
                pixiv.getFallback().let(::publish)
            } catch (e2: Exception) {
                Log.e("fetch", "fetch update fallback error", e2)
                crashlytics.recordException(e2)

                throw e2
            }
        }
    }

    override fun openFile(artwork: Artwork): InputStream {
        val locked = downloadLock.add(artwork.id)
        val mirror = preference.getString(KEY_FETCH_MIRROR, "")!!
            .let { if (it.isBlank() || URLUtil.isNetworkUrl(it)) it else "https://$it" }
            .let(Uri::parse)
        val uri = artwork.persistentUri!!.let { if (mirror.authority.isNullOrBlank()) it else it.buildUpon().authority(mirror.authority).build() }
        val stream = Pipe(DEFAULT_BUFFER_SIZE.toLong()).apply {
            source.timeout().timeout(30, SECONDS)
            sink.timeout().timeout(30, SECONDS)
        }

        Request.Builder()
            .url(uri.toString())
            .header("Referer", "https://app-api.pixiv.net/")
            .build()
            .let(httpClient::newCall)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (locked) downloadLock.remove(artwork.id)
                    stream.cancel() // Throw IOException to reader
                    crashlytics.recordException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.code in 400..499) onInvalidArtwork(artwork)

                    try {
                        response.body?.source()?.use { source -> source.readAll(stream.sink) }
                    } catch (e: IOException) {
                        stream.cancel()
                        crashlytics.recordException(e)
                    } finally {
                        stream.sink.closeQuietly()
                        if (locked) downloadLock.remove(artwork.id)
                    }
                }
            })

        return stream.source.buffer().inputStream()
    }

    // Fully async, don't blocking Binder thread.
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") return super.openFile(uri, mode)

        val artwork = query(uri, null, null, null, null).use { data ->
            if (!data.moveToFirst()) {
                throw FileNotFoundException("Could not get persistent uri for $uri")
            }
            Artwork.fromCursor(data)
        }
        if (!isArtworkValid(artwork)) {
            onInvalidArtwork(artwork)
            throw SecurityException("Artwork $artwork was marked as invalid")
        }
        val (output, input) = ParcelFileDescriptor.createReliablePipe()
        when {
            downloadLock.contains(artwork.id) -> launch(Dispatchers.IO) {
                while (downloadLock.contains(artwork.id)) delay(10) // Wait download complete.

                val pipe = ParcelFileDescriptor.AutoCloseOutputStream(input)
                try {
                    AutoCloseInputStream(super.openFile(uri, mode)).use { it.copyTo(pipe) }
                } catch (e: IOException) {
                    input.closeWithError(e.toString())
                } finally {
                    pipe.closeQuietly()
                }
            }

            artwork.data.exists() -> return ParcelFileDescriptor.open(artwork.data, ParcelFileDescriptor.parseMode(mode))
            else -> launch(Dispatchers.IO) {
                artwork.data.parentFile?.apply {
                    if (!exists() && !mkdirs()) input.closeWithError("Unable to create directory $this for $artwork")
                }

                val pipe = ParcelFileDescriptor.AutoCloseOutputStream(input)
                val mirror = MirrorOutputStream(FileOutputStream(artwork.data), pipe)
                try {
                    openFile(artwork).use { input -> input.copyTo(mirror) }
                } catch (e: Exception) {
                    if (e !is IOException) {
                        if (Log.isLoggable("MuzeiArtProvider", Log.INFO)) {
                            Log.i("MuzeiArtProvider", "Unable to open artwork $artwork for $uri", e)
                        }
                        onInvalidArtwork(artwork)
                    }
                    // Delete the file in cases of an error so that we will try again from scratch next time.
                    if (artwork.data.exists() && !artwork.data.delete()) {
                        if (Log.isLoggable("MuzeiArtProvider", Log.INFO)) {
                            Log.i("MuzeiArtProvider", "Error deleting partially downloaded file after error", e)
                        }
                    }
                    input.closeWithError("Could not download artwork $artwork for $uri: ${e.message}")
                } finally {
                    mirror.closeQuietly()
                }
            }
        }
        return output
    }

    // New version command
    override fun getCommandActions(artwork: Artwork): List<RemoteActionCompat> {
        val context = context!!
        return listOf(
            RemoteActionCompat(
                IconCompat.createWithResource(context, R.drawable.ic_open),
                context.getString(R.string.button_open),
                artwork.title ?: "",
                PendingIntent.getActivity(context, artwork.id.toInt(), Intent(Intent.ACTION_VIEW, artwork.webUri), getPendingIntentFlag())
            ).apply { setShouldShowIcon(false) },
            RemoteActionCompat(
                IconCompat.createWithResource(context, R.drawable.ic_share),
                context.getString(R.string.button_share),
                artwork.title ?: "",
                PendingIntent.getActivity(
                    context,
                    artwork.id.toInt(),
                    Intent(context, ShareActivity::class.java)
                        .putExtra(ShareActivity.INTENT_SHARE_TITLE, artwork.title)
                        .putExtra(ShareActivity.INTENT_SHARE_TEXT, getShareText(artwork))
                        .putExtra(ShareActivity.INTENT_SHARE_FILENAME, artwork.persistentUri!!.pathSegments.last())
                        .putExtra(ShareActivity.INTENT_SHARE_FILE_URI, ContentUris.withAppendedId(contentUri, artwork.id))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    getPendingIntentFlag()
                )
            )
            // TODO save image
        )
    }

    private suspend fun updateToken() {
        analytics.logEvent("update_token", null)

        try {
            val res = PixivOAuth.refresh(preference.getString(KEY_PIXIV_REFRESH_TOKEN, null) ?: return, preference.getBoolean(KEY_FETCH_DIRECT, false))
            if (!res.has_error) res.response?.let { PixivOAuth.save(preference, it) } else PixivOAuth.logout(preference)
        } catch (e: Exception) {
            Log.e("update_token", "update token error", e)
            crashlytics.recordException(e)
        }
    }

    private fun publish(list: List<Illust>) {
        val number = preference.getInt(KEY_FETCH_NUMBER, 30)
        val cleanHistory = preference.getBoolean(KEY_FETCH_CLEANUP, true)
        val filterNSFW = preference.getBoolean(KEY_FILTER_SAFE, true)
        val filterIllust = preference.getBoolean(KEY_FILTER_ILLUST, true)
        val random = preference.getBoolean(KEY_FETCH_RANDOM, false)
        val filterSize = preference.getInt(KEY_FILTER_SIZE, 0)
        val originImage = if (filterSize > 1200) true else preference.getBoolean(KEY_FETCH_ORIGIN, false)
        val minView = preference.getInt(KEY_FILTER_VIEW, 0)
        val minBookmark = preference.getInt(KEY_FILTER_BOOKMARK, 0)

        val artworkList = ArrayList<Artwork>()

        list.forEach {
            if (filterNSFW && it.sanityLevel >= 4) return@forEach
            if (filterIllust && it.type != ILLUST) return@forEach
            if (filterSize > it.height && filterSize > it.width) return@forEach
            if (minView > it.totalView || minBookmark > it.totalBookmarks) return@forEach

            if (it.pageCount > 1) {
                it.metaPages.forEachIndexed { index, image ->
                    val imageUrl = if (originImage) {
                        image.imageUrls.original
                    } else {
                        image.imageUrls.large?.replace("/c/600x1200_90", "")
                    } ?: return@forEachIndexed

                    Artwork.Builder()
                        .title(it.title)
                        .byline(it.user.name)
                        .attribution(Jsoup.parse(it.caption).text())
                        .token("${it.id}_$index")
                        .webUri("https://www.pixiv.net/artworks/${it.id}".toUri())
                        .persistentUri(imageUrl.toUri())
                        .build()
                        .let(artworkList::add)
                }
            } else {
                val imageUrl = if (originImage) {
                    it.metaSinglePage.original_image_url
                } else {
                    it.image_urls.large?.replace("/c/600x1200_90", "")
                }?.toUri() ?: return@forEach

                Artwork.Builder()
                    .title(it.title)
                    .byline(it.user.name)
                    .attribution(Jsoup.parse(it.caption).text())
                    .token(it.id.toString())
                    .webUri("https://www.pixiv.net/artworks/${it.id}".toUri())
                    .persistentUri(imageUrl)
                    .build()
                    .let(artworkList::add)
            }
        }

        if (cleanHistory) delete(contentUri, null, null)
        if (random) artworkList.shuffle()

        addArtwork(artworkList.subList(0, artworkList.size.coerceAtMost(number)))
    }

    private fun getShareText(artwork: Artwork) = "${artwork.title} | ${artwork.byline} #pixiv ${artwork.webUri}"
}
