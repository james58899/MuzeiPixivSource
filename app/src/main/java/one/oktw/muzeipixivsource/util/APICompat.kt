package one.oktw.muzeipixivsource.util

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Build
import android.os.Parcelable
import java.io.Serializable

inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(name: String): T? {
    return if (Build.VERSION.SDK_INT >= 33) {
        getSerializableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializableExtra(name) as? T
    }
}

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String): T? {
    return if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name) as? T
    }
}

fun PackageManager.getProviderInfoCompat(component: ComponentName, flags: PackageManager.ComponentInfoFlags): ProviderInfo {
    return if (Build.VERSION.SDK_INT >= 33) {
        getProviderInfo(component, flags)
    } else {
        @Suppress("DEPRECATION")
        getProviderInfo(component, flags.value.toInt())
    }
}

fun PackageManager.getPackageInfoCompat(packageName: String, flags: PackageManager.PackageInfoFlags): PackageInfo {
    return if (Build.VERSION.SDK_INT >= 33) {
        getPackageInfo(packageName, flags)
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, flags.value.toInt())
    }
}
