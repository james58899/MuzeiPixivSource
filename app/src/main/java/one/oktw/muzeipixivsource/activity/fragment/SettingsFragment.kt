package one.oktw.muzeipixivsource.activity.fragment

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.edit
import androidx.preference.*
import com.google.android.apps.muzei.api.provider.ProviderContract
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.PixivSignIn
import one.oktw.muzeipixivsource.activity.preference.NumberPickerPreference
import one.oktw.muzeipixivsource.provider.MuzeiProvider
import one.oktw.muzeipixivsource.util.AppUtil.Companion.MUZEI_PACKAGE
import one.oktw.muzeipixivsource.util.AppUtil.Companion.launchOrMarket

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var fetchCategory: PreferenceCategory
    private lateinit var fetchMode: ListPreference
    private lateinit var rankingPreference: ListPreference
    private lateinit var bookmarkPreference: SwitchPreferenceCompat

    companion object {
        private const val PIXIV_LOGIN = 0

        // Fetch mode value
        const val FETCH_MODE_FALLBACK = -1
        const val FETCH_MODE_RANKING = 0
        const val FETCH_MODE_RECOMMEND = 1
        const val FETCH_MODE_BOOKMARK = 2

        // Preference keys
        const val KEY_ACCOUNT = "account"
        const val KEY_MUZEI = "muzei"
        const val KEY_FETCH = "fetch"
        const val KEY_FETCH_ORIGIN = "fetch_origin"
        const val KEY_FETCH_NUMBER = "fetch_number"
        const val KEY_FETCH_CLEANUP = "fetch_cleanup"
        const val KEY_FETCH_MODE = "fetch_mode"
        const val KEY_FETCH_FALLBACK = "fetch_fallback"
        const val KEY_FETCH_MODE_RANKING = "fetch_mode_ranking"
        const val KEY_FETCH_MODE_BOOKMARK = "fetch_mode_bookmark"
        const val KEY_FETCH_RANDOM = "fetch_random"
        const val KEY_FETCH_MIRROR = "fetch_mirror"
        const val KEY_FILTER_SAFE = "filter_safe"
        const val KEY_FILTER_ILLUST = "filter_illust"
        const val KEY_FILTER_SIZE = "filter_size"
        const val KEY_FILTER_VIEW = "filter_view"
        const val KEY_FILTER_BOOKMARK = "filter_bookmark"
        const val KEY_PIXIV_ACCESS_TOKEN = "pixiv_access_token"
        const val KEY_PIXIV_REFRESH_TOKEN = "pixiv_refresh_token"
        const val KEY_PIXIV_DEVICE_TOKEN = "pixiv_device_token"
        const val KEY_PIXIV_USER_ID = "pixiv_user_id"
        const val KEY_PIXIV_USER_NAME = "pixiv_user_name"
        const val KEY_PIXIV_USER_USERNAME = "pixiv_user_username"
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is NumberPickerPreference) {
            NumberPickerPreference.Fragment.newInstance(preference.key).also {
                it.setTargetFragment(this, 0)
                it.show(parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
            }
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefragment, rootKey)

        findPreference<Preference>(KEY_ACCOUNT)?.let { initAccountButton(it) }
        findPreference<Preference>(KEY_MUZEI)?.let { initMuzeiButton(it) }
        findPreference<Preference>(KEY_FETCH_MODE)?.let { initFetchMode(it) }
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)

        when (request) {
            PIXIV_LOGIN -> if (result == RESULT_OK) {
                updateAccountInfo()
                updateFetchModePreference()
            }
        }
    }

    private fun initAccountButton(preference: Preference) {
        updateAccountInfo()

        preference.setOnPreferenceClickListener {
            if (it.sharedPreferences.contains(KEY_PIXIV_ACCESS_TOKEN)) {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.pref_pixiv_sign_out_confirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        logout()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                startActivityForResult(Intent(activity, PixivSignIn::class.java), PIXIV_LOGIN)
            }

            true
        }
    }

    private fun initMuzeiButton(preference: Preference) {
        preference.setOnPreferenceClickListener {
            launchOrMarket(requireContext(), MUZEI_PACKAGE)

            true
        }
    }

    private fun initFetchMode(preference: Preference) {
        updateFetchModePreference()

        preference.setOnPreferenceChangeListener { pref, new ->
            if ((pref as ListPreference).value != new) updateFetchModePreference(new as String)

            true
        }
    }

    private fun updateAccountInfo() {
        val account = findPreference<Preference>(KEY_ACCOUNT) ?: return
        val pref = account.sharedPreferences

        if (pref.contains(KEY_PIXIV_ACCESS_TOKEN)) {
            account.title = getString(R.string.pref_pixiv_sign_out)
            findPreference<ListPreference>(KEY_FETCH_MODE)?.isEnabled = true
            findPreference<SwitchPreferenceCompat>(KEY_FETCH_FALLBACK)?.isEnabled = true
        } else {
            account.title = getString(R.string.pref_pixiv_sign_in)
            findPreference<ListPreference>(KEY_FETCH_MODE)?.isEnabled = false
            findPreference<SwitchPreferenceCompat>(KEY_FETCH_FALLBACK)?.isEnabled = false
        }

        account.summary = pref.getString(KEY_PIXIV_USER_NAME, getString(R.string.pref_pixiv_summary))
    }

    private fun updateFetchModePreference(newValue: String? = null) {
        // init some preference
        if (!this::fetchMode.isInitialized)
            fetchMode = findPreference(KEY_FETCH_MODE)!!
        if (!this::fetchCategory.isInitialized)
            fetchCategory = findPreference(KEY_FETCH)!!
        if (!this::rankingPreference.isInitialized)
            rankingPreference = findPreference(KEY_FETCH_MODE_RANKING)!!
        if (!this::bookmarkPreference.isInitialized)
            bookmarkPreference = findPreference(KEY_FETCH_MODE_BOOKMARK)!!

        listOf(rankingPreference, bookmarkPreference).forEach { fetchCategory.removePreference(it) }

        // hide if not login
        if (fetchMode.isEnabled) when (newValue?.toInt() ?: fetchMode.value.toInt()) {
            FETCH_MODE_RANKING -> fetchCategory.addPreference(rankingPreference)
            FETCH_MODE_BOOKMARK -> fetchCategory.addPreference(bookmarkPreference)
        }

        if (newValue != null) {
            val context = requireContext()

            ProviderContract.getContentUri(context.packageManager.getProviderInfo(ComponentName(context, MuzeiProvider::class.java), 0).authority)
                .let { context.contentResolver.delete(it, null, null) }
        }
    }

    private fun logout() {
        preferenceManager.sharedPreferences.edit {
            remove(KEY_PIXIV_ACCESS_TOKEN)
            remove(KEY_PIXIV_REFRESH_TOKEN)
            remove(KEY_PIXIV_DEVICE_TOKEN)
            remove(KEY_PIXIV_USER_ID)
            remove(KEY_PIXIV_USER_USERNAME)
            remove(KEY_PIXIV_USER_NAME)
        }

        updateAccountInfo()
        updateFetchModePreference()
    }
}
