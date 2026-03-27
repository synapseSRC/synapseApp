package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    @SerialName("user_id") val userId: String,
    @SerialName("security_notifications_enabled") val securityNotificationsEnabled: Boolean,
    @SerialName("chat_font_scale") val chatFontScale: Float? = null,
    @SerialName("chat_message_corner_radius") val chatMessageCornerRadius: Int? = null,
    @SerialName("chat_theme_preset") val chatThemePreset: String? = null,
    @SerialName("chat_wallpaper_type") val chatWallpaperType: String? = null,
    @SerialName("chat_wallpaper_value") val chatWallpaperValue: String? = null,
    @SerialName("chat_wallpaper_blur") val chatWallpaperBlur: Float? = null,
    @SerialName("chat_list_layout") val chatListLayout: String? = null,
    @SerialName("chat_swipe_gesture") val chatSwipeGesture: String? = null
)
