package one.oktw.muzeipixivsource.pixiv

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.*
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_ACCESS_TOKEN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_DEVICE_TOKEN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_REFRESH_TOKEN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_USER_ID
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_USER_NAME
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_USER_USERNAME
import one.oktw.muzeipixivsource.pixiv.model.OAuth
import one.oktw.muzeipixivsource.pixiv.model.OAuthResponse
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PixivOAuth {
    companion object {
        private const val API = "https://oauth.secure.pixiv.net/auth/token"
        private const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
        private const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"

        suspend fun login(username: String, password: String): OAuth {
            return sendRequest(
                FormBody.Builder()
                    .add("client_id", CLIENT_ID)
                    .add("client_secret", CLIENT_SECRET)
                    .add("get_secure_url", "true")
                    .add("grant_type", "password")
                    .add("device_token", "pixiv")
                    .add("username", username)
                    .add("password", password)
                    .build()
            )
        }

        suspend fun refresh(deviceToken: String, refreshToken: String): OAuth {
            return sendRequest(
                FormBody.Builder()
                    .add("client_id", CLIENT_ID)
                    .add("client_secret", CLIENT_SECRET)
                    .add("get_secure_url", "true")
                    .add("grant_type", "refresh_token")
                    .add("device_token", deviceToken)
                    .add("refresh_token", refreshToken)
                    .build()
            )
        }

        fun save(preference: SharedPreferences, data: OAuthResponse) = preference.edit {
            putString(KEY_PIXIV_ACCESS_TOKEN, data.accessToken)
            putString(KEY_PIXIV_REFRESH_TOKEN, data.refreshToken)
            putString(KEY_PIXIV_DEVICE_TOKEN, data.deviceToken)
            putInt(KEY_PIXIV_USER_ID, data.user.id)
            putString(KEY_PIXIV_USER_USERNAME, data.user.account)
            putString(KEY_PIXIV_USER_NAME, data.user.name)
        }

        private suspend fun sendRequest(data: RequestBody) = suspendCoroutine<OAuth> {
            val httpClient = OkHttpClient()
            val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

            Request.Builder()
                .post(data)
                .url(API)
                .build()
                .let(httpClient::newCall)
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) = it.resumeWithException(e)

                    override fun onResponse(call: Call, response: Response) {
                        it.resume(gson.fromJson(response.body()!!.charStream(), OAuth::class.java))
                    }
                })

        }
    }
}