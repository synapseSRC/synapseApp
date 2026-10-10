package com.synapse.social.studioasinc.shared.domain.model.settings

data class SettingsNode(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val keywords: List<String> = emptyList(),
    val action: SettingsAction? = null,
    val category: String? = null
)

sealed class SettingsAction {
    data class Navigate(val destination: String) : SettingsAction()
    data class Toggle(val key: String, val currentValue: Boolean) : SettingsAction()
    data class Execute(val actionId: String) : SettingsAction()
}

data class HeroCard(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val action: SettingsAction,
    val priority: Int = 0,
    val backgroundColor: String? = null
)
