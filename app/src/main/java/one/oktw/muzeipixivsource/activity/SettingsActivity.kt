package one.oktw.muzeipixivsource.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Only create new fragment on first create activity
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }
}
