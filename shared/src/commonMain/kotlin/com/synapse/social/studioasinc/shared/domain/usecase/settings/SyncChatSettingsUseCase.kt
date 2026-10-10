package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserPreferencesRepository


class SyncChatSettingsUseCase constructor(
    private val settingsRepository: SettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) {
    suspend fun updateFontScale(scale: Float) {
        settingsRepository.setChatFontScale(scale)
        syncToRemote { it.copy(chatFontScale = scale) }
    }

    suspend fun updateMessageCornerRadius(radius: Int) {
        settingsRepository.setChatMessageCornerRadius(radius)
        syncToRemote { it.copy(chatMessageCornerRadius = radius) }
    }

    suspend fun updateThemePreset(preset: ChatThemePreset) {
        settingsRepository.setChatThemePreset(preset)
        syncToRemote { it.copy(chatThemePreset = preset.name) }
    }

    suspend fun updateWallpaperType(type: WallpaperType) {
        settingsRepository.setChatWallpaperType(type)
        syncToRemote { it.copy(chatWallpaperType = type.name) }
    }

    suspend fun updateWallpaperValue(value: String?) {
        settingsRepository.setChatWallpaperValue(value)
        syncToRemote { it.copy(chatWallpaperValue = value) }
    }

    suspend fun updateWallpaperBlur(blur: Float) {
        settingsRepository.setChatWallpaperBlur(blur)
        syncToRemote { it.copy(chatWallpaperBlur = blur) }
    }

    suspend fun updateListLayout(layout: ChatListLayout) {
        settingsRepository.setChatListLayout(layout)
        syncToRemote { it.copy(chatListLayout = layout.name) }
    }

    suspend fun updateSwipeGesture(gesture: ChatSwipeGesture) {
        settingsRepository.setChatSwipeGesture(gesture)
        syncToRemote { it.copy(chatSwipeGesture = gesture.name) }
    }

    private suspend fun syncToRemote(update: (com.synapse.social.studioasinc.shared.domain.model.UserPreferences) -> com.synapse.social.studioasinc.shared.domain.model.UserPreferences) {
        val userId = authRepository.getCurrentUserId() ?: return
        userPreferencesRepository.updatePreferences(userId, update)
    }

    suspend fun syncFromRemote() {
        val userId = authRepository.getCurrentUserId() ?: return
        userPreferencesRepository.getPreferences(userId).onSuccess { preferences ->
            preferences.chatFontScale?.let { settingsRepository.setChatFontScale(it) }
            preferences.chatMessageCornerRadius?.let { settingsRepository.setChatMessageCornerRadius(it) }
            preferences.chatThemePreset?.let {
                runCatching { ChatThemePreset.valueOf(it) }.onSuccess { preset ->
                    settingsRepository.setChatThemePreset(preset)
                }
            }
            preferences.chatWallpaperType?.let {
                runCatching { WallpaperType.valueOf(it) }.onSuccess { type ->
                    settingsRepository.setChatWallpaperType(type)
                }
            }
            preferences.chatWallpaperValue?.let { settingsRepository.setChatWallpaperValue(it) }
            preferences.chatWallpaperBlur?.let { settingsRepository.setChatWallpaperBlur(it) }
            preferences.chatListLayout?.let {
                runCatching { ChatListLayout.valueOf(it) }.onSuccess { layout ->
                    settingsRepository.setChatListLayout(layout)
                }
            }
            preferences.chatSwipeGesture?.let {
                runCatching { ChatSwipeGesture.valueOf(it) }.onSuccess { gesture ->
                    settingsRepository.setChatSwipeGesture(gesture)
                }
            }
        }
    }
}
