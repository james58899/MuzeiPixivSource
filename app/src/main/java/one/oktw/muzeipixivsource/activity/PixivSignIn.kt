package one.oktw.muzeipixivsource.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.activity_pixiv_login.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.pixiv.PixivOAuth

class PixivSignIn : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the login form.
        setContentView(R.layout.activity_pixiv_login)

        password.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                login()
                return@setOnEditorActionListener true
            }
            false
        }

        login_button.setOnClickListener { login() }
    }

    private fun login() {
        // Reset errors.
        username.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val textUsername = username.text.toString()
        val textPassword = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(textPassword)) {
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        } else if (!isPasswordValid(textPassword)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid username.
        if (TextUtils.isEmpty(textUsername)) {
            username.error = getString(R.string.error_field_required)
            focusView = username
            cancel = true
        } else if (!isUsernameValid(textUsername)) {
            username.error = getString(R.string.error_invalid_username)
            focusView = username
            cancel = true
        }

        if (cancel) focusView?.requestFocus() else launch(UI) {
            // close IME
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
                hideSoftInputFromWindow(username.windowToken, 0)
                hideSoftInputFromWindow(password.windowToken, 0)
            }

            // disable button
            login_button.isEnabled = false

            val login = loginPixiv(textUsername, textPassword)

            if (!login.has_error && login.response != null) {
                setResult(Activity.RESULT_OK, Intent().putExtra("response", login.response))
                finish()
            } else {
                login_button.isEnabled = true

                // TODO show error message
                Snackbar.make(login_layout, R.string.login_fail, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun isPasswordValid(password: String) = password.length >= 6

    private fun isUsernameValid(username: String): Boolean {
        // match email
        if (username.matches(Patterns.EMAIL_ADDRESS.toRegex())) return true

        // match username
        if (username.matches(Regex("[a-z0-9_-]{3,32}"))) return true

        return false
    }

    private suspend fun loginPixiv(username: String, password: String) = withContext(CommonPool) {
        PixivOAuth.login(username, password)
    }
}
