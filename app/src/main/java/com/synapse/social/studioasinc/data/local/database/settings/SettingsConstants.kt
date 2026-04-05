package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType

object SettingsConstants {
    const val DEFAULT_KEEP_MEDIA_DAYS = 7
    const val DEFAULT_MAX_CACHE_SIZE_GB = 5

    val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    val KEY_DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    val KEY_FONT_SCALE = stringPreferencesKey("font_scale")
    val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    val KEY_POST_VIEW_STYLE = stringPreferencesKey("post_view_style")

    val KEY_PROFILE_VISIBILITY = stringPreferencesKey("profile_visibility")
    val KEY_CONTENT_VISIBILITY = stringPreferencesKey("content_visibility")
    val KEY_GROUP_PRIVACY = stringPreferencesKey("group_privacy")
    val KEY_KEEP_MEDIA_DAYS = intPreferencesKey("keep_media_days")
    val KEY_MAX_CACHE_SIZE_GB = intPreferencesKey("max_cache_size_gb")
    val KEY_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
    val KEY_TWO_FACTOR_ENABLED = booleanPreferencesKey("two_factor_enabled")

    val KEY_NOTIFICATIONS_LIKES = booleanPreferencesKey("notifications_likes")
    val KEY_NOTIFICATIONS_COMMENTS = booleanPreferencesKey("notifications_comments")
    val KEY_NOTIFICATIONS_FOLLOWS = booleanPreferencesKey("notifications_follows")
    val KEY_NOTIFICATIONS_MESSAGES = booleanPreferencesKey("notifications_messages")
    val KEY_NOTIFICATIONS_MENTIONS = booleanPreferencesKey("notifications_mentions")
    val KEY_IN_APP_NOTIFICATIONS = booleanPreferencesKey("in_app_notifications")

    val KEY_READ_RECEIPTS_ENABLED = booleanPreferencesKey("read_receipts_enabled")
    val KEY_TYPING_INDICATORS_ENABLED = booleanPreferencesKey("typing_indicators_enabled")
    val KEY_MEDIA_AUTO_DOWNLOAD = stringPreferencesKey("media_auto_download")
    val KEY_CHAT_FONT_SCALE = floatPreferencesKey("chat_font_scale")
    val KEY_CHAT_THEME_PRESET = stringPreferencesKey("chat_theme_preset")
    val KEY_CHAT_WALLPAPER_TYPE = stringPreferencesKey("chat_wallpaper_type")
    val KEY_CHAT_WALLPAPER_VALUE = stringPreferencesKey("chat_wallpaper_value")
    val KEY_CHAT_WALLPAPER_BLUR = floatPreferencesKey("chat_wallpaper_blur")

    val KEY_CHAT_MESSAGE_CORNER_RADIUS = intPreferencesKey("chat_message_corner_radius")
    val KEY_CHAT_LIST_LAYOUT = stringPreferencesKey("chat_list_layout")
    val KEY_CHAT_SWIPE_GESTURE = stringPreferencesKey("chat_swipe_gesture")
    val KEY_CHAT_FOLDERS = stringPreferencesKey("chat_folders")
    val KEY_CHAT_MAX_MESSAGE_CHUNK_SIZE = intPreferencesKey("chat_max_message_chunk_size")
    val KEY_AI_PREFERRED_PROVIDER = stringPreferencesKey("ai_preferred_provider")
    val KEY_AI_FALLBACK_TO_PLATFORM = booleanPreferencesKey("ai_fallback_to_platform")
    val KEY_AI_CUSTOM_MODEL = stringPreferencesKey("ai_custom_model")
    val KEY_MESSAGE_SUGGESTION_ENABLED = booleanPreferencesKey("message_suggestion_enabled")
    val KEY_CHAT_AVATAR_DISABLED = booleanPreferencesKey("chat_avatar_disabled")

    val KEY_DATA_SAVER_ENABLED = booleanPreferencesKey("data_saver_enabled")

    val KEY_ENTER_IS_SEND_ENABLED = booleanPreferencesKey("enter_is_send_enabled")
    val KEY_MEDIA_VISIBILITY_ENABLED = booleanPreferencesKey("media_visibility_enabled")
    val KEY_VOICE_TRANSCRIPTS_ENABLED = booleanPreferencesKey("voice_transcripts_enabled")
    val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")

    val KEY_REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    val KEY_HIGH_PRIORITY_ENABLED = booleanPreferencesKey("high_priority_enabled")
    val KEY_REACTION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("reaction_notifications_enabled")

    val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    val KEY_CHAT_LOCK_ENABLED = booleanPreferencesKey("chat_lock_enabled")

    val KEY_MEDIA_UPLOAD_QUALITY = stringPreferencesKey("media_upload_quality")
    val KEY_USE_LESS_DATA_CALLS = booleanPreferencesKey("use_less_data_calls")
    val KEY_AUTO_DOWNLOAD_MOBILE = stringSetPreferencesKey("auto_download_mobile")
    val KEY_AUTO_DOWNLOAD_WIFI = stringSetPreferencesKey("auto_download_wifi")
    val KEY_AUTO_DOWNLOAD_ROAMING = stringSetPreferencesKey("auto_download_roaming")

    val KEY_ACCOUNT_REPORTS_AUTO_CREATE = booleanPreferencesKey("account_reports_auto_create")
    val KEY_CHANNELS_REPORTS_AUTO_CREATE = booleanPreferencesKey("channels_reports_auto_create")
    val KEY_HIDE_PROFILE_PIC_SUGGESTION = booleanPreferencesKey("hide_profile_pic_suggestion")
    val KEY_SEARCH_HISTORY = stringPreferencesKey("search_history")


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
    val DEFAULT_CHAT_MESSAGE_CORNER_RADIUS = 16
    val DEFAULT_CHAT_LIST_LAYOUT = com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout.DOUBLE_LINE
    val DEFAULT_CHAT_SWIPE_GESTURE = com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture.ARCHIVE
    val DEFAULT_CHAT_MAX_MESSAGE_CHUNK_SIZE = 500
    val DEFAULT_MESSAGE_SUGGESTION_ENABLED = false
    val DEFAULT_CHAT_AVATAR_DISABLED = false
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

    val DEFAULT_AUTO_DOWNLOAD_WIFI_STRINGS = com.synapse.social.studioasinc.ui.settings.MediaType.values().map { it.name }.toSet()
    val DEFAULT_AUTO_DOWNLOAD_WIFI_TYPES = com.synapse.social.studioasinc.ui.settings.MediaType.values().toSet()
}
