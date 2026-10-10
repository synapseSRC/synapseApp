package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.model.settings.HeroCard
import com.synapse.social.studioasinc.shared.domain.model.settings.SettingsAction
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetContextualHeroCardsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<List<HeroCard>> {
        return combine(
            settingsRepository.cacheSize,
            settingsRepository.autoBackupEnabled
        ) { cacheSize, backupEnabled ->
            val cards = mutableListOf<HeroCard>()

            // Storage Hero Card
            if (cacheSize > 500 * 1024 * 1024) { // > 500MB
                cards.add(HeroCard(
                    id = "storage_cleanup",
                    title = "Clean up storage",
                    description = "You have ${cacheSize / (1024 * 1024)}MB of cache. Clear it to free up space.",
                    icon = "storage",
                    action = SettingsAction.Execute("clear_cache"),
                    priority = 10
                ))
            }

            // Encryption/Backup Hero Card
            if (!backupEnabled) {
                cards.add(HeroCard(
                    id = "secure_account",
                    title = "Secure your account",
                    description = "Backup your encryption keys to ensure you never lose access to your chats.",
                    icon = "security",
                    action = SettingsAction.Navigate("settings_account"), // Or specific backup route
                    priority = 20,
                    backgroundColor = "#6200EE"
                ))
            }

            cards.sortedByDescending { it.priority }
        }
    }
}
