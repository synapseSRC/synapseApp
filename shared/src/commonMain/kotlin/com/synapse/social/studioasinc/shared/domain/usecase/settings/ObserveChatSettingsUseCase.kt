package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository


class ObserveChatSettingsUseCase constructor(
    val settingsRepository: SettingsRepository
) {
    val chatFontScale = settingsRepository.chatFontScale
    val chatMessageCornerRadius = settingsRepository.chatMessageCornerRadius
    val chatThemePreset = settingsRepository.chatThemePreset
    val chatWallpaperType = settingsRepository.chatWallpaperType
    val chatWallpaperValue = settingsRepository.chatWallpaperValue
    val chatWallpaperBlur = settingsRepository.chatWallpaperBlur
    val chatListLayout = settingsRepository.chatListLayout
    val chatSwipeGesture = settingsRepository.chatSwipeGesture
    val themeMode = settingsRepository.themeMode

    suspend fun updateThemeMode(mode: com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}
