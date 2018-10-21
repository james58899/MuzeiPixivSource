package one.oktw.muzeipixivsource.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_pixiv_login.*
import kotlinx.coroutines.experimental.*
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import one.oktw.muzeipixivsource.pixiv.model.OAuth
import java.io.IOException

class PixivSignIn : AppCompatActivity(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()

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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
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

        // Check for a valid password.
        if (TextUtils.isEmpty(textPassword)) {
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        } else if (textPassword.length < 6) {
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

        if (cancel) focusView?.requestFocus() else launch {
            // close IME
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
                hideSoftInputFromWindow(username.windowToken, 0)
                hideSoftInputFromWindow(password.windowToken, 0)
            }

            // disable button
            login_button.isEnabled = false

            val login = try {
                withContext(Dispatchers.IO) { PixivOAuth.login(textUsername, textPassword) }
            } catch (e: IOException) {
                OAuth(has_error = true, errors = e.message?.let { OAuth.Errors(OAuth.Errors.System(it)) })
            }

            if (!login.has_error && login.response != null) {
                setResult(RESULT_OK, Intent().putExtra("response", login.response))
                finish()
            } else {
                login_button.isEnabled = true

                // TODO show error message
                Snackbar.make(login_layout, R.string.login_fail, Snackbar.LENGTH_LONG)
            }
        }
    }

    private fun isUsernameValid(username: String): Boolean {
        // match email
        if (username.matches(Patterns.EMAIL_ADDRESS.toRegex())) return true

        // match username
        if (username.matches(Regex("[a-z0-9_-]{3,32}"))) return true

        return false
    }
}
