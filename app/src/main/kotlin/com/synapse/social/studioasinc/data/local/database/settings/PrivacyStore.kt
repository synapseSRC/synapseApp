package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.synapse.social.studioasinc.ui.settings.AboutVisibility
import com.synapse.social.studioasinc.ui.settings.ContentVisibility
import com.synapse.social.studioasinc.ui.settings.GroupPrivacy
import com.synapse.social.studioasinc.ui.settings.LastSeenVisibility
import com.synapse.social.studioasinc.ui.settings.PrivacySettings
import com.synapse.social.studioasinc.ui.settings.ProfilePhotoVisibility
import com.synapse.social.studioasinc.ui.settings.ProfileVisibility
import com.synapse.social.studioasinc.ui.settings.StatusVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PrivacyStore {
    val profileVisibility: Flow<ProfileVisibility>
    val contentVisibility: Flow<ContentVisibility>
    val lastSeenVisibility: Flow<LastSeenVisibility>
    val profilePhotoVisibility: Flow<ProfilePhotoVisibility>
    val aboutVisibility: Flow<AboutVisibility>
    val statusVisibility: Flow<StatusVisibility>
    val lastSeenExcludedUserIds: Flow<Set<String>>
    val profilePhotoExcludedUserIds: Flow<Set<String>>
    val aboutExcludedUserIds: Flow<Set<String>>
    val statusExcludedUserIds: Flow<Set<String>>
    val groupExcludedUserIds: Flow<Set<String>>
    val biometricLockEnabled: Flow<Boolean>
    val twoFactorEnabled: Flow<Boolean>
    val privacySettings: Flow<PrivacySettings>

    suspend fun setProfileVisibility(visibility: ProfileVisibility)
    suspend fun setContentVisibility(visibility: ContentVisibility)
    suspend fun setLastSeenVisibility(visibility: LastSeenVisibility)
    suspend fun setProfilePhotoVisibility(visibility: ProfilePhotoVisibility)
    suspend fun setAboutVisibility(visibility: AboutVisibility)
    suspend fun setStatusVisibility(visibility: StatusVisibility)
    suspend fun setLastSeenExcludedUserIds(user: Set<String>)
    suspend fun setProfilePhotoExcludedUserIds(user: Set<String>)
    suspend fun setAboutExcludedUserIds(user: Set<String>)
    suspend fun setStatusExcludedUserIds(user: Set<String>)
    suspend fun setGroupExcludedUserIds(user: Set<String>)
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

    override val lastSeenVisibility: Flow<LastSeenVisibility> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_LAST_SEEN_VISIBILITY]?.let { value ->
            runCatching { LastSeenVisibility.valueOf(value) }.getOrNull()
        } ?: preferences[SettingsConstants.KEY_PROFILE_VISIBILITY]?.let { value ->
            when (runCatching { ProfileVisibility.valueOf(value) }.getOrNull()) {
                ProfileVisibility.PUBLIC -> LastSeenVisibility.EVERYONE
                ProfileVisibility.FOLLOWERS_ONLY -> LastSeenVisibility.MY_CONTACTS
                ProfileVisibility.PRIVATE -> LastSeenVisibility.NOBODY
                null -> SettingsConstants.DEFAULT_LAST_SEEN_VISIBILITY
            }
        } ?: SettingsConstants.DEFAULT_LAST_SEEN_VISIBILITY
    }

    override val profilePhotoVisibility: Flow<ProfilePhotoVisibility> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_PROFILE_PHOTO_VISIBILITY]?.let { value ->
            runCatching { ProfilePhotoVisibility.valueOf(value) }.getOrNull()
        } ?: preferences[SettingsConstants.KEY_PROFILE_VISIBILITY]?.let { value ->
            when (runCatching { ProfileVisibility.valueOf(value) }.getOrNull()) {
                ProfileVisibility.PUBLIC -> ProfilePhotoVisibility.EVERYONE
                ProfileVisibility.FOLLOWERS_ONLY -> ProfilePhotoVisibility.MY_CONTACTS
                ProfileVisibility.PRIVATE -> ProfilePhotoVisibility.NOBODY
                null -> SettingsConstants.DEFAULT_PROFILE_PHOTO_VISIBILITY
            }
        } ?: SettingsConstants.DEFAULT_PROFILE_PHOTO_VISIBILITY
    }

    override val aboutVisibility: Flow<AboutVisibility> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_ABOUT_VISIBILITY]?.let { value ->
            runCatching { AboutVisibility.valueOf(value) }.getOrNull()
        } ?: preferences[SettingsConstants.KEY_PROFILE_VISIBILITY]?.let { value ->
            when (runCatching { ProfileVisibility.valueOf(value) }.getOrNull()) {
                ProfileVisibility.PUBLIC -> AboutVisibility.EVERYONE
                ProfileVisibility.FOLLOWERS_ONLY -> AboutVisibility.MY_CONTACTS
                ProfileVisibility.PRIVATE -> AboutVisibility.NOBODY
                null -> SettingsConstants.DEFAULT_ABOUT_VISIBILITY
            }
        } ?: SettingsConstants.DEFAULT_ABOUT_VISIBILITY
    }

    override val statusVisibility: Flow<StatusVisibility> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_STATUS_VISIBILITY]?.let { value ->
            runCatching { StatusVisibility.valueOf(value) }.getOrNull()
        } ?: preferences[SettingsConstants.KEY_CONTENT_VISIBILITY]?.let { value ->
            when (runCatching { ContentVisibility.valueOf(value) }.getOrNull()) {
                ContentVisibility.EVERYONE -> StatusVisibility.EVERYONE
                ContentVisibility.FOLLOWERS -> StatusVisibility.MY_CONTACTS
                ContentVisibility.ONLY_ME -> StatusVisibility.NOBODY
                null -> SettingsConstants.DEFAULT_STATUS_VISIBILITY
            }
        } ?: SettingsConstants.DEFAULT_STATUS_VISIBILITY
    }

    override val lastSeenExcludedUserIds: Flow<Set<String>> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_LAST_SEEN_EXCLUDED_USER_IDS] ?: emptySet()
    }

    override val profilePhotoExcludedUserIds: Flow<Set<String>> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_PROFILE_PHOTO_EXCLUDED_USER_IDS] ?: emptySet()
    }

    override val aboutExcludedUserIds: Flow<Set<String>> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_ABOUT_EXCLUDED_USER_IDS] ?: emptySet()
    }

    override val statusExcludedUserIds: Flow<Set<String>> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_STATUS_EXCLUDED_USER_IDS] ?: emptySet()
    }

    override val groupExcludedUserIds: Flow<Set<String>> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_GROUP_EXCLUDED_USER_IDS] ?: emptySet()
    }

    override val privacySettings: Flow<PrivacySettings> = dataStore.safePreferencesFlow().map { preferences ->
        val legacyProfileVis = preferences[SettingsConstants.KEY_PROFILE_VISIBILITY]?.let { value ->
            runCatching { ProfileVisibility.valueOf(value) }.getOrNull()
        }
        val legacyContentVis = preferences[SettingsConstants.KEY_CONTENT_VISIBILITY]?.let { value ->
            runCatching { ContentVisibility.valueOf(value) }.getOrNull()
        }

        val lastSeen = preferences[SettingsConstants.KEY_LAST_SEEN_VISIBILITY]?.let { value ->
            runCatching { LastSeenVisibility.valueOf(value) }.getOrNull()
        } ?: when (legacyProfileVis) {
            ProfileVisibility.PUBLIC -> LastSeenVisibility.EVERYONE
            ProfileVisibility.FOLLOWERS_ONLY -> LastSeenVisibility.MY_CONTACTS
            ProfileVisibility.PRIVATE -> LastSeenVisibility.NOBODY
            null -> SettingsConstants.DEFAULT_LAST_SEEN_VISIBILITY
        }

        val profilePhoto = preferences[SettingsConstants.KEY_PROFILE_PHOTO_VISIBILITY]?.let { value ->
            runCatching { ProfilePhotoVisibility.valueOf(value) }.getOrNull()
        } ?: when (legacyProfileVis) {
            ProfileVisibility.PUBLIC -> ProfilePhotoVisibility.EVERYONE
            ProfileVisibility.FOLLOWERS_ONLY -> ProfilePhotoVisibility.MY_CONTACTS
            ProfileVisibility.PRIVATE -> ProfilePhotoVisibility.NOBODY
            null -> SettingsConstants.DEFAULT_PROFILE_PHOTO_VISIBILITY
        }

        val about = preferences[SettingsConstants.KEY_ABOUT_VISIBILITY]?.let { value ->
            runCatching { AboutVisibility.valueOf(value) }.getOrNull()
        } ?: when (legacyProfileVis) {
            ProfileVisibility.PUBLIC -> AboutVisibility.EVERYONE
            ProfileVisibility.FOLLOWERS_ONLY -> AboutVisibility.MY_CONTACTS
            ProfileVisibility.PRIVATE -> AboutVisibility.NOBODY
            null -> SettingsConstants.DEFAULT_ABOUT_VISIBILITY
        }

        val status = preferences[SettingsConstants.KEY_STATUS_VISIBILITY]?.let { value ->
            runCatching { StatusVisibility.valueOf(value) }.getOrNull()
        } ?: when (legacyContentVis) {
            ContentVisibility.EVERYONE -> StatusVisibility.EVERYONE
            ContentVisibility.FOLLOWERS -> StatusVisibility.MY_CONTACTS
            ContentVisibility.ONLY_ME -> StatusVisibility.NOBODY
            null -> SettingsConstants.DEFAULT_STATUS_VISIBILITY
        }

        PrivacySettings(
            profileVisibility = legacyProfileVis ?: SettingsConstants.DEFAULT_PROFILE_VISIBILITY,
            twoFactorEnabled = preferences[SettingsConstants.KEY_TWO_FACTOR_ENABLED] ?: SettingsConstants.DEFAULT_TWO_FACTOR_ENABLED,
            biometricLockEnabled = preferences[SettingsConstants.KEY_BIOMETRIC_LOCK_ENABLED] ?: SettingsConstants.DEFAULT_BIOMETRIC_LOCK_ENABLED,
            contentVisibility = legacyContentVis ?: SettingsConstants.DEFAULT_CONTENT_VISIBILITY,
            groupPrivacy = preferences[SettingsConstants.KEY_GROUP_PRIVACY]?.let { value ->
                runCatching { GroupPrivacy.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_GROUP_PRIVACY)
            } ?: SettingsConstants.DEFAULT_GROUP_PRIVACY,
            readReceiptsEnabled = preferences[SettingsConstants.KEY_READ_RECEIPTS_ENABLED] ?: SettingsConstants.DEFAULT_READ_RECEIPTS_ENABLED,
            appLockEnabled = preferences[SettingsConstants.KEY_APP_LOCK_ENABLED] ?: SettingsConstants.DEFAULT_APP_LOCK_ENABLED,
            chatLockEnabled = preferences[SettingsConstants.KEY_CHAT_LOCK_ENABLED] ?: SettingsConstants.DEFAULT_CHAT_LOCK_ENABLED,
            lastSeenVisibility = lastSeen,
            profilePhotoVisibility = profilePhoto,
            aboutVisibility = about,
            statusVisibility = status,
            lastSeenExcludedUserIds = preferences[SettingsConstants.KEY_LAST_SEEN_EXCLUDED_USER_IDS] ?: emptySet(),
            profilePhotoExcludedUserIds = preferences[SettingsConstants.KEY_PROFILE_PHOTO_EXCLUDED_USER_IDS] ?: emptySet(),
            aboutExcludedUserIds = preferences[SettingsConstants.KEY_ABOUT_EXCLUDED_USER_IDS] ?: emptySet(),
            statusExcludedUserIds = preferences[SettingsConstants.KEY_STATUS_EXCLUDED_USER_IDS] ?: emptySet(),
            groupExcludedUserIds = preferences[SettingsConstants.KEY_GROUP_EXCLUDED_USER_IDS] ?: emptySet()
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

    override suspend fun setLastSeenVisibility(visibility: LastSeenVisibility) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_LAST_SEEN_VISIBILITY] = visibility.name
        }
    }

    override suspend fun setProfilePhotoVisibility(visibility: ProfilePhotoVisibility) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_PROFILE_PHOTO_VISIBILITY] = visibility.name
        }
    }

    override suspend fun setAboutVisibility(visibility: AboutVisibility) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_ABOUT_VISIBILITY] = visibility.name
        }
    }

    override suspend fun setStatusVisibility(visibility: StatusVisibility) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_STATUS_VISIBILITY] = visibility.name
        }
    }

    override suspend fun setLastSeenExcludedUserIds(user: Set<String>) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_LAST_SEEN_EXCLUDED_USER_IDS] = user
        }
    }

    override suspend fun setProfilePhotoExcludedUserIds(user: Set<String>) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_PROFILE_PHOTO_EXCLUDED_USER_IDS] = user
        }
    }

    override suspend fun setAboutExcludedUserIds(user: Set<String>) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_ABOUT_EXCLUDED_USER_IDS] = user
        }
    }

    override suspend fun setStatusExcludedUserIds(user: Set<String>) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_STATUS_EXCLUDED_USER_IDS] = user
        }
    }

    override suspend fun setGroupExcludedUserIds(user: Set<String>) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_GROUP_EXCLUDED_USER_IDS] = user
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
