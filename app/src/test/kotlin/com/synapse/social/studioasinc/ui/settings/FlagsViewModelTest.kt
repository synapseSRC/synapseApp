package com.synapse.social.studioasinc.ui.settings

import com.synapse.social.studioasinc.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlagsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSettingsRepository
    private lateinit var viewModel: FlagsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSettingsRepository()
        viewModel = FlagsViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun navigationPlacement_defaultIsTop() = runTest {
        assertEquals(NavigationPlacement.TOP, viewModel.navigationPlacement.value)
    }

    @Test
    fun setNavigationPlacement_updatesState() = runTest {
        backgroundScope.launch { viewModel.navigationPlacement.collect {} }
        viewModel.setNavigationPlacement(NavigationPlacement.BOTTOM)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(NavigationPlacement.BOTTOM, viewModel.navigationPlacement.value)
    }
}

class FakeSettingsRepository : SettingsRepository {
    private val _navigationPlacement = MutableStateFlow(NavigationPlacement.TOP)
    override val navigationPlacement: Flow<NavigationPlacement> = _navigationPlacement

    override suspend fun setNavigationPlacement(placement: NavigationPlacement) {
        _navigationPlacement.value = placement
    }

    private val _messageSuggestion = MutableStateFlow(false)
    override val messageSuggestionEnabled: Flow<Boolean> = _messageSuggestion
    override suspend fun setMessageSuggestionEnabled(enabled: Boolean) { _messageSuggestion.value = enabled }

    private val _chatAvatar = MutableStateFlow(false)
    override val chatAvatarDisabled: Flow<Boolean> = _chatAvatar
    override suspend fun setChatAvatarDisabled(enabled: Boolean) { _chatAvatar.value = enabled }

    private val _chatPagination = MutableStateFlow(50)
    override val chatMessagePaginationLimit: Flow<Int> = _chatPagination
    override suspend fun setChatMessagePaginationLimit(limit: Int) { _chatPagination.value = limit }

    // Dummy overrides for unused SettingsRepository functions
    override val themeMode: Flow<ThemeMode> get() = TODO()
    override val dynamicColorEnabled: Flow<Boolean> get() = TODO()
    override val fontScale: Flow<FontScale> get() = TODO()
    override val appearanceSettings: Flow<AppearanceSettings> get() = TODO()
    override val selectedFontId: Flow<String> get() = TODO()
    override suspend fun setThemeMode(mode: ThemeMode) {}
    override suspend fun setDynamicColorEnabled(enabled: Boolean) {}
    override suspend fun setFontScale(scale: FontScale) {}
    override suspend fun setPostViewStyle(style: PostViewStyle) {}
    override suspend fun setSelectedFontId(id: String) {}
    override val language: Flow<String> get() = TODO()
    override suspend fun setLanguage(languageCode: String) {}
    override val profileVisibility: Flow<ProfileVisibility> get() = TODO()
    override val contentVisibility: Flow<ContentVisibility> get() = TODO()
    override val lastSeenVisibility: Flow<LastSeenVisibility> get() = TODO()
    override val profilePhotoVisibility: Flow<ProfilePhotoVisibility> get() = TODO()
    override val aboutVisibility: Flow<AboutVisibility> get() = TODO()
    override val statusVisibility: Flow<StatusVisibility> get() = TODO()
    override val lastSeenExcludedUserIds: Flow<Set<String>> get() = TODO()
    override val profilePhotoExcludedUserIds: Flow<Set<String>> get() = TODO()
    override val aboutExcludedUserIds: Flow<Set<String>> get() = TODO()
    override val statusExcludedUserIds: Flow<Set<String>> get() = TODO()
    override val groupExcludedUserIds: Flow<Set<String>> get() = TODO()
    override val biometricLockEnabled: Flow<Boolean> get() = TODO()
    override val twoFactorEnabled: Flow<Boolean> get() = TODO()
    override val privacySettings: Flow<PrivacySettings> get() = TODO()
    override suspend fun setProfileVisibility(visibility: ProfileVisibility) {}
    override suspend fun setContentVisibility(visibility: ContentVisibility) {}
    override suspend fun setLastSeenVisibility(visibility: LastSeenVisibility) {}
    override suspend fun setProfilePhotoVisibility(visibility: ProfilePhotoVisibility) {}
    override suspend fun setAboutVisibility(visibility: AboutVisibility) {}
    override suspend fun setStatusVisibility(visibility: StatusVisibility) {}
    override suspend fun setLastSeenExcludedUserIds(user: Set<String>) {}
    override suspend fun setProfilePhotoExcludedUserIds(user: Set<String>) {}
    override suspend fun setAboutExcludedUserIds(user: Set<String>) {}
    override suspend fun setStatusExcludedUserIds(user: Set<String>) {}
    override suspend fun setGroupExcludedUserIds(user: Set<String>) {}
    override suspend fun setGroupPrivacy(privacy: GroupPrivacy) {}
    override suspend fun setBiometricLockEnabled(enabled: Boolean) {}
    override suspend fun setTwoFactorEnabled(enabled: Boolean) {}
    override val notificationPreferences: Flow<NotificationPreferences> get() = TODO()
    override suspend fun updateNotificationPreference(category: NotificationCategory, enabled: Boolean) {}
    override suspend fun setInAppNotificationsEnabled(enabled: Boolean) {}
    override suspend fun setReadReceiptsEnabled(enabled: Boolean) {}
    override val chatFontScale: Flow<Float> get() = TODO()
    override suspend fun setChatFontScale(scale: Float) {}
    override val chatThemePreset: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset> get() = TODO()
    override suspend fun setChatThemePreset(preset: com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset) {}
    override val chatWallpaperType: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType> get() = TODO()
    override suspend fun setChatWallpaperType(type: com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType) {}
    override val chatWallpaperValue: Flow<String?> get() = TODO()
    override suspend fun setChatWallpaperValue(value: String?) {}
    override val chatWallpaperBlur: Flow<Float> get() = TODO()
    override suspend fun setChatWallpaperBlur(blur: Float) {}
    override val chatMessageCornerRadius: Flow<Int> get() = TODO()
    override suspend fun setChatMessageCornerRadius(radius: Int) {}
    override val chatListLayout: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout> get() = TODO()
    override suspend fun setChatListLayout(layout: com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout) {}
    override val chatSwipeGesture: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture> get() = TODO()
    override suspend fun setChatSwipeGesture(gesture: com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture) {}
    override val chatMaxMessageChunkSize: Flow<Int> get() = TODO()
    override suspend fun setChatMaxMessageChunkSize(size: Int) {}
    override val chatFoldersJson: Flow<String?> get() = TODO()
    override suspend fun setChatFoldersJson(json: String) {}
    override val mediaUploadQuality: Flow<MediaUploadQuality> get() = TODO()
    override suspend fun setMediaUploadQuality(quality: MediaUploadQuality) {}
    override val useLessDataCalls: Flow<Boolean> get() = TODO()
    override suspend fun setUseLessDataCalls(enabled: Boolean) {}
    override val autoDownloadRules: Flow<AutoDownloadRules> get() = TODO()
    override suspend fun setAutoDownloadRule(networkType: String, mediaTypes: Set<MediaType>) {}
    override val cacheSize: Flow<Long> get() = TODO()
    override val keepMediaDays: Flow<Int> get() = TODO()
    override val maxCacheSizeGB: Flow<Int> get() = TODO()
    override suspend fun setKeepMediaDays(days: Int) {}
    override suspend fun setMaxCacheSizeGB(gb: Int) {}
    override suspend fun clearCache(): Long = 0L
    override suspend fun calculateCacheSize(): Long = 0L
    override suspend fun getStorageBreakdown(): StorageUsageBreakdown = TODO()
    override suspend fun getLargeFiles(minSizeBytes: Long): List<LargeFileInfo> = emptyList()
    override val dataSaverEnabled: Flow<Boolean> get() = TODO()
    override suspend fun setDataSaverEnabled(enabled: Boolean) {}
    override suspend fun setEnterIsSendEnabled(enabled: Boolean) {}
    override suspend fun setMediaVisibilityEnabled(enabled: Boolean) {}
    override suspend fun setVoiceTranscriptsEnabled(enabled: Boolean) {}
    override val autoBackupEnabled: Flow<Boolean> get() = TODO()
    override suspend fun setAutoBackupEnabled(enabled: Boolean) {}
    override suspend fun setRemindersEnabled(enabled: Boolean) {}
    override suspend fun setHighPriorityEnabled(enabled: Boolean) {}
    override suspend fun setReactionNotificationsEnabled(enabled: Boolean) {}
    override suspend fun setAppLockEnabled(enabled: Boolean) {}
    override suspend fun setChatLockEnabled(enabled: Boolean) {}
    override suspend fun clearUserSettings() {}
    override suspend fun clearAllSettings() {}
    override suspend fun restoreDefaults() {}
    override suspend fun checkForUpdates(): Result<com.synapse.social.studioasinc.shared.domain.model.update.AppUpdateInfo?> = Result.success(null)
    override val hideProfilePicSuggestion: Flow<Boolean> get() = TODO()
    override suspend fun setHideProfilePicSuggestion(hide: Boolean) {}
    override val increaseContrastEnabled: Flow<Boolean> get() = TODO()
    override suspend fun setIncreaseContrastEnabled(enabled: Boolean) {}
    override val highContrastTextEnabled: Flow<Boolean> get() = TODO()
    override suspend fun setHighContrastTextEnabled(enabled: Boolean) {}
    override val reduceAnimationsEnabled: Flow<Boolean> get() = TODO()
    override suspend fun setReduceAnimationsEnabled(enabled: Boolean) {}
    override val autoplayAnimationsEnabled: Flow<Boolean> get() = TODO()
    override suspend fun setAutoplayAnimationsEnabled(enabled: Boolean) {}
}
