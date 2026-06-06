package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.usecase.settings.ObserveAccessibilitySettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SyncAccessibilitySettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessibilityViewModel @Inject constructor(
    private val observeAccessibilitySettingsUseCase: ObserveAccessibilitySettingsUseCase,
    private val syncAccessibilitySettingsUseCase: SyncAccessibilitySettingsUseCase
) : ViewModel() {

    val increaseContrastEnabled: StateFlow<Boolean> = observeAccessibilitySettingsUseCase.increaseContrastEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val highContrastTextEnabled: StateFlow<Boolean> = observeAccessibilitySettingsUseCase.highContrastTextEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val reduceAnimationsEnabled: StateFlow<Boolean> = observeAccessibilitySettingsUseCase.reduceAnimationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val autoplayAnimationsEnabled: StateFlow<Boolean> = observeAccessibilitySettingsUseCase.autoplayAnimationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun updateIncreaseContrast(enabled: Boolean) {
        viewModelScope.launch {
            syncAccessibilitySettingsUseCase.updateIncreaseContrast(enabled)
        }
    }

    fun updateHighContrastText(enabled: Boolean) {
        viewModelScope.launch {
            syncAccessibilitySettingsUseCase.updateHighContrastText(enabled)
        }
    }

    fun updateReduceAnimations(enabled: Boolean) {
        viewModelScope.launch {
            syncAccessibilitySettingsUseCase.updateReduceAnimations(enabled)
        }
    }

    fun updateAutoplayAnimations(enabled: Boolean) {
        viewModelScope.launch {
            syncAccessibilitySettingsUseCase.updateAutoplayAnimations(enabled)
        }
    }
}
