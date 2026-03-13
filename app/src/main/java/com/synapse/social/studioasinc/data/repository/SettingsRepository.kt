package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.ui.settings.AppearanceSettings
import com.synapse.social.studioasinc.ui.settings.ContentVisibility
import com.synapse.social.studioasinc.ui.settings.FontScale
import com.synapse.social.studioasinc.ui.settings.GroupPrivacy
import com.synapse.social.studioasinc.ui.settings.MediaAutoDownload
import com.synapse.social.studioasinc.ui.settings.NotificationCategory
import com.synapse.social.studioasinc.ui.settings.NotificationPreferences
import com.synapse.social.studioasinc.ui.settings.PrivacySettings
import com.synapse.social.studioasinc.ui.settings.ProfileVisibility
import com.synapse.social.studioasinc.data.model.AppUpdateInfo
import com.synapse.social.studioasinc.ui.settings.ThemeMode
import kotlinx.coroutines.flow.Flow



interface SettingsRepository {







    val themeMode: Flow<ThemeMode>



    val dynamicColorEnabled: Flow<Boolean>



    val fontScale: Flow<FontScale>



    val appearanceSettings: Flow<AppearanceSettings>



    suspend fun setThemeMode(mode: ThemeMode)



    suspend fun setDynamicColorEnabled(enabled: Boolean)



    suspend fun setFontScale(scale: FontScale)



    suspend fun setPostViewStyle(style: com.synapse.social.studioasinc.ui.settings.PostViewStyle)



    val language: Flow<String>



    suspend fun setLanguage(languageCode: String)







    val profileVisibility: Flow<ProfileVisibility>



    val contentVisibility: Flow<ContentVisibility>



    val biometricLockEnabled: Flow<Boolean>



    val twoFactorEnabled: Flow<Boolean>



    val privacySettings: Flow<PrivacySettings>



    suspend fun setProfileVisibility(visibility: ProfileVisibility)



    suspend fun setContentVisibility(visibility: ContentVisibility)



    suspend fun setGroupPrivacy(privacy: GroupPrivacy)



    suspend fun setBiometricLockEnabled(enabled: Boolean)



    suspend fun setTwoFactorEnabled(enabled: Boolean)







    val notificationPreferences: Flow<NotificationPreferences>



    suspend fun updateNotificationPreference(category: NotificationCategory, enabled: Boolean)



    suspend fun setInAppNotificationsEnabled(enabled: Boolean)






    suspend fun setReadReceiptsEnabled(enabled: Boolean)

    val chatFontScale: Flow<Float>
    suspend fun setChatFontScale(scale: Float)

    val chatThemePreset: Flow<com.synapse.social.studioasinc.domain.model.ChatThemePreset>
    suspend fun setChatThemePreset(preset: com.synapse.social.studioasinc.domain.model.ChatThemePreset)

    val chatWallpaperType: Flow<com.synapse.social.studioasinc.domain.model.WallpaperType>
    suspend fun setChatWallpaperType(type: com.synapse.social.studioasinc.domain.model.WallpaperType)

    val chatWallpaperValue: Flow<String?>
    suspend fun setChatWallpaperValue(value: String?)

    val chatWallpaperBlur: Flow<Float>
    suspend fun setChatWallpaperBlur(blur: Float)


    val chatMessageCornerRadius: Flow<Int>
    suspend fun setChatMessageCornerRadius(radius: Int)

    val chatListLayout: Flow<com.synapse.social.studioasinc.domain.model.ChatListLayout>
    suspend fun setChatListLayout(layout: com.synapse.social.studioasinc.domain.model.ChatListLayout)

    val chatSwipeGesture: Flow<com.synapse.social.studioasinc.domain.model.ChatSwipeGesture>
    suspend fun setChatSwipeGesture(gesture: com.synapse.social.studioasinc.domain.model.ChatSwipeGesture)

    val chatFoldersJson: Flow<String?>
    suspend fun setChatFoldersJson(json: String)

    val mediaUploadQuality: Flow<com.synapse.social.studioasinc.ui.settings.MediaUploadQuality>



    suspend fun setMediaUploadQuality(quality: com.synapse.social.studioasinc.ui.settings.MediaUploadQuality)



    val useLessDataCalls: Flow<Boolean>



    suspend fun setUseLessDataCalls(enabled: Boolean)



    val autoDownloadRules: Flow<com.synapse.social.studioasinc.ui.settings.AutoDownloadRules>



    suspend fun setAutoDownloadRule(
        networkType: String,
        mediaTypes: Set<com.synapse.social.studioasinc.ui.settings.MediaType>
    )



    val cacheSize: Flow<Long>



    suspend fun clearCache(): Long



    suspend fun calculateCacheSize(): Long

    suspend fun getStorageBreakdown(): com.synapse.social.studioasinc.ui.settings.StorageUsageBreakdown

    suspend fun getLargeFiles(minSizeBytes: Long): List<com.synapse.social.studioasinc.ui.settings.LargeFileInfo>

    val dataSaverEnabled: Flow<Boolean>

    suspend fun setDataSaverEnabled(enabled: Boolean)



    suspend fun setEnterIsSendEnabled(enabled: Boolean)



    suspend fun setMediaVisibilityEnabled(enabled: Boolean)



    suspend fun setVoiceTranscriptsEnabled(enabled: Boolean)



    suspend fun setAutoBackupEnabled(enabled: Boolean)



    suspend fun setRemindersEnabled(enabled: Boolean)



    suspend fun setHighPriorityEnabled(enabled: Boolean)



    suspend fun setReactionNotificationsEnabled(enabled: Boolean)



    suspend fun setAppLockEnabled(enabled: Boolean)



    suspend fun setChatLockEnabled(enabled: Boolean)







    suspend fun clearUserSettings()



    suspend fun clearAllSettings()



    suspend fun restoreDefaults()



    suspend fun checkForUpdates(): Result<AppUpdateInfo?>
}
