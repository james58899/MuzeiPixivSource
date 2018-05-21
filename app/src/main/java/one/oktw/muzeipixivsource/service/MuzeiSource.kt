package one.oktw.muzeipixivsource.service

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.FileProvider.getUriForFile
import android.support.v4.net.ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_ENABLED
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
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_FILTER_SIZE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_NUMBER
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_ORIGIN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_SAFE
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
        val trace = Bundle()

        // Check has background connect restrict
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            (getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager).run {
                if (restrictBackgroundStatus == RESTRICT_BACKGROUND_STATUS_ENABLED) throw RetryException()
            }
        }

        // only update token on auto change
        if (reason != MuzeiArtSource.UPDATE_REASON_USER_NEXT) tryUpdateToken()

        val token: String? = preference.getString(KEY_PIXIV_ACCESS_TOKEN, null)
        val originImage = preference.getBoolean(KEY_FETCH_ORIGIN, false)
        val safety = preference.getBoolean(KEY_FETCH_SAFE, true)
        val size = preference.getBoolean(KEY_FETCH_FILTER_SIZE, true)
        val number = preference.getInt(KEY_FETCH_NUMBER, 30)
        val mode = if (token == null) -1 else preference.getString(KEY_FETCH_MODE, "0").toInt()

        val pixiv = Pixiv(
                token = token,
                originImage = originImage,
                safety = safety,
                size = size,
                number = number,
                savePath = cacheDir
        )

        trace.apply {
            putBoolean("origin_image", originImage)
            putBoolean("safety", safety)
            putBoolean("size", size)
            putInt("mode", mode)
            putInt("number", number)
            putBoolean("success", true)
        }

        try {
            when (mode) {
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

            trace.putBoolean("success", false)

            // try update token then get fallback
            tryUpdateToken()

            try {
                pixiv.getFallback().let(::publish)
                trace.putBoolean("fallback", true)
            } catch (e: Exception) {
                Crashlytics.logException(e)

                trace.putBoolean("fallback", false)

                throw RetryException()
            }
        }

        // schedule next update
        preference.getString(KEY_MUZEI_CHANGE_INTERVAL, "60").let {
            trace.putString("interval", it)

            if (it == "never") return

            scheduleUpdate(currentTimeMillis() + it.toInt() * 60000)
        }

        // log event
        analytics.logEvent("fetch_image", trace)
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
