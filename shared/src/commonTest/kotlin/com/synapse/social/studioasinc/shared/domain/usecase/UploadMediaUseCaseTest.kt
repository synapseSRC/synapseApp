package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.domain.model.MediaType
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadMediaUseCaseTest {

    private class FakeStorageRepository(private val config: StorageConfig) : StorageRepository {
        override fun getStorageConfig(): Flow<StorageConfig> = flowOf(config)
        override suspend fun saveStorageConfig(config: StorageConfig) {}
        override suspend fun updatePhotoProvider(provider: StorageProvider) {}
        override suspend fun updateVideoProvider(provider: StorageProvider) {}
        override suspend fun updateOtherProvider(provider: StorageProvider) {}
        override suspend fun updateImgBBConfig(key: String) {}
        override suspend fun updateCloudinaryConfig(cloudName: String, apiKey: String, apiSecret: String, uploadPreset: String) {}
        override suspend fun updateSupabaseConfig(url: String, key: String, bucket: String) {}
        override suspend fun updateR2Config(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String) {}
        override suspend fun updateCompression(enabled: Boolean) {}
        override suspend fun clearProviderConfig(provider: StorageProvider) {}
        override suspend fun ensureDefault() {}
    }

    private class FakeMediaCompressor : MediaCompressor {
        override suspend fun compress(filePath: String): Result<String> = Result.success(filePath)
        override suspend fun compress(filePath: String, quality: MediaUploadQuality): Result<String> = Result.success(filePath)
    }

    private class FakeMediaUploadRepository(
        private val uploadHandler: (filePath: String, provider: StorageProvider, bucketName: String?) -> Result<String>
    ) : MediaUploadRepository {
        val attemptedProviders = mutableListOf<StorageProvider>()

        override suspend fun upload(
            filePath: String,
            provider: StorageProvider,
            config: StorageConfig,
            bucketName: String?,
            onProgress: (Float) -> Unit
        ): Result<String> {
            attemptedProviders.add(provider)
            return uploadHandler(filePath, provider, bucketName)
        }

        override fun deleteFile(filePath: String) {}
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val mediaUploadQuality: Flow<MediaUploadQuality> = flowOf(MediaUploadQuality.STANDARD)
        override suspend fun setMediaUploadQuality(quality: MediaUploadQuality) {}
        override val language: Flow<String> = flowOf("en")
        override suspend fun setLanguage(language: String) {}
        override suspend fun setThemeMode(mode: com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode) {}
        override suspend fun setDynamicColorEnabled(enabled: Boolean) {}
        override suspend fun setFontScale(scale: com.synapse.social.studioasinc.shared.domain.model.settings.FontScale) {}
        override suspend fun setPostViewStyle(style: com.synapse.social.studioasinc.shared.domain.model.settings.PostViewStyle) {}
        override suspend fun setProfileVisibility(visibility: com.synapse.social.studioasinc.shared.domain.model.settings.ProfileVisibility) {}
        override suspend fun setContentVisibility(visibility: com.synapse.social.studioasinc.shared.domain.model.settings.ContentVisibility) {}
        override suspend fun setGroupPrivacy(privacy: com.synapse.social.studioasinc.shared.domain.model.settings.GroupPrivacy) {}
        override suspend fun setBiometricLockEnabled(enabled: Boolean) {}
        override suspend fun setTwoFactorEnabled(enabled: Boolean) {}
        override suspend fun updateNotificationPreference(category: com.synapse.social.studioasinc.shared.domain.model.settings.NotificationCategory, enabled: Boolean) {}
        override suspend fun setInAppNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setReadReceiptsEnabled(enabled: Boolean) {}
        override suspend fun setChatFontScale(scale: Float) {}
        override suspend fun setChatThemePreset(preset: com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset) {}
        override suspend fun setChatWallpaperType(type: com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType) {}
        override suspend fun setChatWallpaperValue(value: String?) {}
        override suspend fun setChatWallpaperBlur(blur: Float) {}
        override suspend fun setChatMessageCornerRadius(radius: Int) {}
        override suspend fun setMessageSuggestionEnabled(enabled: Boolean) {}
        override suspend fun setChatAvatarDisabled(enabled: Boolean) {}
        override suspend fun setChatMessagePaginationLimit(limit: Int) {}
        override suspend fun setChatListLayout(layout: com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout) {}
        override suspend fun setChatSwipeGesture(gesture: com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture) {}
        override suspend fun setChatFoldersJson(json: String) {}
        override suspend fun setUseLessDataCalls(enabled: Boolean) {}
        override suspend fun setAutoDownloadRule(networkType: String, mediaTypes: Set<com.synapse.social.studioasinc.shared.domain.model.settings.MediaType>) {}
        override suspend fun setKeepMediaDays(days: Int) {}
        override suspend fun setMaxCacheSizeGB(gb: Int) {}
        override suspend fun clearCache(): Long = 0L
        override suspend fun calculateCacheSize(): Long = 0L
        override suspend fun getStorageBreakdown(): com.synapse.social.studioasinc.shared.domain.model.settings.StorageUsageBreakdown = com.synapse.social.studioasinc.shared.domain.model.settings.StorageUsageBreakdown(0L, 0L, 0L, 0L, 0L)
        override suspend fun getLargeFiles(minSizeBytes: Long): List<com.synapse.social.studioasinc.shared.domain.model.settings.LargeFileInfo> = emptyList()
        override suspend fun setDataSaverEnabled(enabled: Boolean) {}
        override suspend fun setEnterIsSendEnabled(enabled: Boolean) {}
        override suspend fun setMediaVisibilityEnabled(enabled: Boolean) {}
        override suspend fun setVoiceTranscriptsEnabled(enabled: Boolean) {}
        override suspend fun setAutoBackupEnabled(enabled: Boolean) {}
        override suspend fun setRemindersEnabled(enabled: Boolean) {}
        override suspend fun setHighPriorityEnabled(enabled: Boolean) {}
        override suspend fun setReactionNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setAppLockEnabled(enabled: Boolean) {}
        override suspend fun setChatLockEnabled(enabled: Boolean) {}
        override suspend fun clearUserSettings() {}
        override suspend fun clearAllSettings() {}
        override suspend fun restoreDefaults() {}
        override suspend fun checkForUpdates(): Result<Nothing?> = Result.success(null)
        override suspend fun setHideProfilePicSuggestion(hide: Boolean) {}
        override suspend fun setIncreaseContrastEnabled(enabled: Boolean) {}
        override suspend fun setHighContrastTextEnabled(enabled: Boolean) {}
        override suspend fun setReduceAnimationsEnabled(enabled: Boolean) {}
        override suspend fun setAutoplayAnimationsEnabled(enabled: Boolean) {}

        override val themeMode: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode.SYSTEM)
        override val dynamicColorEnabled: Flow<Boolean> = flowOf(true)
        override val fontScale: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.FontScale> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.FontScale.MEDIUM)
        override val appearanceSettings: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.AppearanceSettings> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.AppearanceSettings())
        override val profileVisibility: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ProfileVisibility> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.ProfileVisibility.PUBLIC)
        override val contentVisibility: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ContentVisibility> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.ContentVisibility.EVERYONE)
        override val biometricLockEnabled: Flow<Boolean> = flowOf(false)
        override val twoFactorEnabled: Flow<Boolean> = flowOf(false)
        override val privacySettings: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.PrivacySettings> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.PrivacySettings())
        override val notificationPreferences: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.NotificationPreferences> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.NotificationPreferences())
        override val chatFontScale: Flow<Float> = flowOf(1.0f)
        override val chatThemePreset: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset.DEFAULT)
        override val chatWallpaperType: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType.DEFAULT)
        override val chatWallpaperValue: Flow<String?> = flowOf(null)
        override val chatWallpaperBlur: Flow<Float> = flowOf(0f)
        override val chatMessageCornerRadius: Flow<Int> = flowOf(16)
        override val messageSuggestionEnabled: Flow<Boolean> = flowOf(true)
        override val chatAvatarDisabled: Flow<Boolean> = flowOf(false)
        override val chatMessagePaginationLimit: Flow<Int> = flowOf(20)
        override val chatListLayout: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout.DOUBLE_LINE)
        override val chatSwipeGesture: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture.ARCHIVE)
        override val chatFoldersJson: Flow<String?> = flowOf(null)
        override val useLessDataCalls: Flow<Boolean> = flowOf(false)
        override val autoDownloadRules: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.AutoDownloadRules> = flowOf(com.synapse.social.studioasinc.shared.domain.model.settings.AutoDownloadRules())
        override val cacheSize: Flow<Long> = flowOf(0L)
        override val keepMediaDays: Flow<Int> = flowOf(30)
        override val maxCacheSizeGB: Flow<Int> = flowOf(5)
        override val dataSaverEnabled: Flow<Boolean> = flowOf(false)
        override val autoBackupEnabled: Flow<Boolean> = flowOf(false)
        override val hideProfilePicSuggestion: Flow<Boolean> = flowOf(false)
        override val increaseContrastEnabled: Flow<Boolean> = flowOf(false)
        override val highContrastTextEnabled: Flow<Boolean> = flowOf(false)
        override val reduceAnimationsEnabled: Flow<Boolean> = flowOf(false)
        override val autoplayAnimationsEnabled: Flow<Boolean> = flowOf(true)
    }

    @Test
    fun testUploadSuccessWithConfiguredProvider() = runTest {
        val config = StorageConfig(
            photoProvider = StorageProvider.SUPABASE
        )
        val storageRepo = FakeStorageRepository(config)
        val settingsRepo = FakeSettingsRepository()
        val mediaCompressor = FakeMediaCompressor()
        val uploadRepo = FakeMediaUploadRepository { _, provider, _ ->
            if (provider == StorageProvider.SUPABASE) {
                Result.success("https://example.com/supabase_image.jpg")
            } else {
                Result.failure(Exception("Not supported"))
            }
        }

        val useCase = UploadMediaUseCase(storageRepo, settingsRepo, uploadRepo, mediaCompressor)
        val result = useCase(filePath = "test.jpg", mediaType = MediaType.PHOTO, onProgress = {})

        assertTrue(result.isSuccess)
        assertEquals("https://example.com/supabase_image.jpg", result.getOrNull())
        assertEquals(listOf(StorageProvider.SUPABASE), uploadRepo.attemptedProviders)
    }

    @Test
    fun testUploadFailsWhenProviderNotConfigured() = runTest {
        val config = StorageConfig(
            photoProvider = StorageProvider.IMGBB,
            imgBBKey = "" // Not configured
        )
        val storageRepo = FakeStorageRepository(config)
        val settingsRepo = FakeSettingsRepository()
        val mediaCompressor = FakeMediaCompressor()
        val uploadRepo = FakeMediaUploadRepository { _, _, _ -> Result.failure(Exception("Should not be called")) }

        val useCase = UploadMediaUseCase(storageRepo, settingsRepo, uploadRepo, mediaCompressor)
        val result = useCase(filePath = "test.jpg", mediaType = MediaType.PHOTO, onProgress = {})

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(uploadRepo.attemptedProviders.isEmpty())
    }
}
