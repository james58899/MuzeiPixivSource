package one.oktw.muzeipixivsource.activity

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import one.oktw.muzeipixivsource.util.getParcelableExtraCompat

class ShareActivity : AppCompatActivity() {
    companion object {
        // Share command extra data
        const val INTENT_SHARE_FILENAME = "filename"
        const val INTENT_SHARE_FILE_URI = "file_uri"
        const val INTENT_SHARE_TITLE = "title"
        const val INTENT_SHARE_TEXT = "text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val cacheFile = intent.getStringExtra(INTENT_SHARE_FILENAME)?.let {
            cacheDir?.resolve("share")?.apply { mkdir() }?.apply { deleteOnExit() }?.resolve(it)
        } ?: return

        if (!cacheFile.exists()) {
            val source = intent.getParcelableExtraCompat<Uri>(INTENT_SHARE_FILE_URI)?.let(contentResolver::openInputStream) ?: return
            cacheFile.outputStream().use { file -> source.use { it.copyTo(file) } }
        }

        Intent(Intent.ACTION_SEND).apply {
            val title = intent.getStringExtra(INTENT_SHARE_TITLE)
            val shareUri = FileProvider.getUriForFile(this@ShareActivity, "one.oktw.muzeipixivsource.share", cacheFile)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(INTENT_SHARE_TEXT))
            putExtra(Intent.EXTRA_STREAM, shareUri)
            clipData = ClipData(title, arrayOf("image/*"), ClipData.Item(shareUri))
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "image/*"
        }.let {
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { finish() }.launch(Intent.createChooser(it, null))
        }
    }
}
