package one.oktw.muzeipixivsource.service

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import android.preference.PreferenceManager
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.content.edit
import com.crashlytics.android.Crashlytics
import com.google.android.apps.muzei.api.Artwork
import com.google.android.apps.muzei.api.MuzeiArtSource
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource
import com.google.firebase.analytics.FirebaseAnalytics
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RECOMMEND
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_NUMBER
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_ORIGIN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_SAFE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_SIZE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_VIEW
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_MUZEI_CHANGE_INTERVAL
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_ACCESS_TOKEN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_DEVICE_TOKEN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_REFRESH_TOKEN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_USER_ID
import one.oktw.muzeipixivsource.pixiv.DataImageInfo
import one.oktw.muzeipixivsource.pixiv.Pixiv
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory
import java.io.File
import java.lang.System.currentTimeMillis

class MuzeiSource : RemoteMuzeiArtSource("Pixiv") {
    private lateinit var preference: SharedPreferences
    private lateinit var analytics: FirebaseAnalytics

    companion object {
        private const val KEY_LAST_IMAGE = "last_image"
    }

    override fun onCreate() {
        super.onCreate()

        PreferenceManager.setDefaultValues(this, R.xml.prefragment, false)
        preference = PreferenceManager.getDefaultSharedPreferences(this)

        analytics = FirebaseAnalytics.getInstance(this)

        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK)
    }

    override fun onTryUpdate(reason: Int) {
        // Check has network and not background connect restrict
        (getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager).run {
            if (activeNetworkInfo?.isConnected != true) throw RetryException()
            if (SDK_INT >= N && restrictBackgroundStatus == RESTRICT_BACKGROUND_STATUS_ENABLED) throw RetryException()
        }

        // only update token on auto change
        if (reason != MuzeiArtSource.UPDATE_REASON_USER_NEXT) tryUpdateToken()

        val token: String? = preference.getString(KEY_PIXIV_ACCESS_TOKEN, null)

        val pixiv = Pixiv(
            token = token,
            originImage = preference.getBoolean(KEY_FETCH_ORIGIN, false),
            safety = preference.getBoolean(KEY_FILTER_SAFE, true),
            size = preference.getBoolean(KEY_FILTER_SIZE, true),
            number = preference.getInt(KEY_FETCH_NUMBER, 30),
            minView = preference.getInt(KEY_FILTER_VIEW, 0),
            minBookmark = preference.getInt(KEY_FILTER_BOOKMARK, 0),
            savePath = cacheDir
        )

        try {
            when (if (token == null) -1 else preference.getString(KEY_FETCH_MODE, "0").toInt()) {
                FETCH_MODE_FALLBACK -> pixiv.getFallback().let(::publish)
                FETCH_MODE_RECOMMEND -> pixiv.getRecommend().let(::publish)

                FETCH_MODE_RANKING -> pixiv.getRanking(
                    RankingCategory.valueOf(preference.getString(KEY_FETCH_MODE_RANKING, RankingCategory.Monthly.name))
                ).let(::publish)

                FETCH_MODE_BOOKMARK -> pixiv.getBookmark(
                    preference.getInt(KEY_PIXIV_USER_ID, -1),
                    preference.getBoolean(KEY_FETCH_MODE_BOOKMARK, false)
                ).let(::publish)
            }
        } catch (e: Exception) {
            // TODO better except handle
            Crashlytics.logException(e)

            // try update token then get fallback
            tryUpdateToken()

            try {
                pixiv.getFallback().let(::publish)
            } catch (e: Exception) {
                Crashlytics.logException(e)

                throw RetryException()
            }
        }

        // schedule next update
        preference.getString(KEY_MUZEI_CHANGE_INTERVAL, "60").let {

            if (it == "never") return

            scheduleUpdate(currentTimeMillis() + it.toInt() * 60000)
        }

        // log event
        analytics.logEvent("fetch_image", null)
    }

    private fun tryUpdateToken() {
        analytics.logEvent("update_token", null)

        try {
            PixivOAuth.refresh(
                preference.getString(KEY_PIXIV_DEVICE_TOKEN, null) ?: return,
                preference.getString(KEY_PIXIV_REFRESH_TOKEN, null) ?: return
            ).response?.let { PixivOAuth.save(preference, it) }
        } catch (e: Exception) {
            Crashlytics.logException(e)
        }
    }

    private fun publish(data: DataImageInfo) {
        val uri = data.file?.let { getUriForFile(this, "one.oktw.fileprovider", it) } ?: throw RetryException()

        grantUriPermission("net.nurik.roman.muzei", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        Artwork.Builder()
            .title(data.title).byline(data.author)
            .imageUri(uri)
            .viewIntent(Intent(Intent.ACTION_VIEW, data.url))
            .build()
            .let(::publishArtwork)

        preference.getString(KEY_LAST_IMAGE, null)?.let { File(it).delete() } // delete old image
        preference.edit { putString(KEY_LAST_IMAGE, data.file!!.absolutePath) } // save image path
    }
}
