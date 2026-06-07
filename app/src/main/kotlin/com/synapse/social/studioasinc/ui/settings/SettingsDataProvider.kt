package com.synapse.social.studioasinc.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

object SettingsDataProvider {
    fun getSettingsGroups(): List<SettingsGroup> {
        return listOf(getGroupA(), getGroupB(), getGroupC(), getGroupD(), getGroupE())
    }

    private fun getGroupA(): SettingsGroup {
        return SettingsGroup(
            id = "group_a",
            title = stringResource(R.string.settings_account_group_title),
            categories = listOf(

                SettingsCategory(
                    id = "synapse_plus",
                    title = stringResource(R.string.settings_synapse_plus_title),
                    subtitle = stringResource(R.string.settings_synapse_plus_subtitle),
                    icon = Icons.Filled.Verified,
                    destination = SettingsDestination.SynapsePlus,
                    keywords = listOf("premium", "verified", "badge", "subscription", "plus")
                ),
                SettingsCategory(
                    id = "account",
                    title = stringResource(R.string.settings_account_settings_title),
                    subtitle = stringResource(R.string.settings_account_settings_subtitle),
                    icon = Icons.Filled.Person,
                    destination = SettingsDestination.Account,
                    keywords = listOf("security", "password", "email", "number", "profile", "delete")
                ),
                SettingsCategory(
                    id = "avatar",
                    title = stringResource(R.string.settings_avatar_title),
                    subtitle = stringResource(R.string.settings_avatar_subtitle),
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
            title = stringResource(R.string.settings_preferences_group_title),
            categories = listOf(
                SettingsCategory(
                    id = "privacy",
                    title = stringResource(R.string.settings_privacy_settings_title),
                    subtitle = stringResource(R.string.settings_privacy_settings_subtitle),
                    icon = Icons.Filled.Shield,
                    destination = SettingsDestination.Privacy,
                    keywords = listOf("block", "hide", "status", "last seen", "read receipts", "disappearing")
                ),
                SettingsCategory(
                    id = "favourites",
                    title = stringResource(R.string.settings_favourites_title),
                    subtitle = stringResource(R.string.settings_favourites_subtitle),
                    icon = Icons.Filled.Favorite,
                    destination = SettingsDestination.Favourites,
                    keywords = listOf("star", "bookmark", "save", "top")
                ),
                SettingsCategory(
                    id = "appearance",
                    title = stringResource(R.string.settings_appearance_settings_title),
                    subtitle = stringResource(R.string.settings_appearance_settings_subtitle),
                    icon = Icons.Filled.Palette,
                    destination = SettingsDestination.Appearance,
                    keywords = listOf("theme", "dark mode", "light mode", "wallpaper", "font", "size", "colors")
                ),
                SettingsCategory(
                    id = "chat_settings",
                    title = stringResource(R.string.settings_chat_settings_title),
                    subtitle = stringResource(R.string.settings_chat_settings_subtitle),
                    icon = Icons.Filled.Message,
                    destination = SettingsDestination.ChatSettings,
                    keywords = listOf("chat", "message", "theme", "wallpaper", "bubble", "swipe", "layout")
                ),
                SettingsCategory(
                    id = "chat_folders",
                    title = stringResource(R.string.settings_chat_folders_title),
                    subtitle = stringResource(R.string.settings_chat_folders_subtitle),
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
            title = stringResource(R.string.settings_notifications_storage_group_title),
            categories = listOf(
                SettingsCategory(
                    id = "notifications",
                    title = stringResource(R.string.settings_notifications_title_new),
                    subtitle = stringResource(R.string.settings_notifications_subtitle),
                    icon = Icons.Filled.Notifications,
                    destination = SettingsDestination.Notifications,
                    keywords = listOf("sound", "tone", "ringtone", "alert", "vibrate", "badge", "mute")
                ),
                SettingsCategory(
                    id = "storage",
                    title = stringResource(R.string.settings_storage_title),
                    subtitle = stringResource(R.string.settings_storage_subtitle),
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
            title = stringResource(R.string.settings_general_group_title),
            categories = listOf(
                SettingsCategory(
                    id = "accessibility",
                    title = stringResource(R.string.settings_accessibility_title_new),
                    subtitle = stringResource(R.string.settings_accessibility_subtitle),
                    icon = Icons.Filled.Accessibility,
                    destination = SettingsDestination.Accessibility,
                    keywords = listOf("contrast", "animation", "text", "vision", "hearing")
                ),
                SettingsCategory(
                    id = "language",
                    title = stringResource(R.string.settings_app_language_title),
                    subtitle = stringResource(R.string.settings_app_language_subtitle),
                    icon = Icons.Filled.Language,
                    destination = SettingsDestination.Language,
                    keywords = listOf("language", "locale", "translate", "english", "spanish")
                ),
                SettingsCategory(
                    id = "about",
                    title = stringResource(R.string.settings_about_app_title),
                    subtitle = stringResource(R.string.settings_about_app_subtitle),
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
            title = stringResource(R.string.settings_experiments_group_title),
            categories = listOf(
                SettingsCategory(
                    id = "flags",
                    title = stringResource(R.string.settings_flags_title),
                    subtitle = stringResource(R.string.settings_flags_subtitle),
                    icon = Icons.Filled.Build,
                    destination = SettingsDestination.Flags,
                    keywords = listOf("flags", "experimental", "beta", "alpha", "features")
                ),
                SettingsCategory(
                    id = "storage_provider",
                    title = stringResource(R.string.settings_storage_providers_title),
                    subtitle = stringResource(R.string.settings_storage_providers_subtitle),
                    icon = Icons.Filled.CloudUpload,
                    destination = SettingsDestination.StorageProvider,
                    keywords = listOf("cloud", "provider", "cloudflare", "cloudinary", "supabase")
                ),
                SettingsCategory(
                    id = "artificial_intelligence",
                    title = stringResource(R.string.settings_artificial_intelligence_title),
                    subtitle = stringResource(R.string.settings_artificial_intelligence_subtitle),
                    icon = Icons.Filled.AutoAwesome,
                    destination = SettingsDestination.ApiKey,
                    keywords = listOf("ai", "artificial intelligence", "api key", "openai", "gemini", "claude")
                )
            )
        )
    }
}
