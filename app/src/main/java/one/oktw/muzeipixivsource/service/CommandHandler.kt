package one.oktw.muzeipixivsource.service

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import one.oktw.muzeipixivsource.provider.Commands
import one.oktw.muzeipixivsource.provider.Commands.COMMAND_DOWNLOAD
import one.oktw.muzeipixivsource.provider.Commands.COMMAND_OPEN
import one.oktw.muzeipixivsource.provider.Commands.COMMAND_SHARE
import one.oktw.muzeipixivsource.util.getParcelableExtraCompat
import one.oktw.muzeipixivsource.util.getSerializableExtraCompat
import java.io.File

class CommandHandler : BroadcastReceiver() {
    companion object {
        const val INTENT_COMMAND = "command"

        // Open command extra data
        const val INTENT_OPEN_URI = "artwork"

        // Share command extra data
        const val INTENT_SHARE_FILENAME = "filename"
        const val INTENT_SHARE_CACHE_FILE = "cache_file"
        const val INTENT_SHARE_FILE_URI = "file_uri"
        const val INTENT_SHARE_TITLE = "title"
        const val INTENT_SHARE_TEXT = "text"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getSerializableExtraCompat<Commands>(INTENT_COMMAND)) {
            COMMAND_OPEN -> context.startActivity(Intent(Intent.ACTION_VIEW, intent.getParcelableExtraCompat(INTENT_OPEN_URI)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            COMMAND_SHARE -> {
                val cacheFile = intent.getStringExtra(INTENT_SHARE_FILENAME)?.let {
                    context.cacheDir?.resolve("share")?.apply { mkdir() }?.apply { deleteOnExit() }?.resolve(it)
                } ?: return

                if (!cacheFile.exists()) {
                    val source = intent.getParcelableExtraCompat<Uri>(INTENT_SHARE_FILE_URI)?.let(context.contentResolver::openInputStream)
                        ?: intent.getSerializableExtraCompat<File>(INTENT_SHARE_CACHE_FILE)?.inputStream()
                        ?: return
                    cacheFile.outputStream().use { file -> source.use { it.copyTo(file) } }
                }

                Intent(Intent.ACTION_SEND).apply {
                    val title = intent.getStringExtra(INTENT_SHARE_TITLE)
                    val shareUri = FileProvider.getUriForFile(context, "one.oktw.muzeipixivsource.share", cacheFile)
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(INTENT_SHARE_TEXT))
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    clipData = ClipData(title, arrayOf("image/*"), ClipData.Item(shareUri))
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    type = "image/*"
                }.let { context.startActivity(Intent.createChooser(it, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            COMMAND_DOWNLOAD -> Unit // TODO
            else -> Unit
        }
    }
}
