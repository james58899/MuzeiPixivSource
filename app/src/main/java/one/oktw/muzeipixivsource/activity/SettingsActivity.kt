package one.oktw.muzeipixivsource.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // if click update version notification
        intent.getStringExtra("new_version")?.let {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
            finish()
        }

        // Only create new fragment on first create activity
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }
}
