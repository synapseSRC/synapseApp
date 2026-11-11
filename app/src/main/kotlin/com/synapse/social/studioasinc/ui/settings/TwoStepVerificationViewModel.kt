package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * View model for the Two-Step Verification placeholder screen.
 *
 * Reads the current two-factor authentication state from the settings repository and exposes
 * it to the UI. A real setup flow can be wired into [setupTwoFactor] once the underlying
 * two-factor setup implementation is available.
 */
class TwoStepVerificationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepositoryImpl.getInstance(application)

    private val _twoFactorEnabled = MutableStateFlow(false)
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTwoFactorState()
    }

    private fun loadTwoFactorState() {
        viewModelScope.launch {
            try {
                settingsRepository.twoFactorEnabled.collect { enabled ->
                    _twoFactorEnabled.value = enabled
                }
            } catch (e: Exception) {
                Log.e("TwoStepVerificationViewModel", "Failed to load two-factor state", e)
                _error.value = "Failed to load two-factor authentication state"
            }
        }
    }

    fun setupTwoFactor() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setTwoFactorEnabled(true)
                Log.d("TwoStepVerificationViewModel", "Two-factor authentication enabled")
            } catch (e: Exception) {
                Log.e("TwoStepVerificationViewModel", "Failed to enable two-factor authentication", e)
                _error.value = "Failed to enable two-factor authentication"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
