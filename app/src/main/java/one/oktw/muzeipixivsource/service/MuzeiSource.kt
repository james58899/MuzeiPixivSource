package one.oktw.muzeipixivsource.service

import android.content.Intent
import android.content.SharedPreferences
import android.support.v4.content.FileProvider.getUriForFile
import android.support.v7.preference.PreferenceManager
import android.util.Log
import com.google.android.apps.muzei.api.Artwork
import com.google.android.apps.muzei.api.MuzeiArtSource
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RECOMMEND
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_FILTER_SIZE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_RANKING
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

    companion object {
        private const val MINUTE = 60000
        private const val KEY_LAST_IMAGE = "last_image"
    }

    override fun onCreate() {
        super.onCreate()

        PreferenceManager.setDefaultValues(this, R.xml.prefragment, false)
        preference = PreferenceManager.getDefaultSharedPreferences(this)

        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK)
    }

    override fun onTryUpdate(reason: Int) {
        // only update token on auto change
        if (reason != MuzeiArtSource.UPDATE_REASON_USER_NEXT) updateToken()

        val token: String? = preference.getString(KEY_PIXIV_ACCESS_TOKEN, null)
        val mode = if (token == null) -1 else preference.getString(KEY_FETCH_MODE, "0").toInt()

        val pixiv = Pixiv(
            token = token,
            originImage = preference.getBoolean(KEY_FETCH_ORIGIN, false),
            safety = preference.getBoolean(KEY_FETCH_SAFE, true),
            size = preference.getBoolean(KEY_FETCH_FILTER_SIZE, true),
            savePath = cacheDir
        )

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
            Log.e("pixiv", e.message, e)

            // try update token then get fallback
            updateToken()
            pixiv.getFallback().let(::publish)
        }

        // schedule next update
        scheduleUpdate(currentTimeMillis() + preference.getString(KEY_MUZEI_CHANGE_INTERVAL, "60").toInt() * MINUTE)
    }

    private fun updateToken() {
        PixivOAuth.refresh(
            preference.getString(KEY_PIXIV_DEVICE_TOKEN, null) ?: return,
            preference.getString(KEY_PIXIV_REFRESH_TOKEN, null) ?: return
        ).response?.let { PixivOAuth.save(preference, it) }
    }

    private fun publish(data: DataImageInfo) {
        val uri = data.file?.let { getUriForFile(applicationContext, "one.oktw.fileprovider", it) }
                ?: throw RemoteMuzeiArtSource.RetryException()

        preference.getString(KEY_LAST_IMAGE, null)?.let { File(it).delete() } // delete old image
        preference.edit().putString(KEY_LAST_IMAGE, data.file!!.absolutePath).apply() // save image path

        applicationContext.grantUriPermission("net.nurik.roman.muzei", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        Artwork.Builder()
            .title(data.title).byline(data.author)
            .imageUri(uri)
            .viewIntent(Intent(Intent.ACTION_VIEW, data.url))
            .build()
            .let(::publishArtwork)
    }
}
