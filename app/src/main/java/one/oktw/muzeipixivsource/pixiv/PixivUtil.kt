package one.oktw.muzeipixivsource.pixiv

import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class PixivUtil {
    companion object {
        fun saveImage(url: String, dir: File): File {
            val httpClient = OkHttpClient()
            val file = File(dir, Uri.parse(url).lastPathSegment)

            Request.Builder() // build request
                .url(url)
                .header("Referer", "https://app-api.pixiv.net/")
                .build()
                .let(httpClient::newCall) // send request
                .execute()
                .run {
                    if (code() == 200) body()!!.byteStream().copyTo(file.outputStream()) // save file
                }

            return file
        }

        fun getPage(id: Int): Uri = Uri.parse("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=$id")
    }
}