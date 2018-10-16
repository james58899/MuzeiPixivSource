package one.oktw.muzeipixivsource.pixiv

import one.oktw.muzeipixivsource.pixiv.mode.*
import one.oktw.muzeipixivsource.pixiv.model.Illust
import java.util.*

class Pixiv(private val token: String?, private val number: Int = 30) {
    fun getFallback() = Fallback.getImages()

    fun getRanking(category: RankingCategory): ArrayList<Illust> {
        if (token == null) return getFallback()

        return Ranking(token, category).getImages(number)
    }

    fun getRecommend(): ArrayList<Illust> {
        if (token == null) return getFallback()

        return Recommend(token).getImages(number)
    }

    fun getBookmark(user: Int, private: Boolean = false): ArrayList<Illust> {
        if (token == null) return getFallback()

        return Bookmark(token, user, private).getImages(number)
    }
}
