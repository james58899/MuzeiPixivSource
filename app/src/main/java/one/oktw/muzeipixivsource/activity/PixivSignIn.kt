package one.oktw.muzeipixivsource.activity

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import org.mozilla.geckoview.*
import java.security.MessageDigest
import java.security.SecureRandom

class PixivSignIn : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Main + SupervisorJob()) {
    companion object {
        private val allowDomain = listOf("app-api.pixiv.net", "accounts.pixiv.net", "oauth.secure.pixiv.net")
    }

    private val messageDigest = MessageDigest.getInstance("SHA-256")
    private val securityRandom = SecureRandom()
    private lateinit var code: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the login form.
        setContentView(R.layout.activity_pixiv_login)
        val view: GeckoView = findViewById(R.id.geckoview)
        val session = GeckoSession()
        val runtime = GeckoRuntime.getDefault(this)
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                if (request.isRedirect && request.uri.startsWith("pixiv://")) {
                    // Async handle
                    launch(Dispatchers.IO) {
                        request.uri.toUri().getQueryParameter("code")?.let {
                            PixivOAuth.login(code, it).also { Log.i("LOGIN", it.toString()) }
                        }?.let {
                            if (!it.has_error && it.response != null)
                                withContext(Dispatchers.Main) {
                                    PixivOAuth.save(PreferenceManager.getDefaultSharedPreferences(this@PixivSignIn), it.response)
                                    session.close()
                                    setResult(RESULT_OK)
                                    finish()
                                }
                        } ?: withContext(Dispatchers.Main) {
                            // TODO error message
                            session.close()
                            finish()
                        }
                    }

                    return GeckoResult.DENY
                }

                // Disallow user use WebView browser other page
                if (request.uri.toUri().host !in allowDomain && !request.uri.startsWith("https://www.pixiv.net/logout.php")) {
                    launch {
                        startActivity(Intent(Intent.ACTION_VIEW, request.uri.toUri()))
                    }
                    return GeckoResult.DENY
                }
                return super.onLoadRequest(session, request)
            }
        }

        session.open(runtime)
        view.setSession(session)
        val (code, hash) = generateCodeAndHash()
        this.code = code
        Log.i("LOGIN webview", "code: $code, hash: $hash")
        session.loadUri("https://app-api.pixiv.net/web/v1/login?code_challenge=$hash&code_challenge_method=S256&client=pixiv-android") // Or any other URL...
    }

    private fun generateCodeAndHash(): Pair<String, String> {
        val code = ByteArray(32).apply(securityRandom::nextBytes).let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }

        return code to Base64.encodeToString(messageDigest.digest(code.encodeToByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
