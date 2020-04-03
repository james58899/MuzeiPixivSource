package one.oktw.muzeipixivsource.service

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import one.oktw.muzeipixivsource.provider.Commands
import one.oktw.muzeipixivsource.provider.Commands.*
import java.io.File

class CommandHandler : BroadcastReceiver() {
    companion object {
        const val INTENT_COMMAND = "command"

        // Open command extra data
        const val INTENT_OPEN_URI = "artwork"

        // Share command extra data
        const val INTENT_SHARE_FILENAME = "filename"
        const val INTENT_SHARE_CACHE_FILE = "cache_file"
        const val INTENT_SHARE_TITLE = "title"
        const val INTENT_SHARE_TEXT = "text"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getSerializableExtra(INTENT_COMMAND) as Commands) {
            COMMAND_OPEN -> context.startActivity(Intent(Intent.ACTION_VIEW, intent.getParcelableExtra(INTENT_OPEN_URI)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            COMMAND_SHARE -> {
                val cacheFile = intent.getStringExtra(INTENT_SHARE_FILENAME)?.let {
                    context.cacheDir?.resolve("share")?.apply { mkdir() }?.apply { deleteOnExit() }?.resolve(it)
                } ?: return

                if (!cacheFile.exists()) (intent.getSerializableExtra(INTENT_SHARE_CACHE_FILE) as File?)?.copyTo(cacheFile) ?: return

                Intent(Intent.ACTION_SEND).apply {
                    val shareUri = FileProvider.getUriForFile(context, "one.oktw.muzeipixivsource.share", cacheFile)
                    putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(INTENT_SHARE_TEXT))
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    clipData = ClipData(intent.getStringExtra(INTENT_SHARE_TITLE), arrayOf("image/*"), ClipData.Item(shareUri))
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    type = "image/*"
                }.let { context.startActivity(Intent.createChooser(it, intent.getStringExtra(INTENT_SHARE_TITLE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            COMMAND_DOWNLOAD -> Unit // TODO
        }
    }
}
