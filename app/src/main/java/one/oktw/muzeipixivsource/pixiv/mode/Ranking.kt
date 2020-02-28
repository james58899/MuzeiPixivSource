package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.*
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON
import one.oktw.muzeipixivsource.util.HttpUtils.apiHttpClient

class Ranking(private val token: String, private val category: RankingCategory) {
    fun getImages(number: Int): ArrayList<Illust> {
        val list = ArrayList<Illust>()
        var url = "https://app-api.pixiv.net/v1/illust/ranking?mode=" + when (category) {
            Daily -> "day"
            Weekly -> "week"
            Monthly -> "month"
        }

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
