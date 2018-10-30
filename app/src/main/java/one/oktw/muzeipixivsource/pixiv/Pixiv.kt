package one.oktw.muzeipixivsource.pixiv

import one.oktw.muzeipixivsource.pixiv.mode.*
import one.oktw.muzeipixivsource.pixiv.model.Illust
import java.util.*

class Pixiv(private val token: String?, private val number: Int = 30, private val fallback: Boolean = true) {
    fun getFallback() = Fallback.getImages()

    fun getRanking(category: RankingCategory): ArrayList<Illust> {
        return Ranking(token!!, category).getImages(number)
    }

    fun getRecommend(): ArrayList<Illust> {
        return Recommend(token!!).getImages(number)
    }

    fun getBookmark(user: Int, private: Boolean = false): ArrayList<Illust> {
        return Bookmark(token!!, user, private).getImages(number)
    }
}
