package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

interface AiStore {
    suspend fun getAiPreferredProvider(): String
    suspend fun setAiPreferredProvider(provider: String)
    suspend fun getAiFallbackToPlatform(): Boolean
    suspend fun setAiFallbackToPlatform(enabled: Boolean)
    suspend fun getAiCustomModel(): String?
    suspend fun setAiCustomModel(model: String?)
}

class AiStoreImpl(private val dataStore: DataStore<Preferences>) : AiStore {
    override suspend fun getAiPreferredProvider(): String =
        dataStore.data.first()[SettingsConstants.KEY_AI_PREFERRED_PROVIDER] ?: "platform"

    override suspend fun setAiPreferredProvider(provider: String) {
        dataStore.edit { it[SettingsConstants.KEY_AI_PREFERRED_PROVIDER] = provider }
    }

    override suspend fun getAiFallbackToPlatform(): Boolean =
        dataStore.data.first()[SettingsConstants.KEY_AI_FALLBACK_TO_PLATFORM] ?: true

    override suspend fun setAiFallbackToPlatform(enabled: Boolean) {
        dataStore.edit { it[SettingsConstants.KEY_AI_FALLBACK_TO_PLATFORM] = enabled }
    }

    override suspend fun getAiCustomModel(): String? =
        dataStore.data.first()[SettingsConstants.KEY_AI_CUSTOM_MODEL]

    override suspend fun setAiCustomModel(model: String?) {
        dataStore.edit { prefs ->
            if (model.isNullOrBlank()) prefs.remove(SettingsConstants.KEY_AI_CUSTOM_MODEL)
            else prefs[SettingsConstants.KEY_AI_CUSTOM_MODEL] = model
        }
    }
}
