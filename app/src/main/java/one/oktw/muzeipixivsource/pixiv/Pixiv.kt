package one.oktw.muzeipixivsource.pixiv

import one.oktw.muzeipixivsource.pixiv.mode.Bookmark
import one.oktw.muzeipixivsource.pixiv.mode.Fallback
import one.oktw.muzeipixivsource.pixiv.mode.Ranking
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory
import one.oktw.muzeipixivsource.pixiv.mode.Recommend
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.util.HttpUtils

class Pixiv(private var token: String?, private val number: Int = 30, direct: Boolean = false) {
    private val httpClient = if (direct) HttpUtils.directHttpClient else HttpUtils.httpClient

    fun getFallback() = Fallback.getImages(httpClient)

    fun getRanking(category: RankingCategory): ArrayList<Illust> {
        return Ranking.getImages(httpClient, token!!, category, number)
    }

    fun getRecommend(): ArrayList<Illust> {
        return Recommend.getImages(httpClient, token!!, number)
    }

    fun getBookmark(user: Int, private: Boolean = false): ArrayList<Illust> {
        return Bookmark.getImages(httpClient, token!!, user, private, number)
    }
}
