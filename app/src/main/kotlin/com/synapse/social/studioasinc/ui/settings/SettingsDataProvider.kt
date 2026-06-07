package com.synapse.social.studioasinc.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.UiText

object SettingsDataProvider {
    fun getSettingsGroups(): List<SettingsGroup> {
        return listOf(getGroupA(), getGroupB(), getGroupC(), getGroupD(), getGroupE())
    }

    private fun getGroupA(): SettingsGroup {
        return SettingsGroup(
            id = "group_a",
            title = UiText.StringResource(R.string.settings_account_group_title),
            categories = listOf(
                SettingsCategory(
                    id = "synapse_plus",
                    title = UiText.StringResource(R.string.settings_synapse_plus_title),
                    subtitle = UiText.StringResource(R.string.settings_synapse_plus_subtitle),
                    icon = Icons.Filled.Verified,
                    destination = SettingsDestination.SynapsePlus,
                    keywords = listOf("premium", "verified", "badge", "subscription", "plus")
                ),
                SettingsCategory(
                    id = "account",
                    title = UiText.StringResource(R.string.settings_account_settings_title),
                    subtitle = UiText.StringResource(R.string.settings_account_settings_subtitle),
                    icon = Icons.Filled.Person,
                    destination = SettingsDestination.Account,
                    keywords = listOf("security", "password", "email", "number", "profile", "delete")
                ),
                SettingsCategory(
                    id = "avatar",
                    title = UiText.StringResource(R.string.settings_avatar_title),
                    subtitle = UiText.StringResource(R.string.settings_avatar_subtitle),
                    icon = Icons.Filled.Face,
                    destination = SettingsDestination.Avatar,
                    keywords = listOf("photo", "picture", "image", "profile")
                )
            )
        )
    }

    private fun getGroupB(): SettingsGroup {
        return SettingsGroup(
            id = "group_b",
            title = UiText.StringResource(R.string.settings_preferences_group_title),
            categories = listOf(
                SettingsCategory(
                    id = "privacy",
                    title = UiText.StringResource(R.string.settings_privacy_settings_title),
                    subtitle = UiText.StringResource(R.string.settings_privacy_settings_subtitle),
                    icon = Icons.Filled.Shield,
                    destination = SettingsDestination.Privacy,
                    keywords = listOf("block", "hide", "status", "last seen", "read receipts", "disappearing")
                ),
                SettingsCategory(
                    id = "favourites",
                    title = UiText.StringResource(R.string.settings_favourites_title),
                    subtitle = UiText.StringResource(R.string.settings_favourites_subtitle),
                    icon = Icons.Filled.Favorite,
                    destination = SettingsDestination.Favourites,
                    keywords = listOf("star", "bookmark", "save", "top")
                ),
                SettingsCategory(
                    id = "appearance",
                    title = UiText.StringResource(R.string.settings_appearance_settings_title),
                    subtitle = UiText.StringResource(R.string.settings_appearance_settings_subtitle),
                    icon = Icons.Filled.Palette,
                    destination = SettingsDestination.Appearance,
                    keywords = listOf("theme", "dark mode", "light mode", "wallpaper", "font", "size", "colors")
                ),
                SettingsCategory(
                    id = "chat_settings",
                    title = UiText.StringResource(R.string.settings_chat_settings_title),
                    subtitle = UiText.StringResource(R.string.settings_chat_settings_subtitle),
                    icon = Icons.Filled.Message,
                    destination = SettingsDestination.ChatSettings,
                    keywords = listOf("chat", "message", "theme", "wallpaper", "bubble", "swipe", "layout")
                ),
                SettingsCategory(
                    id = "chat_folders",
                    title = UiText.StringResource(R.string.settings_chat_folders_title),
                    subtitle = UiText.StringResource(R.string.settings_chat_folders_subtitle),
                    icon = Icons.Filled.Folder,
                    destination = SettingsDestination.ChatFolders,
                    keywords = listOf("chat", "folder", "organize", "tabs")
                )
            )
        )
    }

    private fun getGroupC(): SettingsGroup {
        return SettingsGroup(
            id = "group_c",
            title = UiText.StringResource(R.string.settings_notifications_storage_group_title),
            categories = listOf(
                SettingsCategory(
                    id = "notifications",
                    title = UiText.StringResource(R.string.settings_notifications_title_new),
                    subtitle = UiText.StringResource(R.string.settings_notifications_subtitle_new),
                    icon = Icons.Filled.Notifications,
                    destination = SettingsDestination.Notifications,
                    keywords = listOf("sound", "tone", "ringtone", "alert", "vibrate", "badge", "mute")
                ),
                SettingsCategory(
                    id = "storage",
                    title = UiText.StringResource(R.string.settings_storage_title),
                    subtitle = UiText.StringResource(R.string.settings_storage_subtitle),
                    icon = Icons.Filled.Storage,
                    destination = SettingsDestination.Storage,
                    keywords = listOf("data", "usage", "download", "network", "cache", "space", "memory")
                )
            )
        )
    }

    private fun getGroupD(): SettingsGroup {
        return SettingsGroup(
            id = "group_d",
            title = UiText.StringResource(R.string.settings_general_group_title),
            categories = listOf(
                SettingsCategory(
                    id = "accessibility",
                    title = UiText.StringResource(R.string.settings_accessibility_title_new),
                    subtitle = UiText.StringResource(R.string.settings_accessibility_subtitle),
                    icon = Icons.Filled.Accessibility,
                    destination = SettingsDestination.Accessibility,
                    keywords = listOf("contrast", "animation", "text", "vision", "hearing")
                ),
                SettingsCategory(
                    id = "language",
                    title = UiText.StringResource(R.string.settings_app_language_title),
                    subtitle = UiText.StringResource(R.string.settings_app_language_subtitle),
                    icon = Icons.Filled.Language,
                    destination = SettingsDestination.Language,
                    keywords = listOf("language", "locale", "translate", "english", "spanish")
                ),
                SettingsCategory(
                    id = "about",
                    title = UiText.StringResource(R.string.settings_about_app_title),
                    subtitle = UiText.StringResource(R.string.settings_about_app_subtitle),
                    icon = Icons.Filled.Info,
                    destination = SettingsDestination.About,
                    keywords = listOf("help", "contact", "support", "privacy policy", "terms", "version")
                )
            )
        )
    }

    private fun getGroupE(): SettingsGroup {
        return SettingsGroup(
            id = "group_e",
            title = UiText.StringResource(R.string.settings_experiments_group_title),
            categories = listOf(
                SettingsCategory(
                    id = "flags",
                    title = UiText.StringResource(R.string.settings_flags_title),
                    subtitle = UiText.StringResource(R.string.settings_flags_subtitle),
                    icon = Icons.Filled.Build,
                    destination = SettingsDestination.Flags,
                    keywords = listOf("flags", "experimental", "beta", "alpha", "features")
                ),
                SettingsCategory(
                    id = "storage_provider",
                    title = UiText.StringResource(R.string.settings_storage_providers_title),
                    subtitle = UiText.StringResource(R.string.settings_storage_providers_subtitle),
                    icon = Icons.Filled.CloudUpload,
                    destination = SettingsDestination.StorageProvider,
                    keywords = listOf("cloud", "provider", "cloudflare", "cloudinary", "supabase")
                ),
                SettingsCategory(
                    id = "artificial_intelligence",
                    title = UiText.StringResource(R.string.settings_artificial_intelligence_title),
                    subtitle = UiText.StringResource(R.string.settings_artificial_intelligence_subtitle),
                    icon = Icons.Filled.AutoAwesome,
                    destination = SettingsDestination.ApiKey,
                    keywords = listOf("ai", "artificial intelligence", "api key", "openai", "gemini", "claude")
                )
            )
        )
    }
}
