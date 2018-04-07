package one.oktw.muzeipixivsource.pixiv

import one.oktw.muzeipixivsource.pixiv.mode.Bookmark
import one.oktw.muzeipixivsource.pixiv.mode.Fallback
import one.oktw.muzeipixivsource.pixiv.mode.Ranking
import one.oktw.muzeipixivsource.pixiv.mode.Recommend
import one.oktw.muzeipixivsource.pixiv.model.Illust
import java.io.File

class Pixiv(private val token: String?, savePath: File) {
    private val savePath = File(savePath, "pixiv").apply { mkdir() } // TODO auto clean cache

    fun getFallback() = Fallback.getImage().let(::processIllust)

    fun getRanking(): DataImageInfo {
        if (token == null) return getFallback()

        return Ranking(token).getImage().let(::processIllust)
    }

    fun getRecommend(): DataImageInfo {
        if (token == null) return getFallback()

        return Recommend(token).getImage().let(::processIllust)
    }

    fun getBookmark(user: Int, private: Boolean = false): DataImageInfo {
        if (token == null) return getFallback()

        return Bookmark(token, user, private).getImage().let(::processIllust)
    }

    private fun processIllust(illust: Illust): DataImageInfo {
        // check page count then get original image url
        if (illust.pageCount > 1) {
            illust.metaPages[0].imageUrls.original!!
        } else {
            illust.metaSinglePage.original_image_url
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
