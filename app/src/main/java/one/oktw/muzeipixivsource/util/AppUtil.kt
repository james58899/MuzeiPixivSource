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
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.internal.platform.Platform
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.hack.DisableSNISSLSocketFactory
import java.net.InetAddress

class AppUtil {
    companion object {
        const val MUZEI_PACKAGE = "net.nurik.roman.muzei"
        val GSON: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        val httpClient = OkHttpClient().newBuilder()
            .connectionSpecs(listOf(ConnectionSpec.Builder(ConnectionSpec.RESTRICTED_TLS).supportsTlsExtensions(false).build()))
            .sslSocketFactory(DisableSNISSLSocketFactory(), Platform.get().platformTrustManager())
            .dns(
                DnsOverHttps.Builder()
                    .client(OkHttpClient())
                    .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                    .post(true)
                    .bootstrapDnsHosts(
                        InetAddress.getByName("104.16.249.249"),
                        InetAddress.getByName("104.16.248.249"),
                        InetAddress.getByName("1.1.1.1"),
                        InetAddress.getByName("1.0.0.1")
                    )
                    .build()
            )
            .build()

        fun checkInstalled(context: Context, packageName: String): Boolean {
            return try {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        fun showInstallDialog(context: Context, packageName: String) {
            AlertDialog.Builder(context)
                .setTitle(R.string.muzei_not_install_title)
                .setMessage(R.string.muzei_not_install_message)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                    }
                }
                .setNegativeButton(android.R.string.no, null)
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
