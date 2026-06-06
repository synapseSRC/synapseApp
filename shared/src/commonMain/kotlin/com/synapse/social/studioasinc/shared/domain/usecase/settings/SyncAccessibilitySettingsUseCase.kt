package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserPreferencesRepository
import com.synapse.social.studioasinc.shared.domain.model.UserPreferences

class SyncAccessibilitySettingsUseCase constructor(
    private val settingsRepository: SettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) {
    suspend fun updateIncreaseContrast(enabled: Boolean) {
        settingsRepository.setIncreaseContrastEnabled(enabled)
        syncToRemote { it.copy(increaseContrastEnabled = enabled) }
    }

    suspend fun updateHighContrastText(enabled: Boolean) {
        settingsRepository.setHighContrastTextEnabled(enabled)
        syncToRemote { it.copy(highContrastTextEnabled = enabled) }
    }

    suspend fun updateReduceAnimations(enabled: Boolean) {
        settingsRepository.setReduceAnimationsEnabled(enabled)
        syncToRemote { it.copy(reduceAnimationsEnabled = enabled) }
    }

    suspend fun updateAutoplayAnimations(enabled: Boolean) {
        settingsRepository.setAutoplayAnimationsEnabled(enabled)
        syncToRemote { it.copy(autoplayAnimationsEnabled = enabled) }
    }

    private suspend fun syncToRemote(update: (UserPreferences) -> UserPreferences) {
        try {
            val userId = authRepository.getCurrentUserId() ?: return
            userPreferencesRepository.updatePreferences(userId, update)
        } catch (_: Exception) {}
    }

    suspend fun syncFromRemote() {
        val userId = authRepository.getCurrentUserId() ?: return
        userPreferencesRepository.getPreferences(userId).onSuccess { preferences ->
            preferences.increaseContrastEnabled?.let { settingsRepository.setIncreaseContrastEnabled(it) }
            preferences.highContrastTextEnabled?.let { settingsRepository.setHighContrastTextEnabled(it) }
            preferences.reduceAnimationsEnabled?.let { settingsRepository.setReduceAnimationsEnabled(it) }
            preferences.autoplayAnimationsEnabled?.let { settingsRepository.setAutoplayAnimationsEnabled(it) }
        }
    }
}
