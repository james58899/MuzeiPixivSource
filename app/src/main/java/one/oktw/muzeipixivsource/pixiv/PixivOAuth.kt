package one.oktw.muzeipixivsource.pixiv

import android.content.SharedPreferences
import androidx.core.content.edit
import okhttp3.*
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_ACCESS_TOKEN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_REFRESH_TOKEN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_USER_ID
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_USER_NAME
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_USER_USERNAME
import one.oktw.muzeipixivsource.pixiv.model.OAuth
import one.oktw.muzeipixivsource.pixiv.model.OAuthResponse
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON
import one.oktw.muzeipixivsource.util.HttpUtils.httpClient
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PixivOAuth {
    companion object {
        private const val API = "https://oauth.secure.pixiv.net/auth/token"
        private const val REDIRECT_URL = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback" // From https://app-api.pixiv.net/idp-urls
        private const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
        private const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"

        suspend fun login(verifierCode: String, authorizationCode: String): OAuth {
            return sendRequest(
                FormBody.Builder()
                    .add("client_id", CLIENT_ID)
                    .add("client_secret", CLIENT_SECRET)
                    .add("grant_type", "authorization_code")
                    .add("code_verifier", verifierCode)
                    .add("code", authorizationCode)
                    .add("redirect_uri", REDIRECT_URL)
                    .add("include_policy", "true") // enable new api
                    .build()
            )
        }

        suspend fun refresh(refreshToken: String): OAuth {
            return sendRequest(
                FormBody.Builder()
                    .add("client_id", CLIENT_ID)
                    .add("client_secret", CLIENT_SECRET)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .add("include_policy", "true") // enable new api
                    .build()
            )
        }

        fun save(preference: SharedPreferences, data: OAuthResponse) = preference.edit {
            putString(KEY_PIXIV_ACCESS_TOKEN, data.accessToken)
            putString(KEY_PIXIV_REFRESH_TOKEN, data.refreshToken)
            putInt(KEY_PIXIV_USER_ID, data.user.id)
            putString(KEY_PIXIV_USER_USERNAME, data.user.account)
            putString(KEY_PIXIV_USER_NAME, data.user.name)
        }

        private suspend fun sendRequest(data: RequestBody) = suspendCoroutine<OAuth> {
            val httpClient = httpClient.newBuilder().addNetworkInterceptor(OAuthInterceptor()).build()

            Request.Builder()
                .post(data)
                .url(API)
                .build()
                .let(httpClient::newCall)
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) = it.resumeWithException(e)

                    override fun onResponse(call: Call, response: Response) {
                        it.resume(GSON.fromJson(response.body!!.charStream(), OAuth::class.java))
                    }
                })
        }
    }

    class OAuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.US).format(Date())
            val hash = MessageDigest.getInstance("MD5")
                .digest("${timeFormat}28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c".toByteArray())
                .let { BigInteger(1, it) }
                .toString(16).padStart(32, '0')

            val newRequest = chain.request().newBuilder()
                .addHeader("Accept-Language", Locale.getDefault().toString())
                .addHeader("X-Client-Time", timeFormat)
                .addHeader("X-Client-Hash", hash).build()

            return chain.proceed(newRequest)
        }
    }
}
