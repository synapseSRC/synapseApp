package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType
import com.synapse.social.studioasinc.ui.settings.AutoDownloadRules
import com.synapse.social.studioasinc.ui.settings.MediaType
import kotlinx.coroutines.flow.first

object AutoDownloadManager {
    enum class NetworkType {
        WIFI, MOBILE, ROAMING, NONE
    }

    fun getActiveNetworkType(context: Context): NetworkType {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkType.NONE

        // minSdk is 26, so activeNetwork/getNetworkCapabilities are always available and roaming is
        // readable straight from the capabilities (NET_CAPABILITY_NOT_ROAMING is API 21+). The
        // deprecated NetworkInfo fallbacks this function used to branch into — including the
        // pre-API-23 path that could never run here — are gone.
        val network = cm.activeNetwork ?: return NetworkType.NONE
        val capabilities = cm.getNetworkCapabilities(network) ?: return NetworkType.NONE

        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkType.NONE
        }

        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)) {
            return NetworkType.ROAMING
        }

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            else -> NetworkType.WIFI
        }
    }

    suspend fun isAutoDownloadAllowed(
        context: Context,
        mediaType: MediaType
    ): Boolean {
        val repo = SettingsRepositoryImpl.getInstance(context)
        val rules = try {
            repo.autoDownloadRules.first()
        } catch (e: Exception) {
            AutoDownloadRules()
        }
        return isAutoDownloadAllowed(context, rules, mediaType)
    }

    fun isAutoDownloadAllowed(
        context: Context,
        rules: AutoDownloadRules,
        mediaType: MediaType
    ): Boolean {
        val networkType = getActiveNetworkType(context)
        return isAutoDownloadAllowedForNetwork(networkType, rules, mediaType)
    }

    fun isAutoDownloadAllowedForNetwork(
        networkType: NetworkType,
        rules: AutoDownloadRules,
        mediaType: MediaType
    ): Boolean {
        val allowedTypes = when (networkType) {
            NetworkType.WIFI -> rules.wifi
            NetworkType.MOBILE -> rules.mobileData
            NetworkType.ROAMING -> rules.roaming
            NetworkType.NONE -> emptySet()
        }
        return mediaType in allowedTypes
    }
}
