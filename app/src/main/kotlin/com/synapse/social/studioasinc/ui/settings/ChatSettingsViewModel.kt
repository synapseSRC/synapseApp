package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import com.synapse.social.studioasinc.shared.domain.usecase.settings.ObserveChatSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SyncChatSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val observeChatSettingsUseCase: ObserveChatSettingsUseCase,
    private val syncChatSettingsUseCase: SyncChatSettingsUseCase
) : ViewModel() {

    val chatFontScale: StateFlow<Float> = observeChatSettingsUseCase.chatFontScale
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0f
        )

    val chatMessageCornerRadius: StateFlow<Int> = observeChatSettingsUseCase.chatMessageCornerRadius
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 16
        )

    val chatThemePreset: StateFlow<ChatThemePreset> = observeChatSettingsUseCase.chatThemePreset
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatThemePreset.DEFAULT
        )

    val chatWallpaperType: StateFlow<WallpaperType> = observeChatSettingsUseCase.chatWallpaperType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WallpaperType.DEFAULT
        )


    val chatWallpaperValue: StateFlow<String?> = observeChatSettingsUseCase.chatWallpaperValue
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val chatWallpaperBlur: StateFlow<Float> = observeChatSettingsUseCase.chatWallpaperBlur
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )

    val chatListLayout: StateFlow<ChatListLayout> = observeChatSettingsUseCase.chatListLayout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatListLayout.DOUBLE_LINE
        )

    val chatSwipeGesture: StateFlow<ChatSwipeGesture> = observeChatSettingsUseCase.chatSwipeGesture
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatSwipeGesture.ARCHIVE
        )

    val themeMode: StateFlow<ThemeMode> = observeChatSettingsUseCase.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    fun updateChatFontScale(scale: Float) {
        viewModelScope.launch {
            syncChatSettingsUseCase.updateFontScale(scale)
        }
    }

    fun updateChatMessageCornerRadius(radius: Int) {
        viewModelScope.launch {
            syncChatSettingsUseCase.updateMessageCornerRadius(radius)
        }
    }

    fun updateChatThemePreset(preset: ChatThemePreset) {
        viewModelScope.launch {
            syncChatSettingsUseCase.updateThemePreset(preset)
        }
    }

    fun updateChatWallpaperType(type: WallpaperType) {
        viewModelScope.launch {
            syncChatSettingsUseCase.updateWallpaperType(type)
        }
    }


    fun updateChatWallpaperValue(value: String?) {
        viewModelScope.launch {
            syncChatSettingsUseCase.updateWallpaperValue(value)
        }
    }

    fun updateChatWallpaperBlur(blur: Float) {
        viewModelScope.launch {
            syncChatSettingsUseCase.updateWallpaperBlur(blur)
        }
    }

    fun updateChatListLayout(layout: ChatListLayout) {
        viewModelScope.launch {
            syncChatSettingsUseCase.updateListLayout(layout)
        }
    }

    fun updateChatSwipeGesture(gesture: ChatSwipeGesture) {
        viewModelScope.launch {
            syncChatSettingsUseCase.updateSwipeGesture(gesture)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            observeChatSettingsUseCase.updateThemeMode(mode)
        }
    }
}