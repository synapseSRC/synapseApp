package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.domain.model.ChatListLayout
import com.synapse.social.studioasinc.domain.model.ChatSwipeGesture
import com.synapse.social.studioasinc.domain.model.ChatThemePreset
import com.synapse.social.studioasinc.domain.model.WallpaperType
import dagger.hilt.android.lifecycle.HiltViewModel
import com.synapse.social.studioasinc.ui.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val chatFontScale: StateFlow<Float> = settingsRepository.chatFontScale
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0f
        )

    val chatMessageCornerRadius: StateFlow<Int> = settingsRepository.chatMessageCornerRadius
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 16
        )

    val chatThemePreset: StateFlow<ChatThemePreset> = settingsRepository.chatThemePreset
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatThemePreset.DEFAULT
        )

    val chatWallpaperType: StateFlow<WallpaperType> = settingsRepository.chatWallpaperType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WallpaperType.DEFAULT
        )


    val chatWallpaperValue: StateFlow<String?> = settingsRepository.chatWallpaperValue
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val chatWallpaperBlur: StateFlow<Float> = settingsRepository.chatWallpaperBlur
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )

    val chatListLayout: StateFlow<ChatListLayout> = settingsRepository.chatListLayout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatListLayout.DOUBLE_LINE
        )

    val chatSwipeGesture: StateFlow<ChatSwipeGesture> = settingsRepository.chatSwipeGesture
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatSwipeGesture.ARCHIVE
        )

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    fun updateChatFontScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setChatFontScale(scale)
        }
    }

    fun updateChatMessageCornerRadius(radius: Int) {
        viewModelScope.launch {
            settingsRepository.setChatMessageCornerRadius(radius)
        }
    }

    fun updateChatThemePreset(preset: ChatThemePreset) {
        viewModelScope.launch {
            settingsRepository.setChatThemePreset(preset)
        }
    }

    fun updateChatWallpaperType(type: WallpaperType) {
        viewModelScope.launch {
            settingsRepository.setChatWallpaperType(type)
        }
    }


    fun updateChatWallpaperValue(value: String?) {
        viewModelScope.launch {
            settingsRepository.setChatWallpaperValue(value)
        }
    }

    fun updateChatWallpaperBlur(blur: Float) {
        viewModelScope.launch {
            settingsRepository.setChatWallpaperBlur(blur)
        }
    }

    fun updateChatListLayout(layout: ChatListLayout) {
        viewModelScope.launch {
            settingsRepository.setChatListLayout(layout)
        }
    }

    fun updateChatSwipeGesture(gesture: ChatSwipeGesture) {
        viewModelScope.launch {
            settingsRepository.setChatSwipeGesture(gesture)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }
}