package one.oktw.muzeipixivsource.activity

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment
import one.oktw.muzeipixivsource.util.AppUtil.Companion.MUZEI_PACKAGE
import one.oktw.muzeipixivsource.util.AppUtil.Companion.checkInstalled
import one.oktw.muzeipixivsource.util.AppUtil.Companion.showInstallDialog

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // if click update version notification
        intent.getStringExtra("new_version")?.let {
            startActivity(Intent(ACTION_VIEW, Uri.parse(it)))
            finish()
        }

        // check muzei installed
        if (!checkInstalled(this, MUZEI_PACKAGE)) {
            showInstallDialog(this, MUZEI_PACKAGE)
        }

        // Only create new fragment on first create activity
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }
}
