package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return NetworkType.NONE
            val capabilities = cm.getNetworkCapabilities(network) ?: return NetworkType.NONE

            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return NetworkType.NONE
            }

            val isRoaming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
            } else {
                @Suppress("DEPRECATED")
                val activeNetworkInfo = cm.activeNetworkInfo
                activeNetworkInfo?.isRoaming == true
            }

            if (isRoaming) return NetworkType.ROAMING

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                else -> NetworkType.WIFI
            }
        } else {
            @Suppress("DEPRECATED")
            val activeNetworkInfo = cm.activeNetworkInfo ?: return NetworkType.NONE
            if (!activeNetworkInfo.isConnected) return NetworkType.NONE
            if (activeNetworkInfo.isRoaming) return NetworkType.ROAMING

            return when (activeNetworkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.MOBILE
                else -> NetworkType.WIFI
            }
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

    fun attachmentTypeToMediaType(attachmentType: AttachmentType): MediaType {
        return when (attachmentType) {
            AttachmentType.IMAGE -> MediaType.PHOTO
            AttachmentType.VIDEO -> MediaType.VIDEO
            AttachmentType.AUDIO -> MediaType.AUDIO
            AttachmentType.FILE -> MediaType.DOCUMENT
        }
    }
}
