package one.oktw.muzeipixivsource.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class AppUtil {
    companion object {
        val GSON: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

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
