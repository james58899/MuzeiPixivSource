package one.oktw.muzeipixivsource.pixiv

import android.net.Uri
import java.io.File

data class DataImageInfo(
    val title: String,
    val author: String,
    val url: Uri,
    var file: File? = null
)