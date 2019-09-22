package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.OkHttpClient
import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON
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
        val httpClient = OkHttpClient()

        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
            .let(httpClient::newCall)
            .execute()
            .body!!
            .let { GSON.fromJson(it.charStream(), IllustList::class.java) }
    }
}
