package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SoundStore {
    val soundPreferences: Flow<SoundPreferences>
    suspend fun setSoundMasterEnabled(enabled: Boolean)
    suspend fun setMessageTonesEnabled(enabled: Boolean)
    suspend fun setCallTonesEnabled(enabled: Boolean)
}

class SoundStoreImpl(private val dataStore: DataStore<Preferences>) : SoundStore {
    override val soundPreferences: Flow<SoundPreferences> =
        dataStore.safePreferencesFlow().map { preferences ->
            SoundPreferences(
                masterEnabled = preferences[SettingsConstants.KEY_SOUND_EFFECTS_MASTER]
                    ?: SettingsConstants.DEFAULT_SOUND_TONES_ENABLED,
                messageTonesEnabled = preferences[SettingsConstants.KEY_SOUND_MESSAGE_TONES]
                    ?: SettingsConstants.DEFAULT_SOUND_TONES_ENABLED,
                callTonesEnabled = preferences[SettingsConstants.KEY_SOUND_CALL_TONES]
                    ?: SettingsConstants.DEFAULT_SOUND_TONES_ENABLED
            )
        }

    override suspend fun setSoundMasterEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_SOUND_EFFECTS_MASTER] = enabled
        }
    }

    override suspend fun setMessageTonesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_SOUND_MESSAGE_TONES] = enabled
        }
    }

    override suspend fun setCallTonesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_SOUND_CALL_TONES] = enabled
        }
    }
}
