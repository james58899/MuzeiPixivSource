package one.oktw.muzeipixivsource.activity

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment
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
                val url = request.url
                if (url.scheme == "pixiv") {
                    // Async handle
                    launch(Dispatchers.IO) {
                        val preference = PreferenceManager.getDefaultSharedPreferences(this@PixivSignIn)
                        url.getQueryParameter("code")?.let {
                            PixivOAuth.login(code, it, preference.getBoolean(SettingsFragment.KEY_FETCH_DIRECT, false))
                        }?.let {
                            if (!it.has_error && it.response != null)
                                withContext(Dispatchers.Main) {
                                    PixivOAuth.save(preference, it.response)
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
                if (url.host !in allowDomain && !url.toString().startsWith("https://www.pixiv.net/logout.php")) {
                    if (url.host == "socialize.gigya.com") bypassDomainCheck = true else if (!bypassDomainCheck) {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                        return true
                    }
                } else if (bypassDomainCheck) bypassDomainCheck = false // Enable check if back to pixiv.
                return false
            }

            // Android API < 24
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // to new API
                return shouldOverrideUrlLoading(view, object : WebResourceRequest {
                    override fun getUrl() = url.toUri()

                    override fun isForMainFrame() = true

                    override fun isRedirect() = true

                    override fun hasGesture() = true

                    override fun getMethod() = "GET"

                    override fun getRequestHeaders() = emptyMap<String, String>()
                })
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
