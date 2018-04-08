package one.oktw.muzeipixivsource.pixiv

import one.oktw.muzeipixivsource.pixiv.mode.Bookmark
import one.oktw.muzeipixivsource.pixiv.mode.Fallback
import one.oktw.muzeipixivsource.pixiv.mode.Ranking
import one.oktw.muzeipixivsource.pixiv.mode.Recommend
import one.oktw.muzeipixivsource.pixiv.model.Illust
import java.io.File
import java.util.*

class Pixiv(private val token: String?, private val originImage: Boolean = false, savePath: File) {
    private val savePath = File(savePath, "pixiv").apply { mkdir() } // TODO auto clean cache

    fun getFallback() = Fallback.getImages().let(::random).let(::processIllust)

    fun getRanking(): DataImageInfo {
        if (token == null) return getFallback()

        return Ranking(token).getImages(60).let(::random).let(::processIllust)
    }

    fun getRecommend(): DataImageInfo {
        if (token == null) return getFallback()

        return Recommend(token).getImages(30).let(::random).let(::processIllust)
    }

    fun getBookmark(user: Int, private: Boolean = false): DataImageInfo {
        if (token == null) return getFallback()

        return Bookmark(token, user, private).getImages(90).let(::random).let(::processIllust)
    }

    private fun random(list: ArrayList<Illust>) = list[Random().nextInt(list.size)]

    private fun processIllust(illust: Illust): DataImageInfo {
        // check page count then get image url
        if (illust.pageCount > 1) {
            illust.metaPages[0].run { if (originImage) imageUrls.original!! else imageUrls.large!! }
        } else {
            illust.run { if (originImage) metaSinglePage.original_image_url else image_urls.large!! }
        }.let {
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
