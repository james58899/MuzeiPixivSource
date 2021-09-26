package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON
import one.oktw.muzeipixivsource.util.HttpUtils.httpClient

class Fallback {
    companion object {
        fun getImages(): List<Illust> {
            return request().illusts ?: emptyList()
        }

        private fun request(): IllustList {
            return Request.Builder()
                .url("https://app-api.pixiv.net/v1/walkthrough/illusts")
                .build()
                .let(httpClient::newCall)
                .execute()
                .body!!
                .let { GSON.fromJson(it.charStream(), IllustList::class.java) }
        }
    }
}
