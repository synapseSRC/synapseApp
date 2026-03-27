package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.synapse.social.studioasinc.ui.settings.ContentVisibility
import com.synapse.social.studioasinc.ui.settings.GroupPrivacy
import com.synapse.social.studioasinc.ui.settings.PrivacySettings
import com.synapse.social.studioasinc.ui.settings.ProfileVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PrivacyStore {
    val profileVisibility: Flow<ProfileVisibility>
    val contentVisibility: Flow<ContentVisibility>
    val biometricLockEnabled: Flow<Boolean>
    val twoFactorEnabled: Flow<Boolean>
    val privacySettings: Flow<PrivacySettings>

    suspend fun setProfileVisibility(visibility: ProfileVisibility)
    suspend fun setContentVisibility(visibility: ContentVisibility)
    suspend fun setGroupPrivacy(privacy: GroupPrivacy)
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    suspend fun setTwoFactorEnabled(enabled: Boolean)
    suspend fun setReadReceiptsEnabled(enabled: Boolean)
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setChatLockEnabled(enabled: Boolean)
}

class PrivacyStoreImpl(private val dataStore: DataStore<Preferences>) : PrivacyStore {
    override val profileVisibility: Flow<ProfileVisibility> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_PROFILE_VISIBILITY]?.let { value ->
            runCatching { ProfileVisibility.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_PROFILE_VISIBILITY)
        } ?: SettingsConstants.DEFAULT_PROFILE_VISIBILITY
    }

    override val contentVisibility: Flow<ContentVisibility> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CONTENT_VISIBILITY]?.let { value ->
            runCatching { ContentVisibility.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_CONTENT_VISIBILITY)
        } ?: SettingsConstants.DEFAULT_CONTENT_VISIBILITY
    }

    override val biometricLockEnabled: Flow<Boolean> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_BIOMETRIC_LOCK_ENABLED] ?: SettingsConstants.DEFAULT_BIOMETRIC_LOCK_ENABLED
    }

    override val twoFactorEnabled: Flow<Boolean> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_TWO_FACTOR_ENABLED] ?: SettingsConstants.DEFAULT_TWO_FACTOR_ENABLED
    }

    override val privacySettings: Flow<PrivacySettings> = dataStore.safePreferencesFlow().map { preferences ->
        PrivacySettings(
            profileVisibility = preferences[SettingsConstants.KEY_PROFILE_VISIBILITY]?.let { value ->
                runCatching { ProfileVisibility.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_PROFILE_VISIBILITY)
            } ?: SettingsConstants.DEFAULT_PROFILE_VISIBILITY,
            twoFactorEnabled = preferences[SettingsConstants.KEY_TWO_FACTOR_ENABLED] ?: SettingsConstants.DEFAULT_TWO_FACTOR_ENABLED,
            biometricLockEnabled = preferences[SettingsConstants.KEY_BIOMETRIC_LOCK_ENABLED] ?: SettingsConstants.DEFAULT_BIOMETRIC_LOCK_ENABLED,
            contentVisibility = preferences[SettingsConstants.KEY_CONTENT_VISIBILITY]?.let { value ->
                runCatching { ContentVisibility.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_CONTENT_VISIBILITY)
            } ?: SettingsConstants.DEFAULT_CONTENT_VISIBILITY,
            groupPrivacy = preferences[SettingsConstants.KEY_GROUP_PRIVACY]?.let { value ->
                runCatching { GroupPrivacy.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_GROUP_PRIVACY)
            } ?: SettingsConstants.DEFAULT_GROUP_PRIVACY,
            readReceiptsEnabled = preferences[SettingsConstants.KEY_READ_RECEIPTS_ENABLED] ?: SettingsConstants.DEFAULT_READ_RECEIPTS_ENABLED,
            appLockEnabled = preferences[SettingsConstants.KEY_APP_LOCK_ENABLED] ?: SettingsConstants.DEFAULT_APP_LOCK_ENABLED,
            chatLockEnabled = preferences[SettingsConstants.KEY_CHAT_LOCK_ENABLED] ?: SettingsConstants.DEFAULT_CHAT_LOCK_ENABLED
        )
    }

    override suspend fun setProfileVisibility(visibility: ProfileVisibility) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_PROFILE_VISIBILITY] = visibility.name
        }
    }

    override suspend fun setContentVisibility(visibility: ContentVisibility) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CONTENT_VISIBILITY] = visibility.name
        }
    }

    override suspend fun setGroupPrivacy(privacy: GroupPrivacy) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_GROUP_PRIVACY] = privacy.name
        }
    }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    override suspend fun setTwoFactorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_TWO_FACTOR_ENABLED] = enabled
        }
    }

    override suspend fun setReadReceiptsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_READ_RECEIPTS_ENABLED] = enabled
        }
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    override suspend fun setChatLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_LOCK_ENABLED] = enabled
        }
    }
}
