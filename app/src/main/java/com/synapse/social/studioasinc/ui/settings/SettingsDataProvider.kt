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
            title = null,
            categories = listOf(

                SettingsCategory(
                    id = "synapse_plus",
                    title = "Synapse Plus",
                    subtitle = "Show that your profile is verified",
                    icon = Icons.Filled.Verified,
                    destination = SettingsDestination.SynapsePlus,
                    keywords = listOf("premium", "verified", "badge", "subscription", "plus")
                ),
                SettingsCategory(
                    id = "account",
                    title = "Account",
                    subtitle = "Security notifications, change number",
                    icon = Icons.Filled.Person,
                    destination = SettingsDestination.Account,
                    keywords = listOf("security", "password", "email", "number", "profile", "delete")
                ),
                SettingsCategory(
                    id = "avatar",
                    title = "Avatar",
                    subtitle = "Create, edit, profile photo",
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
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "privacy",
                    title = "Privacy",
                    subtitle = "Block contacts, disappearing messages",
                    icon = Icons.Filled.Shield,
                    destination = SettingsDestination.Privacy,
                    keywords = listOf("block", "hide", "status", "last seen", "read receipts", "disappearing")
                ),
                SettingsCategory(
                    id = "favourites",
                    title = "Favourites",
                    subtitle = "Add, reorder, remove",
                    icon = Icons.Filled.Favorite,
                    destination = SettingsDestination.Favourites,
                    keywords = listOf("star", "bookmark", "save", "top")
                ),
                SettingsCategory(
                    id = "appearance",
                    title = "Appearance",
                    subtitle = "Theme, wallpapers, font size",
                    icon = Icons.Filled.Palette,
                    destination = SettingsDestination.Appearance,
                    keywords = listOf("theme", "dark mode", "light mode", "wallpaper", "font", "size", "colors")
                ),
                SettingsCategory(
                    id = "chat_settings",
                    title = "Chat Settings",
                    subtitle = "Chat theme, wallpaper, list layout, corner radius",
                    icon = Icons.Filled.Message,
                    destination = SettingsDestination.ChatSettings,
                    keywords = listOf("chat", "message", "theme", "wallpaper", "bubble", "swipe", "layout")
                ),
                SettingsCategory(
                    id = "chat_folders",
                    title = "Chat Folders",
                    subtitle = "Create and manage chat folders",
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
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "notifications",
                    title = "Notifications",
                    subtitle = "Message, group & call tones",
                    icon = Icons.Filled.Notifications,
                    destination = SettingsDestination.Notifications,
                    keywords = listOf("sound", "tone", "ringtone", "alert", "vibrate", "badge", "mute")
                ),
                SettingsCategory(
                    id = "storage",
                    title = "Storage and Data",
                    subtitle = "Network usage, auto-download",
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
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "accessibility",
                    title = "Accessibility",
                    subtitle = "Increase contrast, animation",
                    icon = Icons.Filled.Accessibility,
                    destination = SettingsDestination.Accessibility,
                    keywords = listOf("contrast", "animation", "text", "vision", "hearing")
                ),
                SettingsCategory(
                    id = "language",
                    title = "App Language",
                    subtitle = "Language selection (e.g., English)",
                    icon = Icons.Filled.Language,
                    destination = SettingsDestination.Language,
                    keywords = listOf("language", "locale", "translate", "english", "spanish")
                ),
                SettingsCategory(
                    id = "about",
                    title = "About App",
                    subtitle = "Help center, contact us, privacy policy",
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
            title = "Experiments",
            categories = listOf(
                SettingsCategory(
                    id = "flags",
                    title = "Flags",
                    subtitle = "Toggle experimental features",
                    icon = Icons.Filled.Build,
                    destination = SettingsDestination.Flags,
                    keywords = listOf("flags", "experimental", "beta", "alpha", "features")
                ),
                SettingsCategory(
                    id = "storage_provider",
                    title = "Storage Providers",
                    subtitle = "Configure Cloudflare, Cloudinary, Supabase",
                    icon = Icons.Filled.CloudUpload,
                    destination = SettingsDestination.StorageProvider,
                    keywords = listOf("cloud", "provider", "cloudflare", "cloudinary", "supabase")
                ),
                SettingsCategory(
                    id = "artificial_intelligence",
                    title = "Artificial Intelligence",
                    subtitle = "Configure AI providers and API keys",
                    icon = Icons.Filled.AutoAwesome,
                    destination = SettingsDestination.ApiKey,
                    keywords = listOf("ai", "artificial intelligence", "api key", "openai", "gemini", "claude")
                )
            )
        )
    }
}
