package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    val chatWallpaperType: Flow<WallpaperType> = settingsRepository.chatWallpaperType
    val chatWallpaperValue: Flow<String?> = settingsRepository.chatWallpaperValue
    val chatWallpaperBlur: Flow<Float> = settingsRepository.chatWallpaperBlur
    val chatFontScale: Flow<Float> = settingsRepository.chatFontScale
    val chatThemePreset: Flow<ChatThemePreset> = settingsRepository.chatThemePreset
    val chatMessageCornerRadius: Flow<Int> = settingsRepository.chatMessageCornerRadius
    val chatMaxMessageChunkSize: Flow<Int> = settingsRepository.chatMaxMessageChunkSize
    val messageSuggestionEnabled: Flow<Boolean> = settingsRepository.messageSuggestionEnabled
    val chatAvatarDisabled: Flow<Boolean> = settingsRepository.chatAvatarDisabled
}
