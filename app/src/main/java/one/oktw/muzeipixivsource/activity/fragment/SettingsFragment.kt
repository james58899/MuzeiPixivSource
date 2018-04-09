package one.oktw.muzeipixivsource.activity.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v7.app.AlertDialog
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.Preference.OnPreferenceClickListener
import android.support.v7.preference.PreferenceCategory
import android.support.v7.preference.PreferenceFragmentCompat
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.PixivSignIn
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import one.oktw.muzeipixivsource.pixiv.model.OAuthResponse
import java.util.Arrays.asList

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var fetchCategory: PreferenceCategory
    private lateinit var fetchMode: ListPreference
    private lateinit var rankingPreference: ListPreference
    private lateinit var bookmarkPreference: SwitchPreference

    companion object {
        private const val PIXIV_LOGIN = 0

        // Fetch mode value
        const val FETCH_MODE_FALLBACK = -1
        const val FETCH_MODE_RANKING = 0
        const val FETCH_MODE_RECOMMEND = 1
        const val FETCH_MODE_BOOKMARK = 2

        // Preference keys
        const val KEY_ACCOUNT = "account"
        const val KEY_FETCH = "fetch"
        const val KEY_FETCH_ORIGIN = "fetch_origin"
        const val KEY_FETCH_SAFE = "fetch_safe"
        const val KEY_FETCH_MODE = "fetch_mode"
        const val KEY_FETCH_MODE_RANKING = "fetch_mode_ranking"
        const val KEY_FETCH_MODE_BOOKMARK = "fetch_mode_bookmark"
        const val KEY_FETCH_FILTER_SIZE = "fetch_size"
        const val KEY_MUZEI_CHANGE_INTERVAL = "muzei_interval"
        const val KEY_PIXIV_ACCESS_TOKEN = "pixiv_access_token"
        const val KEY_PIXIV_REFRESH_TOKEN = "pixiv_refresh_token"
        const val KEY_PIXIV_DEVICE_TOKEN = "pixiv_device_token"
        const val KEY_PIXIV_USER_ID = "pixiv_user_id"
        const val KEY_PIXIV_USER_NAME = "pixiv_user_name"
        const val KEY_PIXIV_USER_USERNAME = "pixiv_user_username"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefragment, rootKey)

        initAccountButton(findPreference(KEY_ACCOUNT))
        initFetchMode(findPreference(KEY_FETCH_MODE))
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)

        when (request) {
            PIXIV_LOGIN -> onLogin(result, data)
        }
    }

    private fun initAccountButton(preference: Preference) {
        updateAccountInfo()

        preference.onPreferenceClickListener = OnPreferenceClickListener {
            if (it.sharedPreferences.contains(KEY_PIXIV_ACCESS_TOKEN)) {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.pref_pixiv_sign_out_confirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        logout()
                        updateAccountInfo()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
            } else {
                startActivityForResult(Intent(context, PixivSignIn::class.java), PIXIV_LOGIN)
            }

            true
        }
    }

    private fun initFetchMode(preference: Preference) {
        updateFetchModePreference()

        preference.setOnPreferenceChangeListener { _, new ->
            updateFetchModePreference(new as String)

            true
        }
    }

    private fun updateAccountInfo() {
        val account = findPreference(KEY_ACCOUNT)
        val pref = account.sharedPreferences

        if (pref.contains(KEY_PIXIV_ACCESS_TOKEN)) {
            account.title = getString(R.string.pref_pixiv_sign_out)
            findPreference(KEY_FETCH_MODE).isEnabled = true
        } else {
            account.title = getString(R.string.pref_pixiv_sign_in)
            findPreference(KEY_FETCH_MODE).isEnabled = false
        }

        account.summary = pref.getString(KEY_PIXIV_USER_NAME, getString(R.string.pref_pixiv_summary))
    }

    private fun updateFetchModePreference(newValue: String? = null) {
        // init some preference
        if (!this::fetchCategory.isInitialized)
            fetchCategory = findPreference(KEY_FETCH) as PreferenceCategory
        if (!this::fetchMode.isInitialized)
            fetchMode = findPreference(KEY_FETCH_MODE) as ListPreference
        if (!this::rankingPreference.isInitialized)
            rankingPreference = findPreference(KEY_FETCH_MODE_RANKING) as ListPreference
        if (!this::bookmarkPreference.isInitialized)
            bookmarkPreference = findPreference(KEY_FETCH_MODE_BOOKMARK) as SwitchPreference

        val mode = (newValue ?: fetchMode.value).toInt()

        asList(rankingPreference, bookmarkPreference).forEach { fetchCategory.removePreference(it) }

        // hide if not login
        if (fetchMode.isEnabled) when (mode) {
            FETCH_MODE_RANKING -> fetchCategory.addPreference(rankingPreference)
            FETCH_MODE_BOOKMARK -> fetchCategory.addPreference(bookmarkPreference)
        }
    }

    private fun onLogin(result: Int, data: Intent?) {
        if (result != RESULT_OK || data == null) return

        data.getParcelableExtra<OAuthResponse>("response").let {
            PixivOAuth.save(preferenceManager.sharedPreferences, it)
        }

        updateAccountInfo()
    }

    private fun logout() {
        preferenceManager.sharedPreferences.edit()
            .remove(KEY_PIXIV_ACCESS_TOKEN)
            .remove(KEY_PIXIV_REFRESH_TOKEN)
            .remove(KEY_PIXIV_DEVICE_TOKEN)
            .remove(KEY_PIXIV_USER_ID)
            .remove(KEY_PIXIV_USER_USERNAME)
            .remove(KEY_PIXIV_USER_NAME)
            .apply()
    }
}