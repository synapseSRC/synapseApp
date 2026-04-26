package com.synapse.social.studioasinc.data.repository

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
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository as DomainSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.synapse.social.studioasinc.ui.settings.ThemeMode as UiThemeMode
import com.synapse.social.studioasinc.ui.settings.FontScale as UiFontScale
import com.synapse.social.studioasinc.ui.settings.PostViewStyle as UiPostViewStyle
import com.synapse.social.studioasinc.ui.settings.ProfileVisibility as UiProfileVisibility
import com.synapse.social.studioasinc.ui.settings.ContentVisibility as UiContentVisibility
import com.synapse.social.studioasinc.ui.settings.GroupPrivacy as UiGroupPrivacy
import com.synapse.social.studioasinc.ui.settings.NotificationCategory as UiNotificationCategory
import com.synapse.social.studioasinc.ui.settings.MediaUploadQuality as UiMediaUploadQuality
import com.synapse.social.studioasinc.ui.settings.MediaType as UiMediaType

/**
 * Adapts the data-layer SettingsRepository to the domain SettingsRepository interface,
 * mapping ui.settings.* ↔ domain.model.settings.* types by enum name.
 */
class DomainSettingsRepositoryAdapter @Inject constructor(
    private val delegate: SettingsRepositoryImpl
) : DomainSettingsRepository {

    // --- Appearance ---
    override val themeMode: Flow<ThemeMode> = delegate.themeMode.map { ThemeMode.valueOf(it.name) }
    override val dynamicColorEnabled: Flow<Boolean> = delegate.dynamicColorEnabled
    override val fontScale: Flow<FontScale> = delegate.fontScale.map { FontScale.valueOf(it.name) }
    override val appearanceSettings: Flow<AppearanceSettings> = delegate.appearanceSettings.map { it.toDomain() }

    override suspend fun setThemeMode(mode: ThemeMode) = delegate.setThemeMode(UiThemeMode.valueOf(mode.name))
    override suspend fun setDynamicColorEnabled(enabled: Boolean) = delegate.setDynamicColorEnabled(enabled)
    override suspend fun setFontScale(scale: FontScale) = delegate.setFontScale(UiFontScale.valueOf(scale.name))
    override suspend fun setPostViewStyle(style: PostViewStyle) = delegate.setPostViewStyle(UiPostViewStyle.valueOf(style.name))

    // --- Language ---
    override val language: Flow<String> = delegate.language
    override suspend fun setLanguage(languageCode: String) = delegate.setLanguage(languageCode)

    // --- Privacy ---
    override val profileVisibility: Flow<ProfileVisibility> = delegate.profileVisibility.map { ProfileVisibility.valueOf(it.name) }
    override val contentVisibility: Flow<ContentVisibility> = delegate.contentVisibility.map { ContentVisibility.valueOf(it.name) }
    override val biometricLockEnabled: Flow<Boolean> = delegate.biometricLockEnabled
    override val twoFactorEnabled: Flow<Boolean> = delegate.twoFactorEnabled
    override val privacySettings: Flow<PrivacySettings> = delegate.privacySettings.map { it.toDomain() }

    override suspend fun setProfileVisibility(visibility: ProfileVisibility) = delegate.setProfileVisibility(UiProfileVisibility.valueOf(visibility.name))
    override suspend fun setContentVisibility(visibility: ContentVisibility) = delegate.setContentVisibility(UiContentVisibility.valueOf(visibility.name))
    override suspend fun setGroupPrivacy(privacy: GroupPrivacy) = delegate.setGroupPrivacy(UiGroupPrivacy.valueOf(privacy.name))
    override suspend fun setBiometricLockEnabled(enabled: Boolean) = delegate.setBiometricLockEnabled(enabled)
    override suspend fun setTwoFactorEnabled(enabled: Boolean) = delegate.setTwoFactorEnabled(enabled)

    // --- Notifications ---
    override val notificationPreferences: Flow<NotificationPreferences> = delegate.notificationPreferences.map { it.toDomain() }
    override suspend fun updateNotificationPreference(category: NotificationCategory, enabled: Boolean) =
        delegate.updateNotificationPreference(UiNotificationCategory.valueOf(category.name), enabled)
    override suspend fun setInAppNotificationsEnabled(enabled: Boolean) = delegate.setInAppNotificationsEnabled(enabled)
    override suspend fun setReadReceiptsEnabled(enabled: Boolean) = delegate.setReadReceiptsEnabled(enabled)

    // --- Chat ---
    override val chatFontScale: Flow<Float> = delegate.chatFontScale
    override suspend fun setChatFontScale(scale: Float) = delegate.setChatFontScale(scale)
    override val chatThemePreset: Flow<ChatThemePreset> = delegate.chatThemePreset
    override suspend fun setChatThemePreset(preset: ChatThemePreset) = delegate.setChatThemePreset(preset)
    override val chatWallpaperType: Flow<WallpaperType> = delegate.chatWallpaperType
    override suspend fun setChatWallpaperType(type: WallpaperType) = delegate.setChatWallpaperType(type)
    override val chatWallpaperValue: Flow<String?> = delegate.chatWallpaperValue
    override suspend fun setChatWallpaperValue(value: String?) = delegate.setChatWallpaperValue(value)
    override val chatWallpaperBlur: Flow<Float> = delegate.chatWallpaperBlur
    override suspend fun setChatWallpaperBlur(blur: Float) = delegate.setChatWallpaperBlur(blur)
    override val chatMessageCornerRadius: Flow<Int> = delegate.chatMessageCornerRadius
    override suspend fun setChatMessageCornerRadius(radius: Int) = delegate.setChatMessageCornerRadius(radius)

    override val messageSuggestionEnabled: Flow<Boolean> = delegate.messageSuggestionEnabled

    override suspend fun setMessageSuggestionEnabled(enabled: Boolean) {
        delegate.setMessageSuggestionEnabled(enabled)
    }

    override val chatListLayout: Flow<ChatListLayout> = delegate.chatListLayout
    override suspend fun setChatListLayout(layout: ChatListLayout) = delegate.setChatListLayout(layout)
    override val chatSwipeGesture: Flow<ChatSwipeGesture> = delegate.chatSwipeGesture
    override suspend fun setChatSwipeGesture(gesture: ChatSwipeGesture) = delegate.setChatSwipeGesture(gesture)
    override val chatFoldersJson: Flow<String?> = delegate.chatFoldersJson
    override suspend fun setChatFoldersJson(json: String) = delegate.setChatFoldersJson(json)

    // --- Media ---
    override val mediaUploadQuality: Flow<MediaUploadQuality> = delegate.mediaUploadQuality.map { MediaUploadQuality.valueOf(it.name) }
    override suspend fun setMediaUploadQuality(quality: MediaUploadQuality) = delegate.setMediaUploadQuality(UiMediaUploadQuality.valueOf(quality.name))
    override val useLessDataCalls: Flow<Boolean> = delegate.useLessDataCalls
    override suspend fun setUseLessDataCalls(enabled: Boolean) = delegate.setUseLessDataCalls(enabled)
    override val autoDownloadRules: Flow<AutoDownloadRules> = delegate.autoDownloadRules.map { it.toDomain() }
    override suspend fun setAutoDownloadRule(networkType: String, mediaTypes: Set<MediaType>) =
        delegate.setAutoDownloadRule(networkType, mediaTypes.map { UiMediaType.valueOf(it.name) }.toSet())

    // --- Storage ---
    override val cacheSize: Flow<Long> = delegate.cacheSize
    override val keepMediaDays: Flow<Int> = delegate.keepMediaDays
    override val maxCacheSizeGB: Flow<Int> = delegate.maxCacheSizeGB
    override suspend fun setKeepMediaDays(days: Int) = delegate.setKeepMediaDays(days)
    override suspend fun setMaxCacheSizeGB(gb: Int) = delegate.setMaxCacheSizeGB(gb)
    override suspend fun clearCache(): Long = delegate.clearCache()
    override suspend fun calculateCacheSize(): Long = delegate.calculateCacheSize()
    override suspend fun getStorageBreakdown(): StorageUsageBreakdown = delegate.getStorageBreakdown().toDomain()
    override suspend fun getLargeFiles(minSizeBytes: Long): List<LargeFileInfo> = delegate.getLargeFiles(minSizeBytes).map { it.toDomain() }

    override val chatAvatarDisabled: Flow<Boolean> = delegate.chatAvatarDisabled
    override suspend fun setChatAvatarDisabled(enabled: Boolean) = delegate.setChatAvatarDisabled(enabled)
    override val chatMessagePaginationLimit: Flow<Int> = delegate.chatMessagePaginationLimit
    override suspend fun setChatMessagePaginationLimit(limit: Int) = delegate.setChatMessagePaginationLimit(limit)

    // --- Misc ---
    override val dataSaverEnabled: Flow<Boolean> = delegate.dataSaverEnabled
    override suspend fun setDataSaverEnabled(enabled: Boolean) = delegate.setDataSaverEnabled(enabled)
    override suspend fun setEnterIsSendEnabled(enabled: Boolean) = delegate.setEnterIsSendEnabled(enabled)
    override suspend fun setMediaVisibilityEnabled(enabled: Boolean) = delegate.setMediaVisibilityEnabled(enabled)
    override suspend fun setVoiceTranscriptsEnabled(enabled: Boolean) = delegate.setVoiceTranscriptsEnabled(enabled)
    override suspend fun setAutoBackupEnabled(enabled: Boolean) = delegate.setAutoBackupEnabled(enabled)
    override suspend fun setRemindersEnabled(enabled: Boolean) = delegate.setRemindersEnabled(enabled)
    override suspend fun setHighPriorityEnabled(enabled: Boolean) = delegate.setHighPriorityEnabled(enabled)
    override suspend fun setReactionNotificationsEnabled(enabled: Boolean) = delegate.setReactionNotificationsEnabled(enabled)
    override suspend fun setAppLockEnabled(enabled: Boolean) = delegate.setAppLockEnabled(enabled)
    override suspend fun setChatLockEnabled(enabled: Boolean) = delegate.setChatLockEnabled(enabled)
    override suspend fun clearUserSettings() = delegate.clearUserSettings()
    override suspend fun clearAllSettings() = delegate.clearAllSettings()
    override suspend fun restoreDefaults() = delegate.restoreDefaults()
    override suspend fun checkForUpdates(): Result<AppUpdateInfo?> = delegate.checkForUpdates()
    override val hideProfilePicSuggestion: Flow<Boolean> = delegate.hideProfilePicSuggestion
    override suspend fun setHideProfilePicSuggestion(hide: Boolean) = delegate.setHideProfilePicSuggestion(hide)
}

// --- Mappers ---

private fun com.synapse.social.studioasinc.ui.settings.AppearanceSettings.toDomain() = AppearanceSettings(
    themeMode = ThemeMode.valueOf(themeMode.name),
    dynamicColorEnabled = dynamicColorEnabled,
    fontScale = FontScale.valueOf(fontScale.name),
    postViewStyle = PostViewStyle.valueOf(postViewStyle.name)
)

private fun com.synapse.social.studioasinc.ui.settings.PrivacySettings.toDomain() = PrivacySettings(
    profileVisibility = ProfileVisibility.valueOf(profileVisibility.name),
    twoFactorEnabled = twoFactorEnabled,
    biometricLockEnabled = biometricLockEnabled,
    contentVisibility = ContentVisibility.valueOf(contentVisibility.name),
    groupPrivacy = GroupPrivacy.valueOf(groupPrivacy.name),
    readReceiptsEnabled = readReceiptsEnabled,
    appLockEnabled = appLockEnabled,
    chatLockEnabled = chatLockEnabled
)

private fun com.synapse.social.studioasinc.ui.settings.NotificationPreferences.toDomain() = NotificationPreferences(
    globalEnabled = globalEnabled,
    likesEnabled = likesEnabled,
    commentsEnabled = commentsEnabled,
    repliesEnabled = repliesEnabled,
    followsEnabled = followsEnabled,
    mentionsEnabled = mentionsEnabled,
    newPostsEnabled = newPostsEnabled,
    sharesEnabled = sharesEnabled,
    securityEnabled = securityEnabled,
    updatesEnabled = updatesEnabled,
    quietHoursEnabled = quietHoursEnabled,
    quietHoursStart = quietHoursStart,
    quietHoursEnd = quietHoursEnd,
    doNotDisturb = doNotDisturb,
    dndUntil = dndUntil,
    inAppNotificationsEnabled = inAppNotificationsEnabled,
    remindersEnabled = remindersEnabled,
    highPriorityEnabled = highPriorityEnabled,
    reactionNotificationsEnabled = reactionNotificationsEnabled,
    messagesEnabled = messagesEnabled
)

private fun com.synapse.social.studioasinc.ui.settings.AutoDownloadRules.toDomain() = AutoDownloadRules(
    mobileData = mobileData.map { MediaType.valueOf(it.name) }.toSet(),
    wifi = wifi.map { MediaType.valueOf(it.name) }.toSet(),
    roaming = roaming.map { MediaType.valueOf(it.name) }.toSet()
)

private fun com.synapse.social.studioasinc.ui.settings.StorageUsageBreakdown.toDomain() = StorageUsageBreakdown(
    totalSize = totalSize,
    usedSize = usedSize,
    freeSize = freeSize,
    appsAndOtherSize = appsAndOtherSize,
    synapseSize = synapseSize,
    photoSize = photoSize,
    videoSize = videoSize,
    documentSize = documentSize,
    otherSize = otherSize,
    chatSize = chatSize
)

private fun com.synapse.social.studioasinc.ui.settings.LargeFileInfo.toDomain() = LargeFileInfo(
    fileId = fileId,
    fileName = fileName,
    size = size,
    thumbnailUri = thumbnailUri,
    type = MediaType.valueOf(type.name)
)
