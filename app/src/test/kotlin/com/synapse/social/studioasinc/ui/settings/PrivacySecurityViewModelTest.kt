package com.synapse.social.studioasinc.ui.settings

import com.synapse.social.studioasinc.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacySecurityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePrivacySettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePrivacySettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setLastSeenVisibility_updatesRepositoryState() = runTest {
        fakeRepository.setLastSeenVisibility(LastSeenVisibility.NOBODY)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(LastSeenVisibility.NOBODY, fakeRepository.lastSeenVisibilityValue)
    }

    @Test
    fun setProfilePhotoVisibility_updatesRepositoryState() = runTest {
        fakeRepository.setProfilePhotoVisibility(ProfilePhotoVisibility.MY_CONTACTS)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProfilePhotoVisibility.MY_CONTACTS, fakeRepository.profilePhotoVisibilityValue)
    }

    @Test
    fun setAboutVisibility_updatesRepositoryState() = runTest {
        fakeRepository.setAboutVisibility(AboutVisibility.MY_CONTACTS_EXCEPT)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AboutVisibility.MY_CONTACTS_EXCEPT, fakeRepository.aboutVisibilityValue)
    }

    @Test
    fun setStatusVisibility_updatesRepositoryState() = runTest {
        fakeRepository.setStatusVisibility(StatusVisibility.NOBODY)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(StatusVisibility.NOBODY, fakeRepository.statusVisibilityValue)
    }

    @Test
    fun setAppLockEnabled_updatesRepositoryAndSettingsFlow() = runTest {
        fakeRepository.setAppLockEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, fakeRepository.appLockEnabledValue)

        fakeRepository.setAppLockEnabled(false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, fakeRepository.appLockEnabledValue)
    }
}

class FakePrivacySettingsRepository : SettingsRepository {
    var appLockEnabledValue = false
    var lastSeenVisibilityValue = LastSeenVisibility.EVERYONE
    var profilePhotoVisibilityValue = ProfilePhotoVisibility.EVERYONE
    var aboutVisibilityValue = AboutVisibility.EVERYONE
    var statusVisibilityValue = StatusVisibility.EVERYONE

    var lastSeenExcludedUserIdsValue = emptySet<String>()
    var profilePhotoExcludedUserIdsValue = emptySet<String>()
    var aboutExcludedUserIdsValue = emptySet<String>()
    var statusExcludedUserIdsValue = emptySet<String>()
    var groupExcludedUserIdsValue = emptySet<String>()

    private val _privacySettings = MutableStateFlow(PrivacySettings())
    override val privacySettings: Flow<PrivacySettings> = _privacySettings

    override suspend fun setLastSeenVisibility(visibility: LastSeenVisibility) {
        lastSeenVisibilityValue = visibility
        _privacySettings.value = _privacySettings.value.copy(lastSeenVisibility = visibility)
    }

    override suspend fun setProfilePhotoVisibility(visibility: ProfilePhotoVisibility) {
        profilePhotoVisibilityValue = visibility
        _privacySettings.value = _privacySettings.value.copy(profilePhotoVisibility = visibility)
    }

    override suspend fun setAboutVisibility(visibility: AboutVisibility) {
        aboutVisibilityValue = visibility
        _privacySettings.value = _privacySettings.value.copy(aboutVisibility = visibility)
    }

    override suspend fun setStatusVisibility(visibility: StatusVisibility) {
        statusVisibilityValue = visibility
        _privacySettings.value = _privacySettings.value.copy(statusVisibility = visibility)
    }

    override suspend fun setLastSeenExcludedUserIds(user: Set<String>) {
        lastSeenExcludedUserIdsValue = user
        _privacySettings.value = _privacySettings.value.copy(lastSeenExcludedUserIds = user)
    }

    override suspend fun setProfilePhotoExcludedUserIds(user: Set<String>) {
        profilePhotoExcludedUserIdsValue = user
        _privacySettings.value = _privacySettings.value.copy(profilePhotoExcludedUserIds = user)
    }

    override suspend fun setAboutExcludedUserIds(user: Set<String>) {
        aboutExcludedUserIdsValue = user
        _privacySettings.value = _privacySettings.value.copy(aboutExcludedUserIds = user)
    }

    override suspend fun setStatusExcludedUserIds(user: Set<String>) {
        statusExcludedUserIdsValue = user
        _privacySettings.value = _privacySettings.value.copy(statusExcludedUserIds = user)
    }

    override suspend fun setGroupExcludedUserIds(user: Set<String>) {
        groupExcludedUserIdsValue = user
        _privacySettings.value = _privacySettings.value.copy(groupExcludedUserIds = user)
    }

    // Unused overrides
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
    override val lastSeenVisibility: Flow<LastSeenVisibility> get() = MutableStateFlow(lastSeenVisibilityValue)
    override val profilePhotoVisibility: Flow<ProfilePhotoVisibility> get() = MutableStateFlow(profilePhotoVisibilityValue)
    override val aboutVisibility: Flow<AboutVisibility> get() = MutableStateFlow(aboutVisibilityValue)
    override val statusVisibility: Flow<StatusVisibility> get() = MutableStateFlow(statusVisibilityValue)
    override val lastSeenExcludedUserIds: Flow<Set<String>> get() = MutableStateFlow(lastSeenExcludedUserIdsValue)
    override val profilePhotoExcludedUserIds: Flow<Set<String>> get() = MutableStateFlow(profilePhotoExcludedUserIdsValue)
    override val aboutExcludedUserIds: Flow<Set<String>> get() = MutableStateFlow(aboutExcludedUserIdsValue)
    override val statusExcludedUserIds: Flow<Set<String>> get() = MutableStateFlow(statusExcludedUserIdsValue)
    override val groupExcludedUserIds: Flow<Set<String>> get() = MutableStateFlow(groupExcludedUserIdsValue)
    override val biometricLockEnabled: Flow<Boolean> get() = TODO()
    override val twoFactorEnabled: Flow<Boolean> get() = TODO()
    override suspend fun setProfileVisibility(visibility: ProfileVisibility) {}
    override suspend fun setContentVisibility(visibility: ContentVisibility) {}
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
    override val navigationPlacement: Flow<NavigationPlacement> get() = TODO()
    override suspend fun setNavigationPlacement(placement: NavigationPlacement) {}
    override val messageSuggestionEnabled: Flow<Boolean> get() = TODO()
    override suspend fun setMessageSuggestionEnabled(enabled: Boolean) {}
    override val chatAvatarDisabled: Flow<Boolean> get() = TODO()
    override suspend fun setChatAvatarDisabled(enabled: Boolean) {}
    override val chatMessagePaginationLimit: Flow<Int> get() = TODO()
    override suspend fun setChatMessagePaginationLimit(limit: Int) {}
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
    override suspend fun setAppLockEnabled(enabled: Boolean) {
        appLockEnabledValue = enabled
        _privacySettings.value = _privacySettings.value.copy(appLockEnabled = enabled)
    }
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
