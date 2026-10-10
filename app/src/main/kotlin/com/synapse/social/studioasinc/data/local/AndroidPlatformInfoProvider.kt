package com.synapse.social.studioasinc.data.local

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.synapse.social.studioasinc.shared.domain.repository.PlatformInfoProvider

class AndroidPlatformInfoProvider(private val context: Context) : PlatformInfoProvider {

    override fun getAppVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
