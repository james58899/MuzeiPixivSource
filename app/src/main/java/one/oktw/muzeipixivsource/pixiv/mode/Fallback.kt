package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON
import one.oktw.muzeipixivsource.util.HttpUtils.apiHttpClient
import java.util.*

class Fallback {
    companion object {
        fun getImages(): ArrayList<Illust> {
            return request().illusts
        }

        private fun request(): IllustList {
            return Request.Builder()
                .url("https://app-api.pixiv.net/v1/walkthrough/illusts")
                .build()
                .let(apiHttpClient::newCall)
                .execute()
                .body!!
                .let { GSON.fromJson(it.charStream(), IllustList::class.java) }
        }
    }
}
