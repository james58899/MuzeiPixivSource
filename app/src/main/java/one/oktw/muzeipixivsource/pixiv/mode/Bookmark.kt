package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON
import one.oktw.muzeipixivsource.util.HttpUtils.httpClient
import java.util.*

class Bookmark(private val token: String, private val user: Int, private val private: Boolean) {
    fun getImages(number: Int): ArrayList<Illust> {
        val list = ArrayList<Illust>()
        var publicUrl = "https://app-api.pixiv.net/v1/user/bookmarks/illust?restrict=public&user_id=$user"
        var privateUrl = "https://app-api.pixiv.net/v1/user/bookmarks/illust?restrict=private&user_id=$user"
        var i = 0

        do {
            // random select private or public bookmark
            val random = if (private) Random().nextBoolean() else false
            val res = request(if (random) privateUrl else publicUrl)

            if (res.nextUrl != null) {
                res.nextUrl.let { if (random) privateUrl = it else publicUrl = it }
            } else {
                // retry 3 time if private enable
                if (!private || i >= 3) break

                i++
            }

            if (res.illusts.isNullOrEmpty()) break
            list += res.illusts
        } while (list.size < number)

        return list
    }

    private fun request(url: String): IllustList {
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
            .let(httpClient::newCall)
            .execute()
            .body!!
            .let { GSON.fromJson(it.charStream(), IllustList::class.java) }
    }
}
