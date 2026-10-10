package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.model.settings.SettingsAction
import com.synapse.social.studioasinc.shared.domain.model.settings.SettingsNode
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SearchSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(query: String): Flow<List<SettingsNode>> {
        if (query.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())

        return combine(
            settingsRepository.notificationPreferences,
            settingsRepository.appearanceSettings
        ) { notifications, appearance ->
            val allNodes = mutableListOf<SettingsNode>()

            // Static categories (simplified)
            allNodes.add(SettingsNode("acc", "Account", "Security, change number", listOf("password", "delete"), SettingsAction.Navigate("settings_account")))
            allNodes.add(SettingsNode("priv", "Privacy", "Block contacts, disappearing messages", listOf("hide", "status"), SettingsAction.Navigate("settings_privacy")))

            // Direct Actions
            allNodes.add(SettingsNode(
                id = "toggle_notifications",
                title = "Global Notifications",
                subtitle = if (notifications.globalEnabled) "On" else "Off",
                keywords = listOf("mute", "sound", "alert"),
                action = SettingsAction.Toggle("notifications_global", notifications.globalEnabled),
                category = "Notifications"
            ))

            allNodes.add(SettingsNode(
                id = "toggle_dark_mode",
                title = "Dark Mode",
                subtitle = "Appearance theme",
                keywords = listOf("theme", "light", "night"),
                action = SettingsAction.Execute("toggle_dark_mode"),
                category = "Appearance"
            ))

            allNodes.add(SettingsNode(
                id = "clear_cache",
                title = "Clear Cache",
                subtitle = "Free up storage space",
                keywords = listOf("storage", "memory", "clean"),
                action = SettingsAction.Execute("clear_cache"),
                category = "Storage"
            ))

            allNodes.filter { node ->
                node.title.contains(query, ignoreCase = true) ||
                node.subtitle?.contains(query, ignoreCase = true) == true ||
                node.keywords.any { it.contains(query, ignoreCase = true) } ||
                node.category?.contains(query, ignoreCase = true) == true
            }
        }
    }
}
