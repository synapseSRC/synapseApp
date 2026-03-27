package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.update.AppUpdateInfo
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import com.synapse.social.studioasinc.shared.domain.model.settings.AppearanceSettings
import com.synapse.social.studioasinc.shared.domain.model.settings.AutoDownloadRules
import com.synapse.social.studioasinc.shared.domain.model.settings.ContentVisibility
import com.synapse.social.studioasinc.shared.domain.model.settings.FontScale
import com.synapse.social.studioasinc.shared.domain.model.settings.GroupPrivacy
import com.synapse.social.studioasinc.shared.domain.model.settings.LargeFileInfo
import com.synapse.social.studioasinc.shared.domain.model.settings.MediaType
import com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality
import com.synapse.social.studioasinc.shared.domain.model.settings.NotificationCategory
import com.synapse.social.studioasinc.shared.domain.model.settings.NotificationPreferences
import com.synapse.social.studioasinc.shared.domain.model.settings.PostViewStyle
import com.synapse.social.studioasinc.shared.domain.model.settings.PrivacySettings
import com.synapse.social.studioasinc.shared.domain.model.settings.ProfileVisibility
import com.synapse.social.studioasinc.shared.domain.model.settings.StorageUsageBreakdown
import com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val dynamicColorEnabled: Flow<Boolean>
    val fontScale: Flow<FontScale>
    val appearanceSettings: Flow<AppearanceSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setFontScale(scale: FontScale)
    suspend fun setPostViewStyle(style: PostViewStyle)
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
    val chatThemePreset: Flow<ChatThemePreset>
    suspend fun setChatThemePreset(preset: ChatThemePreset)
    val chatWallpaperType: Flow<WallpaperType>
    suspend fun setChatWallpaperType(type: WallpaperType)
    val chatWallpaperValue: Flow<String?>
    suspend fun setChatWallpaperValue(value: String?)
    val chatWallpaperBlur: Flow<Float>
    suspend fun setChatWallpaperBlur(blur: Float)
    val chatMessageCornerRadius: Flow<Int>
    suspend fun setChatMessageCornerRadius(radius: Int)

    val messageSuggestionEnabled: Flow<Boolean>
    suspend fun setMessageSuggestionEnabled(enabled: Boolean)

    val chatListLayout: Flow<ChatListLayout>
    suspend fun setChatListLayout(layout: ChatListLayout)
    val chatSwipeGesture: Flow<ChatSwipeGesture>
    suspend fun setChatSwipeGesture(gesture: ChatSwipeGesture)
    val chatFoldersJson: Flow<String?>
    suspend fun setChatFoldersJson(json: String)
    val mediaUploadQuality: Flow<MediaUploadQuality>
    suspend fun setMediaUploadQuality(quality: MediaUploadQuality)
    val useLessDataCalls: Flow<Boolean>
    suspend fun setUseLessDataCalls(enabled: Boolean)
    val autoDownloadRules: Flow<AutoDownloadRules>
    suspend fun setAutoDownloadRule(networkType: String, mediaTypes: Set<MediaType>)
    val cacheSize: Flow<Long>
    val keepMediaDays: Flow<Int>
    val maxCacheSizeGB: Flow<Int>
    suspend fun setKeepMediaDays(days: Int)
    suspend fun setMaxCacheSizeGB(gb: Int)
    suspend fun clearCache(): Long
    suspend fun calculateCacheSize(): Long
    suspend fun getStorageBreakdown(): StorageUsageBreakdown
    suspend fun getLargeFiles(minSizeBytes: Long): List<LargeFileInfo>
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
    val hideProfilePicSuggestion: Flow<Boolean>
    suspend fun setHideProfilePicSuggestion(hide: Boolean)
}
