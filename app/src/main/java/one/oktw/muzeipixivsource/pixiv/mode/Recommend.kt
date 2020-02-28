package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON
import one.oktw.muzeipixivsource.util.HttpUtils.apiHttpClient
import java.util.*

class Recommend(private val token: String) {
    fun getImages(number: Int): ArrayList<Illust> {
        val list = ArrayList<Illust>()
        var url = "https://app-api.pixiv.net/v1/illust/recommended"

        do {
            val res = request(url)

            if (res.nextUrl != null) url = res.nextUrl else break

            list += res.illusts
        } while (list.size < number)

        return list
    }

    private fun request(url: String): IllustList {
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
            .let(apiHttpClient::newCall)
            .execute()
            .body!!
            .let { GSON.fromJson(it.charStream(), IllustList::class.java) }
    }
}
