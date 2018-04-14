package one.oktw.muzeipixivsource.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri

class AppUtil {
    companion object {
        fun launchOrMarket(context: Context, packageName: String): Intent {
            return if (checkInstalled(context, packageName)) launchFromPackage(context, packageName)!! else viewMarket(
                packageName
            )
        }

        fun launchFromPackage(context: Context, packageName: String): Intent? =
            context.packageManager.getLaunchIntentForPackage(packageName)

        fun viewMarket(packageName: String) = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())

        fun checkInstalled(context: Context, packageName: String): Boolean {
            return try {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}