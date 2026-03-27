package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GeneralStore {
    val language: Flow<String>
    val messageSuggestionEnabled: Flow<Boolean>
    val dataSaverEnabled: Flow<Boolean>
    val accountReportsAutoCreate: Flow<Boolean>
    val channelsReportsAutoCreate: Flow<Boolean>
    val hideProfilePicSuggestion: Flow<Boolean>
    val searchHistory: Flow<List<String>>

    suspend fun setLanguage(languageCode: String)
    suspend fun setMessageSuggestionEnabled(enabled: Boolean)
    suspend fun setDataSaverEnabled(enabled: Boolean)
    suspend fun setEnterIsSendEnabled(enabled: Boolean)
    suspend fun setMediaVisibilityEnabled(enabled: Boolean)
    suspend fun setVoiceTranscriptsEnabled(enabled: Boolean)
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setHighPriorityEnabled(enabled: Boolean)
    suspend fun setReactionNotificationsEnabled(enabled: Boolean)
    suspend fun setAccountReportsAutoCreate(enabled: Boolean)
    suspend fun setChannelsReportsAutoCreate(enabled: Boolean)
    suspend fun setHideProfilePicSuggestion(hide: Boolean)
    suspend fun setSearchHistory(history: List<String>)
}

class GeneralStoreImpl(private val dataStore: DataStore<Preferences>) : GeneralStore {
    override val language: Flow<String> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_APP_LANGUAGE] ?: SettingsConstants.DEFAULT_APP_LANGUAGE
    }

    override suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_APP_LANGUAGE] = languageCode
        }
    }

    override val messageSuggestionEnabled: Flow<Boolean> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_MESSAGE_SUGGESTION_ENABLED] ?: SettingsConstants.DEFAULT_MESSAGE_SUGGESTION_ENABLED
    }

    override suspend fun setMessageSuggestionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_MESSAGE_SUGGESTION_ENABLED] = enabled
        }
    }

    override val dataSaverEnabled: Flow<Boolean> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_DATA_SAVER_ENABLED] ?: SettingsConstants.DEFAULT_DATA_SAVER_ENABLED
    }

    override suspend fun setDataSaverEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_DATA_SAVER_ENABLED] = enabled
        }
    }

    override suspend fun setEnterIsSendEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_ENTER_IS_SEND_ENABLED] = enabled
        }
    }

    override suspend fun setMediaVisibilityEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_MEDIA_VISIBILITY_ENABLED] = enabled
        }
    }

    override suspend fun setVoiceTranscriptsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_VOICE_TRANSCRIPTS_ENABLED] = enabled
        }
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_AUTO_BACKUP_ENABLED] = enabled
        }
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_REMINDERS_ENABLED] = enabled
        }
    }

    override suspend fun setHighPriorityEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_HIGH_PRIORITY_ENABLED] = enabled
        }
    }

    override suspend fun setReactionNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_REACTION_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override val accountReportsAutoCreate: Flow<Boolean> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_ACCOUNT_REPORTS_AUTO_CREATE] ?: SettingsConstants.DEFAULT_ACCOUNT_REPORTS_AUTO_CREATE
    }

    override suspend fun setAccountReportsAutoCreate(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_ACCOUNT_REPORTS_AUTO_CREATE] = enabled
        }
    }

    override val channelsReportsAutoCreate: Flow<Boolean> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHANNELS_REPORTS_AUTO_CREATE] ?: SettingsConstants.DEFAULT_CHANNELS_REPORTS_AUTO_CREATE
    }

    override suspend fun setChannelsReportsAutoCreate(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHANNELS_REPORTS_AUTO_CREATE] = enabled
        }
    }

    override val hideProfilePicSuggestion: Flow<Boolean> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_HIDE_PROFILE_PIC_SUGGESTION] ?: false
    }

    override suspend fun setHideProfilePicSuggestion(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_HIDE_PROFILE_PIC_SUGGESTION] = hide
        }
    }

    override val searchHistory: Flow<List<String>> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_SEARCH_HISTORY]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    override suspend fun setSearchHistory(history: List<String>) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_SEARCH_HISTORY] = history.take(5).joinToString(",")
        }
    }
}
