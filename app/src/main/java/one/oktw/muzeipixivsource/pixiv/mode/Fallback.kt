package one.oktw.muzeipixivsource.pixiv.mode

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import java.util.*

class Fallback {
    companion object {
        fun getImages(): ArrayList<Illust> {
            return request("https://app-api.pixiv.net/v1/walkthrough/illusts").illusts
        }

        private fun request(url: String): IllustList {
            val httpClient = OkHttpClient()

            return Request.Builder()
                .url(url)
                .build()
                .let(httpClient::newCall)
                .execute()
                .body()!!
                .let {
                    GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
                        .fromJson<IllustList>(it.charStream(), IllustList::class.java)
                }
        }
    }
}