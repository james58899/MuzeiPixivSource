package one.oktw.muzeipixivsource.pixiv

import one.oktw.muzeipixivsource.pixiv.mode.*
import one.oktw.muzeipixivsource.pixiv.model.Illust
import java.io.File
import java.util.*

class Pixiv(
    private val token: String?,
    private val originImage: Boolean = false,
    private val safety: Boolean = true,
    private val size: Boolean = true,
    private val number: Int = 30,
    private val minView: Int = 0,
    private val minBookmark: Int = 0,
    savePath: File
) {
    private val savePath = File(savePath, "pixiv").apply { mkdir() }

    fun getFallback() = Fallback.getImages().let(::random).let(::processIllust)

    fun getRanking(category: RankingCategory): DataImageInfo {
        if (token == null) return getFallback()

        return Ranking(token, category).getImages(number).let(::processList)
    }

    fun getRecommend(): DataImageInfo {
        if (token == null) return getFallback()

        return Recommend(token).getImages(number).let(::processList)
    }

    fun getBookmark(user: Int, private: Boolean = false): DataImageInfo {
        if (token == null) return getFallback()

        return Bookmark(token, user, private).getImages(number).let(::processList)
    }

    private fun processList(list: List<Illust>): DataImageInfo {
        return list.let(::filterSafety)
            .let(::filterSize)
            .let(::filterView)
            .let(::filterBookmark)
            .let(::random)
            .let(::processIllust)
    }

    private fun filterSafety(list: List<Illust>) = if (safety) list.filter { it.sanityLevel <= 4 } else list

    private fun filterSize(list: List<Illust>) = if (size) list.filter { it.height > 1000 } else list

    private fun filterView(list: List<Illust>) = if (minView > 0) list.filter { it.totalView >= minView } else list

    private fun filterBookmark(list: List<Illust>) =
        if (minBookmark > 0) list.filter { it.totalBookmarks >= minBookmark } else list

    private fun random(list: List<Illust>) = list[Random().nextInt(list.size)]

    private fun processIllust(illust: Illust): DataImageInfo {
        // check page count then get image url
        if (illust.pageCount > 1) {
            illust.metaPages[0].run { if (originImage) imageUrls.original!! else imageUrls.large!! }
        } else {
            illust.run { if (originImage) metaSinglePage.original_image_url else image_urls.large!! }
        }.run { replace("/c/600x1200_90", "") }.let {
            // save image then return
            return DataImageInfo(
                illust.title,
                illust.user.name,
                PixivUtil.getPage(illust.id),
                PixivUtil.saveImage(it, savePath)
            )
        }
    }
}
