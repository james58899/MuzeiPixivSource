package one.oktw.muzeipixivsource.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import one.oktw.muzeipixivsource.R

class AppUtil {
    companion object {
        const val MUZEI_PACKAGE = "net.nurik.roman.muzei"
        val GSON: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

        fun checkInstalled(context: Context, packageName: String): Boolean {
            return try {
                context.packageManager.getPackageInfoCompat(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        fun showInstallDialog(context: Context, packageName: String) {
            AlertDialog.Builder(context)
                .setTitle(R.string.muzei_not_install_title)
                .setMessage(R.string.muzei_not_install_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .show()
        }

        fun launchOrMarket(context: Context, packageName: String) {
            if (checkInstalled(context, packageName)) {
                context.startActivity(context.packageManager.getLaunchIntentForPackage(packageName))
            } else {
                showInstallDialog(context, packageName)
            }
        }
    }
}
