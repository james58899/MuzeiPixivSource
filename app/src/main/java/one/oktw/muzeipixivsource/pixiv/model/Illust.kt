package one.oktw.muzeipixivsource.pixiv.model

data class Illust(
    val id: Int,
    val title: String,
    val image_urls: ImageUrls,
    val caption: String,
    val user: User,
    val tags: ArrayList<Tag>,
    val tools: ArrayList<String>,
    val type: IllustTypes,
//    val createDate: Date,
    val pageCount: Int,
    val width: Int,
    val height: Int,
    val sanityLevel: Int,
    val metaSinglePage: IllustSinglePage,
    val metaPages: ArrayList<IllustPage>,
    val totalView: Int,
    val totalBookmarks: Int
//    val isBookmarked: Boolean,
//    val visible: Boolean,
//    val isMuted: Boolean
)
