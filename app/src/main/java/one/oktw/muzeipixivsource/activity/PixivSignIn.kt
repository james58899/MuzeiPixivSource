package one.oktw.muzeipixivsource.activity

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import java.security.MessageDigest
import java.security.SecureRandom

class PixivSignIn : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Main + SupervisorJob()) {
    companion object {
        private val allowDomain = listOf("app-api.pixiv.net", "accounts.pixiv.net", "oauth.secure.pixiv.net")
    }

    private lateinit var code: String
    private var bypassDomainCheck = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the login form.
        setContentView(R.layout.activity_pixiv_login)
        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.userAgentString = webView.settings.userAgentString.replace(Regex("Version/\\d\\.\\d\\s"), "") // Hide WebView version
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.isRedirect && request.url.scheme == "pixiv") {
                    // Async handle
                    launch(Dispatchers.IO) {
                        request.url.getQueryParameter("code")?.let {
                            PixivOAuth.login(code, it)
                        }?.let {
                            if (!it.has_error && it.response != null)
                                withContext(Dispatchers.Main) {
                                    PixivOAuth.save(PreferenceManager.getDefaultSharedPreferences(this@PixivSignIn), it.response)
                                    webView.destroy()
                                    setResult(RESULT_OK)
                                    finish()
                                }
                        } ?: withContext(Dispatchers.Main) {
                            // TODO error message
                            finish()
                        }
                    }

                    return true
                }

                // Disallow user use WebView browser other page
                if (!bypassDomainCheck && request.url.host !in allowDomain && !request.url.toString().startsWith("https://www.pixiv.net/logout.php")) {
                    if (request.url.host == "socialize.gigya.com") bypassDomainCheck = true else {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        return true
                    }
                }
                return false
            }
        }

        val (code, hash) = generateCodeAndHash()
        this.code = code
        webView.loadUrl("https://app-api.pixiv.net/web/v1/login?code_challenge=$hash&code_challenge_method=S256&client=pixiv-android")
    }

    private fun generateCodeAndHash(): Pair<String, String> {
        val code = ByteArray(32).apply(SecureRandom()::nextBytes).let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }

        return code to Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(code.encodeToByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
