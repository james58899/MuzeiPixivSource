package one.oktw.muzeipixivsource.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import one.oktw.muzeipixivsource.provider.Commands
import one.oktw.muzeipixivsource.provider.Commands.COMMAND_DOWNLOAD
import one.oktw.muzeipixivsource.provider.Commands.COMMAND_OPEN
import one.oktw.muzeipixivsource.provider.Commands.COMMAND_SHARE
import one.oktw.muzeipixivsource.util.getSerializableExtraCompat

class CommandHandler : BroadcastReceiver() {
    companion object {
        const val INTENT_COMMAND = "command"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getSerializableExtraCompat<Commands>(INTENT_COMMAND)) {
            // Android 12 restriction start activity from broadcast, so direct start activity to open share dialog.
            // https://developer.android.com/about/versions/12/behavior-changes-12#notification-trampolines
            COMMAND_OPEN, COMMAND_SHARE -> Unit
            COMMAND_DOWNLOAD -> Unit // TODO
            else -> Unit
        }
    }
}
