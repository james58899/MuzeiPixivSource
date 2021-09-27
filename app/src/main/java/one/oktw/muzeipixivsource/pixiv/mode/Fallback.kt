package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.OkHttpClient
import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON

class Fallback {
    companion object {
        fun getImages(httpClient: OkHttpClient): List<Illust> {
            return Request.Builder().url("https://app-api.pixiv.net/v1/walkthrough/illusts").build()
                .let(httpClient::newCall).execute().body!!
                .use { GSON.fromJson(it.charStream(), IllustList::class.java) }.illusts ?: emptyList()
        }
    }
}
