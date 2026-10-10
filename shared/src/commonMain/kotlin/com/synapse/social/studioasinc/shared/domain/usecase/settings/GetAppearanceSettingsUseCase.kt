package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.model.settings.AppearanceSettings
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow


class GetAppearanceSettingsUseCase constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppearanceSettings> = repository.appearanceSettings
}
