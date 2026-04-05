package com.synapse.social.studioasinc.data.local.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.synapse.social.studioasinc.data.local.database.settings.*

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "synapse_user_settings"
)

class SettingsDataStore private constructor(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.settingsDataStore,
    private val appearanceStore: AppearanceStore = AppearanceStoreImpl(dataStore),
    private val chatStore: ChatStore = ChatStoreImpl(dataStore),
    private val privacyStore: PrivacyStore = PrivacyStoreImpl(dataStore),
    private val notificationStore: NotificationStore = NotificationStoreImpl(dataStore),
    private val mediaStore: MediaStore = MediaStoreImpl(dataStore),
    private val aiStore: AiStore = AiStoreImpl(dataStore),
    private val generalStore: GeneralStore = GeneralStoreImpl(dataStore)
) : AppearanceStore by appearanceStore,
    ChatStore by chatStore,
    PrivacyStore by privacyStore,
    NotificationStore by notificationStore,
    MediaStore by mediaStore,
    AiStore by aiStore,
    GeneralStore by generalStore {

    companion object {
        private const val TAG = "SettingsDataStore"

        @Volatile
        private var INSTANCE: SettingsDataStore? = null

        fun getInstance(context: Context): SettingsDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun clearUserSettings() {
        dataStore.edit { preferences ->
            preferences.remove(SettingsConstants.KEY_PROFILE_VISIBILITY)
            preferences.remove(SettingsConstants.KEY_CONTENT_VISIBILITY)
            preferences.remove(SettingsConstants.KEY_GROUP_PRIVACY)
            preferences.remove(SettingsConstants.KEY_BIOMETRIC_LOCK_ENABLED)
            preferences.remove(SettingsConstants.KEY_TWO_FACTOR_ENABLED)

            preferences.remove(SettingsConstants.KEY_NOTIFICATIONS_LIKES)
            preferences.remove(SettingsConstants.KEY_NOTIFICATIONS_COMMENTS)
            preferences.remove(SettingsConstants.KEY_NOTIFICATIONS_FOLLOWS)
            preferences.remove(SettingsConstants.KEY_NOTIFICATIONS_MESSAGES)
            preferences.remove(SettingsConstants.KEY_NOTIFICATIONS_MENTIONS)
            preferences.remove(SettingsConstants.KEY_IN_APP_NOTIFICATIONS)

            preferences.remove(SettingsConstants.KEY_READ_RECEIPTS_ENABLED)
            preferences.remove(SettingsConstants.KEY_TYPING_INDICATORS_ENABLED)
            preferences.remove(SettingsConstants.KEY_MEDIA_AUTO_DOWNLOAD)
            preferences.remove<Float>(SettingsConstants.KEY_CHAT_FONT_SCALE)
            preferences.remove(SettingsConstants.KEY_CHAT_THEME_PRESET)
            preferences.remove(SettingsConstants.KEY_CHAT_WALLPAPER_TYPE)
            preferences.remove(SettingsConstants.KEY_CHAT_WALLPAPER_VALUE)
            preferences.remove(SettingsConstants.KEY_CHAT_WALLPAPER_BLUR)
            preferences.remove(SettingsConstants.KEY_CHAT_MESSAGE_CORNER_RADIUS)
            preferences.remove(SettingsConstants.KEY_CHAT_LIST_LAYOUT)
            preferences.remove(SettingsConstants.KEY_CHAT_SWIPE_GESTURE)
            preferences.remove(SettingsConstants.KEY_CHAT_FOLDERS)
            preferences.remove(SettingsConstants.KEY_CHAT_MAX_MESSAGE_CHUNK_SIZE)
            preferences.remove(SettingsConstants.KEY_MESSAGE_SUGGESTION_ENABLED)
            preferences.remove(SettingsConstants.KEY_CHAT_AVATAR_DISABLED)

            preferences.remove(SettingsConstants.KEY_DATA_SAVER_ENABLED)

            preferences.remove(SettingsConstants.KEY_MEDIA_UPLOAD_QUALITY)
            preferences.remove(SettingsConstants.KEY_USE_LESS_DATA_CALLS)
            preferences.remove(SettingsConstants.KEY_AUTO_DOWNLOAD_MOBILE)
            preferences.remove(SettingsConstants.KEY_AUTO_DOWNLOAD_WIFI)
            preferences.remove(SettingsConstants.KEY_AUTO_DOWNLOAD_ROAMING)

            preferences.remove(SettingsConstants.KEY_ACCOUNT_REPORTS_AUTO_CREATE)
            preferences.remove(SettingsConstants.KEY_CHANNELS_REPORTS_AUTO_CREATE)
            preferences.remove(SettingsConstants.KEY_HIDE_PROFILE_PIC_SUGGESTION)
        }
    }

    suspend fun clearAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun restoreDefaults() {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_THEME_MODE] = SettingsConstants.DEFAULT_THEME_MODE.name
            preferences[SettingsConstants.KEY_DYNAMIC_COLOR_ENABLED] = SettingsConstants.DEFAULT_DYNAMIC_COLOR_ENABLED
            preferences[SettingsConstants.KEY_FONT_SCALE] = SettingsConstants.DEFAULT_FONT_SCALE.name
            preferences[SettingsConstants.KEY_APP_LANGUAGE] = SettingsConstants.DEFAULT_APP_LANGUAGE

            preferences[SettingsConstants.KEY_PROFILE_VISIBILITY] = SettingsConstants.DEFAULT_PROFILE_VISIBILITY.name
            preferences[SettingsConstants.KEY_CONTENT_VISIBILITY] = SettingsConstants.DEFAULT_CONTENT_VISIBILITY.name
            preferences[SettingsConstants.KEY_GROUP_PRIVACY] = SettingsConstants.DEFAULT_GROUP_PRIVACY.name
            preferences[SettingsConstants.KEY_BIOMETRIC_LOCK_ENABLED] = SettingsConstants.DEFAULT_BIOMETRIC_LOCK_ENABLED
            preferences[SettingsConstants.KEY_TWO_FACTOR_ENABLED] = SettingsConstants.DEFAULT_TWO_FACTOR_ENABLED

            preferences[SettingsConstants.KEY_NOTIFICATIONS_LIKES] = SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED
            preferences[SettingsConstants.KEY_NOTIFICATIONS_COMMENTS] = SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED
            preferences[SettingsConstants.KEY_NOTIFICATIONS_FOLLOWS] = SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED
            preferences[SettingsConstants.KEY_NOTIFICATIONS_MESSAGES] = SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED
            preferences[SettingsConstants.KEY_NOTIFICATIONS_MENTIONS] = SettingsConstants.DEFAULT_NOTIFICATIONS_ENABLED
            preferences[SettingsConstants.KEY_IN_APP_NOTIFICATIONS] = SettingsConstants.DEFAULT_IN_APP_NOTIFICATIONS_ENABLED

            preferences[SettingsConstants.KEY_READ_RECEIPTS_ENABLED] = SettingsConstants.DEFAULT_READ_RECEIPTS_ENABLED
            preferences[SettingsConstants.KEY_TYPING_INDICATORS_ENABLED] = SettingsConstants.DEFAULT_TYPING_INDICATORS_ENABLED
            preferences[SettingsConstants.KEY_MEDIA_AUTO_DOWNLOAD] = SettingsConstants.DEFAULT_MEDIA_AUTO_DOWNLOAD.name
            preferences[SettingsConstants.KEY_CHAT_FONT_SCALE] = SettingsConstants.DEFAULT_CHAT_FONT_SCALE
            preferences[SettingsConstants.KEY_CHAT_THEME_PRESET] = SettingsConstants.DEFAULT_CHAT_THEME_PRESET.name
            preferences[SettingsConstants.KEY_CHAT_WALLPAPER_TYPE] = SettingsConstants.DEFAULT_CHAT_WALLPAPER_TYPE.name
            preferences.remove(SettingsConstants.KEY_CHAT_WALLPAPER_VALUE)
            preferences.remove(SettingsConstants.KEY_CHAT_WALLPAPER_BLUR)
            preferences[SettingsConstants.KEY_CHAT_MESSAGE_CORNER_RADIUS] = SettingsConstants.DEFAULT_CHAT_MESSAGE_CORNER_RADIUS
            preferences[SettingsConstants.KEY_CHAT_LIST_LAYOUT] = SettingsConstants.DEFAULT_CHAT_LIST_LAYOUT.name
            preferences[SettingsConstants.KEY_CHAT_SWIPE_GESTURE] = SettingsConstants.DEFAULT_CHAT_SWIPE_GESTURE.name
            preferences.remove(SettingsConstants.KEY_CHAT_FOLDERS)
            preferences[SettingsConstants.KEY_CHAT_MAX_MESSAGE_CHUNK_SIZE] = SettingsConstants.DEFAULT_CHAT_MAX_MESSAGE_CHUNK_SIZE
            preferences[SettingsConstants.KEY_MESSAGE_SUGGESTION_ENABLED] = SettingsConstants.DEFAULT_MESSAGE_SUGGESTION_ENABLED
            preferences[SettingsConstants.KEY_CHAT_AVATAR_DISABLED] = SettingsConstants.DEFAULT_CHAT_AVATAR_DISABLED

            preferences[SettingsConstants.KEY_DATA_SAVER_ENABLED] = SettingsConstants.DEFAULT_DATA_SAVER_ENABLED

            preferences[SettingsConstants.KEY_MEDIA_UPLOAD_QUALITY] = com.synapse.social.studioasinc.ui.settings.MediaUploadQuality.STANDARD.name
            preferences[SettingsConstants.KEY_USE_LESS_DATA_CALLS] = false
            preferences[SettingsConstants.KEY_AUTO_DOWNLOAD_MOBILE] = setOf(com.synapse.social.studioasinc.ui.settings.MediaType.PHOTO.name)
            preferences[SettingsConstants.KEY_AUTO_DOWNLOAD_WIFI] = SettingsConstants.DEFAULT_AUTO_DOWNLOAD_WIFI_STRINGS
            preferences[SettingsConstants.KEY_AUTO_DOWNLOAD_ROAMING] = emptySet()

            preferences[SettingsConstants.KEY_ACCOUNT_REPORTS_AUTO_CREATE] = SettingsConstants.DEFAULT_ACCOUNT_REPORTS_AUTO_CREATE
            preferences[SettingsConstants.KEY_CHANNELS_REPORTS_AUTO_CREATE] = SettingsConstants.DEFAULT_CHANNELS_REPORTS_AUTO_CREATE
            preferences[SettingsConstants.KEY_HIDE_PROFILE_PIC_SUGGESTION] = false
        }
    }
}
