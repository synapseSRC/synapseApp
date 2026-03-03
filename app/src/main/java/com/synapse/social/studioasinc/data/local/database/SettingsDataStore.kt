package com.synapse.social.studioasinc.data.local.database

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.synapse.social.studioasinc.ui.settings.AppearanceSettings
import com.synapse.social.studioasinc.ui.settings.ContentVisibility
import com.synapse.social.studioasinc.ui.settings.FontScale
import com.synapse.social.studioasinc.ui.settings.GroupPrivacy
import com.synapse.social.studioasinc.ui.settings.MediaAutoDownload
import com.synapse.social.studioasinc.ui.settings.NotificationCategory
import com.synapse.social.studioasinc.ui.settings.NotificationPreferences
import com.synapse.social.studioasinc.ui.settings.PrivacySettings
import com.synapse.social.studioasinc.ui.settings.ProfileVisibility
import com.synapse.social.studioasinc.ui.settings.ThemeMode
import com.synapse.social.studioasinc.domain.model.ChatThemePreset
import com.synapse.social.studioasinc.domain.model.ChatWallpaper
import com.synapse.social.studioasinc.domain.model.WallpaperType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException



private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "synapse_user_settings"
)



class SettingsDataStore private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SettingsDataStore"

        @Volatile
        private var INSTANCE: SettingsDataStore? = null

        fun getInstance(context: Context): SettingsDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }






        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        private val KEY_FONT_SCALE = stringPreferencesKey("font_scale")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_POST_VIEW_STYLE = stringPreferencesKey("post_view_style")






        private val KEY_PROFILE_VISIBILITY = stringPreferencesKey("profile_visibility")
        private val KEY_CONTENT_VISIBILITY = stringPreferencesKey("content_visibility")
        private val KEY_GROUP_PRIVACY = stringPreferencesKey("group_privacy")
        private val KEY_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        private val KEY_TWO_FACTOR_ENABLED = booleanPreferencesKey("two_factor_enabled")


        private val KEY_NOTIFICATIONS_LIKES = booleanPreferencesKey("notifications_likes")
        private val KEY_NOTIFICATIONS_COMMENTS = booleanPreferencesKey("notifications_comments")
        private val KEY_NOTIFICATIONS_FOLLOWS = booleanPreferencesKey("notifications_follows")
        private val KEY_NOTIFICATIONS_MESSAGES = booleanPreferencesKey("notifications_messages")
        private val KEY_NOTIFICATIONS_MENTIONS = booleanPreferencesKey("notifications_mentions")
        private val KEY_IN_APP_NOTIFICATIONS = booleanPreferencesKey("in_app_notifications")


        private val KEY_READ_RECEIPTS_ENABLED = booleanPreferencesKey("read_receipts_enabled")
        private val KEY_TYPING_INDICATORS_ENABLED = booleanPreferencesKey("typing_indicators_enabled")
        private val KEY_MEDIA_AUTO_DOWNLOAD = stringPreferencesKey("media_auto_download")
        private val KEY_CHAT_FONT_SCALE = floatPreferencesKey("chat_font_scale")
        private val KEY_CHAT_THEME_PRESET = stringPreferencesKey("chat_theme_preset")
        private val KEY_CHAT_WALLPAPER_TYPE = stringPreferencesKey("chat_wallpaper_type")
        private val KEY_CHAT_WALLPAPER_VALUE = stringPreferencesKey("chat_wallpaper_value")


        private val KEY_DATA_SAVER_ENABLED = booleanPreferencesKey("data_saver_enabled")


        private val KEY_ENTER_IS_SEND_ENABLED = booleanPreferencesKey("enter_is_send_enabled")
        private val KEY_MEDIA_VISIBILITY_ENABLED = booleanPreferencesKey("media_visibility_enabled")
        private val KEY_VOICE_TRANSCRIPTS_ENABLED = booleanPreferencesKey("voice_transcripts_enabled")
        private val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")


        private val KEY_REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        private val KEY_HIGH_PRIORITY_ENABLED = booleanPreferencesKey("high_priority_enabled")
        private val KEY_REACTION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("reaction_notifications_enabled")


        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_CHAT_LOCK_ENABLED = booleanPreferencesKey("chat_lock_enabled")


        private val KEY_MEDIA_UPLOAD_QUALITY = stringPreferencesKey("media_upload_quality")
        private val KEY_USE_LESS_DATA_CALLS = booleanPreferencesKey("use_less_data_calls")
        private val KEY_AUTO_DOWNLOAD_MOBILE = androidx.datastore.preferences.core.stringSetPreferencesKey("auto_download_mobile")
        private val KEY_AUTO_DOWNLOAD_WIFI = androidx.datastore.preferences.core.stringSetPreferencesKey("auto_download_wifi")
        private val KEY_AUTO_DOWNLOAD_ROAMING = androidx.datastore.preferences.core.stringSetPreferencesKey("auto_download_roaming")


        private val KEY_ACCOUNT_REPORTS_AUTO_CREATE = booleanPreferencesKey("account_reports_auto_create")
        private val KEY_CHANNELS_REPORTS_AUTO_CREATE = booleanPreferencesKey("channels_reports_auto_create")





        val DEFAULT_THEME_MODE = ThemeMode.SYSTEM
        val DEFAULT_DYNAMIC_COLOR_ENABLED = true
        val DEFAULT_FONT_SCALE = FontScale.MEDIUM
        val DEFAULT_APP_LANGUAGE = "en"
        val DEFAULT_PROFILE_VISIBILITY = ProfileVisibility.PUBLIC
        val DEFAULT_CONTENT_VISIBILITY = ContentVisibility.EVERYONE
        val DEFAULT_GROUP_PRIVACY = GroupPrivacy.EVERYONE
        val DEFAULT_BIOMETRIC_LOCK_ENABLED = false
        val DEFAULT_TWO_FACTOR_ENABLED = false
        val DEFAULT_NOTIFICATIONS_ENABLED = true
        val DEFAULT_IN_APP_NOTIFICATIONS_ENABLED = true
        val DEFAULT_READ_RECEIPTS_ENABLED = true
        val DEFAULT_TYPING_INDICATORS_ENABLED = true
        val DEFAULT_MEDIA_AUTO_DOWNLOAD = MediaAutoDownload.WIFI_ONLY
        val DEFAULT_CHAT_FONT_SCALE = 1.0f
        val DEFAULT_CHAT_THEME_PRESET = ChatThemePreset.DEFAULT
        val DEFAULT_CHAT_WALLPAPER_TYPE = WallpaperType.DEFAULT
        val DEFAULT_DATA_SAVER_ENABLED = false
        val DEFAULT_ENTER_IS_SEND_ENABLED = false
        val DEFAULT_MEDIA_VISIBILITY_ENABLED = true
        val DEFAULT_VOICE_TRANSCRIPTS_ENABLED = false
        val DEFAULT_AUTO_BACKUP_ENABLED = true
        val DEFAULT_REMINDERS_ENABLED = false
        val DEFAULT_HIGH_PRIORITY_ENABLED = true
        val DEFAULT_REACTION_NOTIFICATIONS_ENABLED = true
        val DEFAULT_APP_LOCK_ENABLED = false
        val DEFAULT_CHAT_LOCK_ENABLED = false
        val DEFAULT_ACCOUNT_REPORTS_AUTO_CREATE = false
        val DEFAULT_CHANNELS_REPORTS_AUTO_CREATE = false
    }

    private val dataStore: DataStore<Preferences>
        get() = context.settingsDataStore








    private fun safePreferencesFlow(): Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }







    val themeMode: Flow<ThemeMode> = safePreferencesFlow().map { preferences ->
        preferences[KEY_THEME_MODE]?.let { value ->
            runCatching { ThemeMode.valueOf(value) }.getOrDefault(DEFAULT_THEME_MODE)
        } ?: DEFAULT_THEME_MODE
    }



    val dynamicColorEnabled: Flow<Boolean> = safePreferencesFlow().map { preferences ->
        preferences[KEY_DYNAMIC_COLOR_ENABLED] ?: DEFAULT_DYNAMIC_COLOR_ENABLED
    }



    val fontScale: Flow<FontScale> = safePreferencesFlow().map { preferences ->
        preferences[KEY_FONT_SCALE]?.let { value ->
            runCatching { FontScale.valueOf(value) }.getOrDefault(DEFAULT_FONT_SCALE)
        } ?: DEFAULT_FONT_SCALE
    }



    val appearanceSettings: Flow<AppearanceSettings> = safePreferencesFlow().map { preferences ->
        AppearanceSettings(
            themeMode = preferences[KEY_THEME_MODE]?.let { value ->
                runCatching { ThemeMode.valueOf(value) }.getOrDefault(DEFAULT_THEME_MODE)
            } ?: DEFAULT_THEME_MODE,
            dynamicColorEnabled = preferences[KEY_DYNAMIC_COLOR_ENABLED] ?: DEFAULT_DYNAMIC_COLOR_ENABLED,
            fontScale = preferences[KEY_FONT_SCALE]?.let { value ->
                runCatching { FontScale.valueOf(value) }.getOrDefault(DEFAULT_FONT_SCALE)
            } ?: DEFAULT_FONT_SCALE,
            postViewStyle = preferences[KEY_POST_VIEW_STYLE]?.let { value ->
                runCatching { com.synapse.social.studioasinc.ui.settings.PostViewStyle.valueOf(value) }
                    .getOrDefault(com.synapse.social.studioasinc.ui.settings.PostViewStyle.SWIPE)
            } ?: com.synapse.social.studioasinc.ui.settings.PostViewStyle.SWIPE
        )
    }



    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }



    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLOR_ENABLED] = enabled
        }
    }



    suspend fun setFontScale(scale: FontScale) {
        dataStore.edit { preferences ->
            preferences[KEY_FONT_SCALE] = scale.name
        }
    }



    suspend fun setPostViewStyle(style: com.synapse.social.studioasinc.ui.settings.PostViewStyle) {
        dataStore.edit { preferences ->
            preferences[KEY_POST_VIEW_STYLE] = style.name
        }
    }



    val language: Flow<String> = safePreferencesFlow().map { preferences ->
        preferences[KEY_APP_LANGUAGE] ?: DEFAULT_APP_LANGUAGE
    }



    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = languageCode
        }
    }








    val profileVisibility: Flow<ProfileVisibility> = safePreferencesFlow().map { preferences ->
        preferences[KEY_PROFILE_VISIBILITY]?.let { value ->
            runCatching { ProfileVisibility.valueOf(value) }.getOrDefault(DEFAULT_PROFILE_VISIBILITY)
        } ?: DEFAULT_PROFILE_VISIBILITY
    }



    val contentVisibility: Flow<ContentVisibility> = safePreferencesFlow().map { preferences ->
        preferences[KEY_CONTENT_VISIBILITY]?.let { value ->
            runCatching { ContentVisibility.valueOf(value) }.getOrDefault(DEFAULT_CONTENT_VISIBILITY)
        } ?: DEFAULT_CONTENT_VISIBILITY
    }



    val biometricLockEnabled: Flow<Boolean> = safePreferencesFlow().map { preferences ->
        preferences[KEY_BIOMETRIC_LOCK_ENABLED] ?: DEFAULT_BIOMETRIC_LOCK_ENABLED
    }



    val twoFactorEnabled: Flow<Boolean> = safePreferencesFlow().map { preferences ->
        preferences[KEY_TWO_FACTOR_ENABLED] ?: DEFAULT_TWO_FACTOR_ENABLED
    }



    val privacySettings: Flow<PrivacySettings> = safePreferencesFlow().map { preferences ->
        PrivacySettings(
            profileVisibility = preferences[KEY_PROFILE_VISIBILITY]?.let { value ->
                runCatching { ProfileVisibility.valueOf(value) }.getOrDefault(DEFAULT_PROFILE_VISIBILITY)
            } ?: DEFAULT_PROFILE_VISIBILITY,
            twoFactorEnabled = preferences[KEY_TWO_FACTOR_ENABLED] ?: DEFAULT_TWO_FACTOR_ENABLED,
            biometricLockEnabled = preferences[KEY_BIOMETRIC_LOCK_ENABLED] ?: DEFAULT_BIOMETRIC_LOCK_ENABLED,
            contentVisibility = preferences[KEY_CONTENT_VISIBILITY]?.let { value ->
                runCatching { ContentVisibility.valueOf(value) }.getOrDefault(DEFAULT_CONTENT_VISIBILITY)
            } ?: DEFAULT_CONTENT_VISIBILITY,
            groupPrivacy = preferences[KEY_GROUP_PRIVACY]?.let { value ->
                runCatching { GroupPrivacy.valueOf(value) }.getOrDefault(DEFAULT_GROUP_PRIVACY)
            } ?: DEFAULT_GROUP_PRIVACY,
            readReceiptsEnabled = preferences[KEY_READ_RECEIPTS_ENABLED] ?: DEFAULT_READ_RECEIPTS_ENABLED,
            appLockEnabled = preferences[KEY_APP_LOCK_ENABLED] ?: DEFAULT_APP_LOCK_ENABLED,
            chatLockEnabled = preferences[KEY_CHAT_LOCK_ENABLED] ?: DEFAULT_CHAT_LOCK_ENABLED
        )
    }



    suspend fun setProfileVisibility(visibility: ProfileVisibility) {
        dataStore.edit { preferences ->
            preferences[KEY_PROFILE_VISIBILITY] = visibility.name
        }
    }



    suspend fun setContentVisibility(visibility: ContentVisibility) {
        dataStore.edit { preferences ->
            preferences[KEY_CONTENT_VISIBILITY] = visibility.name
        }
    }



    suspend fun setGroupPrivacy(privacy: GroupPrivacy) {
        dataStore.edit { preferences ->
            preferences[KEY_GROUP_PRIVACY] = privacy.name
        }
    }



    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }



    suspend fun setTwoFactorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_TWO_FACTOR_ENABLED] = enabled
        }
    }








    val notificationPreferences: Flow<NotificationPreferences> = safePreferencesFlow().map { preferences ->
        NotificationPreferences(
            likesEnabled = preferences[KEY_NOTIFICATIONS_LIKES] ?: DEFAULT_NOTIFICATIONS_ENABLED,
            commentsEnabled = preferences[KEY_NOTIFICATIONS_COMMENTS] ?: DEFAULT_NOTIFICATIONS_ENABLED,
            followsEnabled = preferences[KEY_NOTIFICATIONS_FOLLOWS] ?: DEFAULT_NOTIFICATIONS_ENABLED,
            messagesEnabled = preferences[KEY_NOTIFICATIONS_MESSAGES] ?: DEFAULT_NOTIFICATIONS_ENABLED,
            mentionsEnabled = preferences[KEY_NOTIFICATIONS_MENTIONS] ?: DEFAULT_NOTIFICATIONS_ENABLED,
            inAppNotificationsEnabled = preferences[KEY_IN_APP_NOTIFICATIONS] ?: DEFAULT_IN_APP_NOTIFICATIONS_ENABLED
        )
    }



    suspend fun updateNotificationPreference(category: NotificationCategory, enabled: Boolean) {
        dataStore.edit { preferences ->
            when (category) {
                NotificationCategory.LIKES -> preferences[KEY_NOTIFICATIONS_LIKES] = enabled
                NotificationCategory.COMMENTS -> preferences[KEY_NOTIFICATIONS_COMMENTS] = enabled
                NotificationCategory.FOLLOWS -> preferences[KEY_NOTIFICATIONS_FOLLOWS] = enabled
                NotificationCategory.MESSAGES -> preferences[KEY_NOTIFICATIONS_MESSAGES] = enabled
                NotificationCategory.MENTIONS -> preferences[KEY_NOTIFICATIONS_MENTIONS] = enabled
                NotificationCategory.REPLIES,
                NotificationCategory.NEW_POSTS,
                NotificationCategory.SHARES,
                NotificationCategory.SYSTEM_UPDATES -> {

                }
            }
        }
    }



    suspend fun setInAppNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IN_APP_NOTIFICATIONS] = enabled
        }
    }









    suspend fun setReadReceiptsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_READ_RECEIPTS_ENABLED] = enabled
        }
    }



    val mediaUploadQuality: Flow<com.synapse.social.studioasinc.ui.settings.MediaUploadQuality> = safePreferencesFlow().map { preferences ->
        preferences[KEY_MEDIA_UPLOAD_QUALITY]?.let { value ->
            runCatching { com.synapse.social.studioasinc.ui.settings.MediaUploadQuality.valueOf(value) }
                .getOrDefault(com.synapse.social.studioasinc.ui.settings.MediaUploadQuality.STANDARD)
        } ?: com.synapse.social.studioasinc.ui.settings.MediaUploadQuality.STANDARD
    }



    suspend fun setMediaUploadQuality(quality: com.synapse.social.studioasinc.ui.settings.MediaUploadQuality) {
        dataStore.edit { preferences ->
            preferences[KEY_MEDIA_UPLOAD_QUALITY] = quality.name
        }
    }



    val useLessDataCalls: Flow<Boolean> = safePreferencesFlow().map { preferences ->
        preferences[KEY_USE_LESS_DATA_CALLS] ?: false
    }



    suspend fun setUseLessDataCalls(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_USE_LESS_DATA_CALLS] = enabled
        }
    }



    val autoDownloadRules: Flow<com.synapse.social.studioasinc.ui.settings.AutoDownloadRules> = safePreferencesFlow().map { preferences ->
        com.synapse.social.studioasinc.ui.settings.AutoDownloadRules(
            mobileData = preferences[KEY_AUTO_DOWNLOAD_MOBILE]?.mapNotNull {
                runCatching { com.synapse.social.studioasinc.ui.settings.MediaType.valueOf(it) }.getOrNull()
            }?.toSet() ?: setOf(com.synapse.social.studioasinc.ui.settings.MediaType.PHOTO),

            wifi = preferences[KEY_AUTO_DOWNLOAD_WIFI]?.mapNotNull {
                runCatching { com.synapse.social.studioasinc.ui.settings.MediaType.valueOf(it) }.getOrNull()
            }?.toSet() ?: com.synapse.social.studioasinc.ui.settings.MediaType.values().toSet(),

            roaming = preferences[KEY_AUTO_DOWNLOAD_ROAMING]?.mapNotNull {
                runCatching { com.synapse.social.studioasinc.ui.settings.MediaType.valueOf(it) }.getOrNull()
            }?.toSet() ?: emptySet()
        )
    }



    suspend fun setAutoDownloadRule(
        networkType: String,
        mediaTypes: Set<com.synapse.social.studioasinc.ui.settings.MediaType>
    ) {
        dataStore.edit { preferences ->
            val key = when (networkType) {
                "mobile" -> KEY_AUTO_DOWNLOAD_MOBILE
                "wifi" -> KEY_AUTO_DOWNLOAD_WIFI
                "roaming" -> KEY_AUTO_DOWNLOAD_ROAMING
                else -> return@edit
            }
            preferences[key] = mediaTypes.map { it.name }.toSet()
        }
    }







    val dataSaverEnabled: Flow<Boolean> = safePreferencesFlow().map { preferences ->
        preferences[KEY_DATA_SAVER_ENABLED] ?: DEFAULT_DATA_SAVER_ENABLED
    }



    suspend fun setDataSaverEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DATA_SAVER_ENABLED] = enabled
        }
    }



    suspend fun setEnterIsSendEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ENTER_IS_SEND_ENABLED] = enabled
        }
    }



    suspend fun setMediaVisibilityEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_MEDIA_VISIBILITY_ENABLED] = enabled
        }
    }



    suspend fun setVoiceTranscriptsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_VOICE_TRANSCRIPTS_ENABLED] = enabled
        }
    }



    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_BACKUP_ENABLED] = enabled
        }
    }



    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_REMINDERS_ENABLED] = enabled
        }
    }



    suspend fun setHighPriorityEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HIGH_PRIORITY_ENABLED] = enabled
        }
    }



    suspend fun setReactionNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_REACTION_NOTIFICATIONS_ENABLED] = enabled
        }
    }



    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] = enabled
        }
    }



    suspend fun setChatLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_CHAT_LOCK_ENABLED] = enabled
        }
    }







    val accountReportsAutoCreate: Flow<Boolean> = safePreferencesFlow().map { preferences ->
        preferences[KEY_ACCOUNT_REPORTS_AUTO_CREATE] ?: DEFAULT_ACCOUNT_REPORTS_AUTO_CREATE
    }



    suspend fun setAccountReportsAutoCreate(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCOUNT_REPORTS_AUTO_CREATE] = enabled
        }
    }



    val channelsReportsAutoCreate: Flow<Boolean> = safePreferencesFlow().map { preferences ->
        preferences[KEY_CHANNELS_REPORTS_AUTO_CREATE] ?: DEFAULT_CHANNELS_REPORTS_AUTO_CREATE
    }



    suspend fun setChannelsReportsAutoCreate(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_CHANNELS_REPORTS_AUTO_CREATE] = enabled
        }
    }







    suspend fun clearUserSettings() {
        dataStore.edit { preferences ->


            preferences.remove(KEY_PROFILE_VISIBILITY)
            preferences.remove(KEY_CONTENT_VISIBILITY)
            preferences.remove(KEY_GROUP_PRIVACY)
            preferences.remove(KEY_BIOMETRIC_LOCK_ENABLED)
            preferences.remove(KEY_TWO_FACTOR_ENABLED)


            preferences.remove(KEY_NOTIFICATIONS_LIKES)
            preferences.remove(KEY_NOTIFICATIONS_COMMENTS)
            preferences.remove(KEY_NOTIFICATIONS_FOLLOWS)
            preferences.remove(KEY_NOTIFICATIONS_MESSAGES)
            preferences.remove(KEY_NOTIFICATIONS_MENTIONS)
            preferences.remove(KEY_IN_APP_NOTIFICATIONS)


            preferences.remove(KEY_READ_RECEIPTS_ENABLED)
            preferences.remove(KEY_TYPING_INDICATORS_ENABLED)
            preferences.remove(KEY_MEDIA_AUTO_DOWNLOAD)
            preferences.remove<Float>(KEY_CHAT_FONT_SCALE)
            preferences.remove(KEY_CHAT_THEME_PRESET)
            preferences.remove(KEY_CHAT_WALLPAPER_TYPE)
            preferences.remove(KEY_CHAT_WALLPAPER_VALUE)


            preferences.remove(KEY_DATA_SAVER_ENABLED)


            preferences.remove(KEY_MEDIA_UPLOAD_QUALITY)
            preferences.remove(KEY_USE_LESS_DATA_CALLS)
            preferences.remove(KEY_AUTO_DOWNLOAD_MOBILE)
            preferences.remove(KEY_AUTO_DOWNLOAD_WIFI)
            preferences.remove(KEY_AUTO_DOWNLOAD_ROAMING)


            preferences.remove(KEY_ACCOUNT_REPORTS_AUTO_CREATE)
            preferences.remove(KEY_CHANNELS_REPORTS_AUTO_CREATE)
        }
    }



    suspend fun clearAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }



    suspend fun restoreDefaults() {
        dataStore.edit { preferences ->

            preferences[KEY_THEME_MODE] = DEFAULT_THEME_MODE.name
            preferences[KEY_DYNAMIC_COLOR_ENABLED] = DEFAULT_DYNAMIC_COLOR_ENABLED
            preferences[KEY_FONT_SCALE] = DEFAULT_FONT_SCALE.name
            preferences[KEY_APP_LANGUAGE] = DEFAULT_APP_LANGUAGE


            preferences[KEY_PROFILE_VISIBILITY] = DEFAULT_PROFILE_VISIBILITY.name
            preferences[KEY_CONTENT_VISIBILITY] = DEFAULT_CONTENT_VISIBILITY.name
            preferences[KEY_GROUP_PRIVACY] = DEFAULT_GROUP_PRIVACY.name
            preferences[KEY_BIOMETRIC_LOCK_ENABLED] = DEFAULT_BIOMETRIC_LOCK_ENABLED
            preferences[KEY_TWO_FACTOR_ENABLED] = DEFAULT_TWO_FACTOR_ENABLED


            preferences[KEY_NOTIFICATIONS_LIKES] = DEFAULT_NOTIFICATIONS_ENABLED
            preferences[KEY_NOTIFICATIONS_COMMENTS] = DEFAULT_NOTIFICATIONS_ENABLED
            preferences[KEY_NOTIFICATIONS_FOLLOWS] = DEFAULT_NOTIFICATIONS_ENABLED
            preferences[KEY_NOTIFICATIONS_MESSAGES] = DEFAULT_NOTIFICATIONS_ENABLED
            preferences[KEY_NOTIFICATIONS_MENTIONS] = DEFAULT_NOTIFICATIONS_ENABLED
            preferences[KEY_IN_APP_NOTIFICATIONS] = DEFAULT_IN_APP_NOTIFICATIONS_ENABLED


            preferences[KEY_READ_RECEIPTS_ENABLED] = DEFAULT_READ_RECEIPTS_ENABLED
            preferences[KEY_TYPING_INDICATORS_ENABLED] = DEFAULT_TYPING_INDICATORS_ENABLED
            preferences[KEY_MEDIA_AUTO_DOWNLOAD] = DEFAULT_MEDIA_AUTO_DOWNLOAD.name
            preferences[KEY_CHAT_FONT_SCALE] = DEFAULT_CHAT_FONT_SCALE
            preferences[KEY_CHAT_THEME_PRESET] = DEFAULT_CHAT_THEME_PRESET.name
            preferences[KEY_CHAT_WALLPAPER_TYPE] = DEFAULT_CHAT_WALLPAPER_TYPE.name
            preferences.remove(KEY_CHAT_WALLPAPER_VALUE)


            preferences[KEY_DATA_SAVER_ENABLED] = DEFAULT_DATA_SAVER_ENABLED


            preferences[KEY_MEDIA_UPLOAD_QUALITY] = com.synapse.social.studioasinc.ui.settings.MediaUploadQuality.STANDARD.name
            preferences[KEY_USE_LESS_DATA_CALLS] = false
            preferences[KEY_AUTO_DOWNLOAD_MOBILE] = setOf(com.synapse.social.studioasinc.ui.settings.MediaType.PHOTO.name)
            preferences[KEY_AUTO_DOWNLOAD_WIFI] = com.synapse.social.studioasinc.ui.settings.MediaType.values().map { it.name }.toSet()
            preferences[KEY_AUTO_DOWNLOAD_ROAMING] = emptySet()


            preferences[KEY_ACCOUNT_REPORTS_AUTO_CREATE] = DEFAULT_ACCOUNT_REPORTS_AUTO_CREATE
            preferences[KEY_CHANNELS_REPORTS_AUTO_CREATE] = DEFAULT_CHANNELS_REPORTS_AUTO_CREATE
        }
    }
}
