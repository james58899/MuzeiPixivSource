package one.oktw.muzeipixivsource.pixiv.mode

import okhttp3.OkHttpClient
import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import one.oktw.muzeipixivsource.util.AppUtil.Companion.GSON
import java.util.Random

class Bookmark {
    companion object {
        private var user: Int? = null
        private var publicUrl = "https://app-api.pixiv.net/v1/user/bookmarks/illust?restrict=public&user_id=$user"
        private var privateUrl = "https://app-api.pixiv.net/v1/user/bookmarks/illust?restrict=private&user_id=$user"

        fun getImages(httpClient: OkHttpClient, token: String, user: Int, private: Boolean, number: Int): ArrayList<Illust> {
            if (user != this.user) {
                this.user = user
                resetUrl()
            }

            val list = ArrayList<Illust>()
            var i = 0

            do {
                // random select private or public bookmark
                val random = if (private) Random().nextBoolean() else false
                val res = Request.Builder().url(if (random) privateUrl else publicUrl).header("Authorization", "Bearer $token").build()
                    .let(httpClient::newCall).execute().body!!
                    .use { GSON.fromJson(it.charStream(), IllustList::class.java) }

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

        private fun resetUrl() {
            publicUrl = "https://app-api.pixiv.net/v1/user/bookmarks/illust?restrict=public&user_id=$user"
            privateUrl = "https://app-api.pixiv.net/v1/user/bookmarks/illust?restrict=private&user_id=$user"
        }
    }
}
