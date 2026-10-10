package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository

class ObserveAccessibilitySettingsUseCase constructor(
    private val settingsRepository: SettingsRepository
) {
    val increaseContrastEnabled = settingsRepository.increaseContrastEnabled
    val highContrastTextEnabled = settingsRepository.highContrastTextEnabled
    val reduceAnimationsEnabled = settingsRepository.reduceAnimationsEnabled
    val autoplayAnimationsEnabled = settingsRepository.autoplayAnimationsEnabled
}
