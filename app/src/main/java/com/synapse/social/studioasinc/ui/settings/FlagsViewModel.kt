package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlagsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val messageSuggestionEnabled: StateFlow<Boolean> = settingsRepository.messageSuggestionEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val chatAvatarDisabled: StateFlow<Boolean> = settingsRepository.chatAvatarDisabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setMessageSuggestionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMessageSuggestionEnabled(enabled)
        }
    }

    fun setChatAvatarDisabled(disabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setChatAvatarDisabled(disabled)
        }
    }
}
