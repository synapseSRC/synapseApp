package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.synapse.social.studioasinc.ui.settings.NotificationCategory
import com.synapse.social.studioasinc.ui.settings.NotificationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NotificationStore {
    val notificationPreferences: Flow<NotificationPreferences>
    suspend fun updateNotificationPreference(category: NotificationCategory, enabled: Boolean)
    suspend fun setInAppNotificationsEnabled(enabled: Boolean)
}

class NotificationStoreImpl(private val dataStore: DataStore<Preferences>) : NotificationStore {
    override val notificationPreferences: Flow<NotificationPreferences> = dataStore.safePreferencesFlow().map { preferences ->
        NotificationPreferences(
            likesEnabled = preferences[SettingsConstants.KEY_NOTIFICATIONS_LIKES] ?: SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED,
            commentsEnabled = preferences[SettingsConstants.KEY_NOTIFICATIONS_COMMENTS] ?: SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED,
            followsEnabled = preferences[SettingsConstants.KEY_NOTIFICATIONS_FOLLOWS] ?: SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED,
            messagesEnabled = preferences[SettingsConstants.KEY_NOTIFICATIONS_MESSAGES] ?: SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED,
            mentionsEnabled = preferences[SettingsConstants.KEY_NOTIFICATIONS_MENTIONS] ?: SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED,
            inAppNotificationsEnabled = preferences[SettingsConstants.KEY_IN_APP_NOTIFICATIONS] ?: SettingsConstants.DEFAULT_IN_APP_NOTIFICATIONS_ENABLED
        )
    }

    override suspend fun updateNotificationPreference(category: NotificationCategory, enabled: Boolean) {
        dataStore.edit { preferences ->
            when (category) {
                NotificationCategory.LIKES -> preferences[SettingsConstants.KEY_NOTIFICATIONS_LIKES] = enabled
                NotificationCategory.COMMENTS -> preferences[SettingsConstants.KEY_NOTIFICATIONS_COMMENTS] = enabled
                NotificationCategory.FOLLOWS -> preferences[SettingsConstants.KEY_NOTIFICATIONS_FOLLOWS] = enabled
                NotificationCategory.MESSAGES -> preferences[SettingsConstants.KEY_NOTIFICATIONS_MESSAGES] = enabled
                NotificationCategory.MENTIONS -> preferences[SettingsConstants.KEY_NOTIFICATIONS_MENTIONS] = enabled
                NotificationCategory.REPLIES,
                NotificationCategory.NEW_POSTS,
                NotificationCategory.SHARES,
                NotificationCategory.SYSTEM_UPDATES -> {
                    // Do nothing or implement if needed
                }
            }
        }
    }

    override suspend fun setInAppNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_IN_APP_NOTIFICATIONS] = enabled
        }
    }
}
