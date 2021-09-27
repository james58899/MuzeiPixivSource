package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.OkHttpClient
import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.*
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON

class Ranking {
    companion object {
        private var category: RankingCategory? = null
        private var url: String? = null

        fun getImages(httpClient: OkHttpClient, token: String, category: RankingCategory, number: Int): ArrayList<Illust> {
            if (category != this.category || url == null) {
                this.category = category
                url = getUrl(category)
            }

            val list = ArrayList<Illust>()
            do {
                val res = Request.Builder().url(url!!).header("Authorization", "Bearer $token").build()
                    .let(httpClient::newCall).execute().body!!
                    .use { GSON.fromJson(it.charStream(), IllustList::class.java) }

                if (res.nextUrl != null) url = res.nextUrl else break

                if (res.illusts.isNullOrEmpty()) break
                list += res.illusts
            } while (list.size < number)

            return list
        }

        private fun getUrl(category: RankingCategory) = "https://app-api.pixiv.net/v1/illust/ranking?mode=" + when (category) {
            Daily -> "day"
            Weekly -> "week"
            Monthly -> "month"
        }
    }
}
