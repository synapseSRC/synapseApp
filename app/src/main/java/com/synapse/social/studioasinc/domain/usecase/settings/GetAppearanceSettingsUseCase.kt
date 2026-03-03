package com.synapse.social.studioasinc.domain.usecase.settings

import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.ui.settings.AppearanceSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppearanceSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppearanceSettings> = repository.appearanceSettings
}
