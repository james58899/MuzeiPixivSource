package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.OkHttpClient
import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON

class Recommend {
    companion object {
        fun getImages(httpClient: OkHttpClient, token: String, number: Int): ArrayList<Illust> {
            val list = ArrayList<Illust>()
            var url = "https://app-api.pixiv.net/v1/illust/recommended"

            do {
                val res = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
                    .let(httpClient::newCall).execute().body!!
                    .use { GSON.fromJson(it.charStream(), IllustList::class.java) }

                if (res.nextUrl != null) url = res.nextUrl else break

                if (res.illusts.isNullOrEmpty()) break
                list += res.illusts
            } while (list.size < number)

            return list
        }
    }
}
