package com.synapse.social.studioasinc.data.repository

import android.content.Context
import android.util.Log
import com.synapse.social.studioasinc.data.local.database.SettingsDataStore
import com.synapse.social.studioasinc.ui.settings.AppearanceSettings
import com.synapse.social.studioasinc.ui.settings.ContentVisibility
import com.synapse.social.studioasinc.ui.settings.FontScale
import com.synapse.social.studioasinc.ui.settings.GroupPrivacy
import com.synapse.social.studioasinc.ui.settings.NotificationCategory
import com.synapse.social.studioasinc.ui.settings.NotificationPreferences
import com.synapse.social.studioasinc.ui.settings.PrivacySettings
import com.synapse.social.studioasinc.ui.settings.ProfileVisibility
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.model.update.AppUpdateInfo
import com.synapse.social.studioasinc.ui.settings.ThemeMode
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import com.synapse.social.studioasinc.shared.data.local.AndroidSecureStorage



class SettingsRepositoryImpl private constructor(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    private val secureStorage by lazy { AndroidSecureStorage(context) }

    companion object {
        private const val TAG = "SettingsRepositoryImpl"

        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_APP_LOCK_TIMEOUT = "app_lock_timeout"
        private const val KEY_APP_LOCK_METHOD = "app_lock_method"
        private const val KEY_CHAT_LOCK_ENABLED = "chat_lock_enabled"
        private const val KEY_LOCKED_CHAT_IDS = "locked_chat_ids"
        private const val KEY_CHAT_LOCK_METHOD = "chat_lock_method"

        @Volatile
        private var INSTANCE: SettingsRepositoryImpl? = null

        fun getInstance(context: Context): SettingsRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                val dataStore = SettingsDataStore.getInstance(context)
                INSTANCE ?: SettingsRepositoryImpl(
                    context.applicationContext,
                    dataStore
                ).also { INSTANCE = it }
            }
        }
    }



    private val _cacheSize = MutableStateFlow(0L)





    override val themeMode: Flow<ThemeMode> = settingsDataStore.themeMode

    override val dynamicColorEnabled: Flow<Boolean> = settingsDataStore.dynamicColorEnabled

    override val fontScale: Flow<FontScale> = settingsDataStore.fontScale

    override val appearanceSettings: Flow<AppearanceSettings> = settingsDataStore.appearanceSettings

    override suspend fun setThemeMode(mode: ThemeMode) {
        settingsDataStore.setThemeMode(mode)
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        settingsDataStore.setDynamicColorEnabled(enabled)
    }

    override suspend fun setFontScale(scale: FontScale) {
        settingsDataStore.setFontScale(scale)
    }

    override suspend fun setPostViewStyle(style: com.synapse.social.studioasinc.ui.settings.PostViewStyle) {
        settingsDataStore.setPostViewStyle(style)
    }

    override val language: Flow<String> = settingsDataStore.language

    override suspend fun setLanguage(languageCode: String) {
        settingsDataStore.setLanguage(languageCode)
    }





    override val profileVisibility: Flow<ProfileVisibility> = settingsDataStore.profileVisibility

    override val contentVisibility: Flow<ContentVisibility> = settingsDataStore.contentVisibility

    override val biometricLockEnabled: Flow<Boolean> = settingsDataStore.biometricLockEnabled

    override val twoFactorEnabled: Flow<Boolean> = settingsDataStore.twoFactorEnabled

    override val privacySettings: Flow<PrivacySettings> = settingsDataStore.privacySettings

    override suspend fun setProfileVisibility(visibility: ProfileVisibility) {
        settingsDataStore.setProfileVisibility(visibility)
    }

    override suspend fun setContentVisibility(visibility: ContentVisibility) {
        settingsDataStore.setContentVisibility(visibility)
    }

    override suspend fun setGroupPrivacy(privacy: GroupPrivacy) {
        settingsDataStore.setGroupPrivacy(privacy)
    }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        settingsDataStore.setBiometricLockEnabled(enabled)
    }

    override suspend fun setTwoFactorEnabled(enabled: Boolean) {
        settingsDataStore.setTwoFactorEnabled(enabled)
    }





    override val notificationPreferences: Flow<NotificationPreferences> =
        settingsDataStore.notificationPreferences

    override suspend fun updateNotificationPreference(category: NotificationCategory, enabled: Boolean) {
        settingsDataStore.updateNotificationPreference(category, enabled)
    }

    override suspend fun setInAppNotificationsEnabled(enabled: Boolean) {
        settingsDataStore.setInAppNotificationsEnabled(enabled)
    }



    override suspend fun setReadReceiptsEnabled(enabled: Boolean) {
        settingsDataStore.setReadReceiptsEnabled(enabled)
    }

    override val chatFontScale: Flow<Float> = settingsDataStore.chatFontScale

    override suspend fun setChatFontScale(scale: Float) {
        settingsDataStore.setChatFontScale(scale)
    }

    override val chatThemePreset: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset> = settingsDataStore.chatThemePreset

    override suspend fun setChatThemePreset(preset: com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset) {
        settingsDataStore.setChatThemePreset(preset)
    }

    override val chatWallpaperType: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType> = settingsDataStore.chatWallpaperType

    override suspend fun setChatWallpaperType(type: com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType) {
        settingsDataStore.setChatWallpaperType(type)
    }

    override val chatWallpaperValue: Flow<String?> = settingsDataStore.chatWallpaperValue

    override suspend fun setChatWallpaperValue(value: String?) {
        settingsDataStore.setChatWallpaperValue(value)
    }

    override val chatWallpaperBlur: Flow<Float> = settingsDataStore.chatWallpaperBlur

    override suspend fun setChatWallpaperBlur(blur: Float) {
        settingsDataStore.setChatWallpaperBlur(blur)
    }


    override val chatMessageCornerRadius: Flow<Int> = settingsDataStore.chatMessageCornerRadius

    override suspend fun setChatMessageCornerRadius(radius: Int) {
        settingsDataStore.setChatMessageCornerRadius(radius)
    }

    override val chatListLayout: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout> = settingsDataStore.chatListLayout

    override suspend fun setChatListLayout(layout: com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout) {
        settingsDataStore.setChatListLayout(layout)
    }

    override val chatSwipeGesture: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture> = settingsDataStore.chatSwipeGesture

    override suspend fun setChatSwipeGesture(gesture: com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture) {
        settingsDataStore.setChatSwipeGesture(gesture)
    }


    override val messageSuggestionEnabled: Flow<Boolean> = settingsDataStore.messageSuggestionEnabled

    override suspend fun setMessageSuggestionEnabled(enabled: Boolean) {
        settingsDataStore.setMessageSuggestionEnabled(enabled)
    }

    override val chatAvatarDisabled: Flow<Boolean> = settingsDataStore.chatAvatarDisabled

    override suspend fun setChatAvatarDisabled(enabled: Boolean) {
        settingsDataStore.setChatAvatarDisabled(enabled)
    }

    override val chatMaxMessageChunkSize: Flow<Int> = settingsDataStore.chatMaxMessageChunkSize

    override suspend fun setChatMaxMessageChunkSize(size: Int) {
        settingsDataStore.setChatMaxMessageChunkSize(size)
    }

    override val chatFoldersJson: Flow<String?> = settingsDataStore.chatFoldersJson

    override suspend fun setChatFoldersJson(json: String) {
        settingsDataStore.setChatFoldersJson(json)
    }

    override val mediaUploadQuality: Flow<com.synapse.social.studioasinc.ui.settings.MediaUploadQuality> = settingsDataStore.mediaUploadQuality

    override suspend fun setMediaUploadQuality(quality: com.synapse.social.studioasinc.ui.settings.MediaUploadQuality) {
        settingsDataStore.setMediaUploadQuality(quality)
    }

    override val useLessDataCalls: Flow<Boolean> = settingsDataStore.useLessDataCalls

    override suspend fun setUseLessDataCalls(enabled: Boolean) {
        settingsDataStore.setUseLessDataCalls(enabled)
    }

    override val autoDownloadRules: Flow<com.synapse.social.studioasinc.ui.settings.AutoDownloadRules> = settingsDataStore.autoDownloadRules

    override suspend fun setAutoDownloadRule(
        networkType: String,
        mediaTypes: Set<com.synapse.social.studioasinc.ui.settings.MediaType>
    ) {
        settingsDataStore.setAutoDownloadRule(networkType, mediaTypes)
    }


    override val cacheSize: Flow<Long> = _cacheSize.asStateFlow()

    override val keepMediaDays: Flow<Int> = settingsDataStore.keepMediaDays
    override val maxCacheSizeGB: Flow<Int> = settingsDataStore.maxCacheSizeGB

    override suspend fun setKeepMediaDays(days: Int) {
        settingsDataStore.setKeepMediaDays(days)
    }

    override suspend fun setMaxCacheSizeGB(gb: Int) {
        settingsDataStore.setMaxCacheSizeGB(gb)
    }




    override suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        val sizeBefore = calculateCacheSize()

        try {
            context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            context.codeCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }

        val sizeAfter = calculateCacheSize()
        val freedSpace = sizeBefore - sizeAfter

        Log.d(TAG, "Cache cleared: freed ${freedSpace / 1024}KB")
        freedSpace
    }



    override suspend fun calculateCacheSize(): Long = withContext(Dispatchers.IO) {
        var totalSize = 0L

        try {
            context.cacheDir?.let { totalSize += getDirectorySize(it) }
            context.externalCacheDir?.let { totalSize += getDirectorySize(it) }
            context.codeCacheDir?.let { totalSize += getDirectorySize(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
        }

        _cacheSize.value = totalSize
        totalSize
    }



    private fun getDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        var size = 0L
        val stack = java.util.ArrayDeque<File>()
        stack.push(directory)
        while (stack.isNotEmpty()) {
            val dir = stack.pop()
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        size += file.length()
                    } else if (file.isDirectory) {
                        stack.push(file)
                    }
                }
            }
        }
        return size
    }

    private val storageCalculator by lazy { 
        com.synapse.social.studioasinc.core.util.StorageCalculator(context) 
    }

    override suspend fun getStorageBreakdown(): com.synapse.social.studioasinc.ui.settings.StorageUsageBreakdown {
        return try {
            storageCalculator.getStorageBreakdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting storage breakdown", e)
            com.synapse.social.studioasinc.ui.settings.StorageUsageBreakdown(
                totalSize = 0L,
                usedSize = 0L,
                freeSize = 0L,
                appsAndOtherSize = 0L,
                synapseSize = 0L,
                photoSize = 0L,
                videoSize = 0L,
                documentSize = 0L,
                otherSize = 0L,
                chatSize = 0L
            )
        }
    }

    override suspend fun getLargeFiles(minSizeBytes: Long): List<com.synapse.social.studioasinc.ui.settings.LargeFileInfo> {
        return try {
            storageCalculator.getLargeFiles(minSizeBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting large files", e)
            emptyList()
        }
    }



    override val dataSaverEnabled: Flow<Boolean> = settingsDataStore.dataSaverEnabled

    override suspend fun setDataSaverEnabled(enabled: Boolean) {
        settingsDataStore.setDataSaverEnabled(enabled)
    }

    override suspend fun setEnterIsSendEnabled(enabled: Boolean) {
        settingsDataStore.setEnterIsSendEnabled(enabled)
    }

    override suspend fun setMediaVisibilityEnabled(enabled: Boolean) {
        settingsDataStore.setMediaVisibilityEnabled(enabled)
    }

    override suspend fun setVoiceTranscriptsEnabled(enabled: Boolean) {
        settingsDataStore.setVoiceTranscriptsEnabled(enabled)
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        settingsDataStore.setAutoBackupEnabled(enabled)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        settingsDataStore.setRemindersEnabled(enabled)
    }

    override suspend fun setHighPriorityEnabled(enabled: Boolean) {
        settingsDataStore.setHighPriorityEnabled(enabled)
    }

    override suspend fun setReactionNotificationsEnabled(enabled: Boolean) {
        settingsDataStore.setReactionNotificationsEnabled(enabled)
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        secureStorage.save(KEY_APP_LOCK_ENABLED, enabled.toString())
        secureStorage.save(KEY_APP_LOCK_TIMEOUT, "immediate")
        secureStorage.save(KEY_APP_LOCK_METHOD, "biometric")
    }

    override suspend fun setChatLockEnabled(enabled: Boolean) {
        secureStorage.save(KEY_CHAT_LOCK_ENABLED, enabled.toString())
        secureStorage.save(KEY_LOCKED_CHAT_IDS, "")
        secureStorage.save(KEY_CHAT_LOCK_METHOD, "biometric")
    }








    override suspend fun clearUserSettings() {
        settingsDataStore.clearUserSettings()
    }



    override suspend fun clearAllSettings() {
        settingsDataStore.clearAllSettings()
    }



    override suspend fun restoreDefaults() {
        settingsDataStore.restoreDefaults()
    }

    override suspend fun checkForUpdates(): Result<AppUpdateInfo?> {
        return try {
            val latestVersion = SupabaseClient.client
                .from("app_versions")
                .select() {
                    order("version_code", Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<AppUpdateInfo>()

            Result.success(latestVersion)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            Result.failure(e)
        }
    }

    override val hideProfilePicSuggestion: Flow<Boolean> = settingsDataStore.hideProfilePicSuggestion

    override suspend fun setHideProfilePicSuggestion(hide: Boolean) {
        settingsDataStore.setHideProfilePicSuggestion(hide)
    }
}
