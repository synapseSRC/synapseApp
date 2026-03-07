package com.synapse.social.studioasinc.ui.settings

import com.synapse.social.studioasinc.R

object SettingsDataProvider {
    fun getSettingsGroups(): List<SettingsGroup> {
        val groupA = SettingsGroup(
            id = "group_a",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "synapse_plus",
                    title = "Synapse Plus",
                    subtitle = "Show that your profile is verified",
                    icon = R.drawable.ic_verified,
                    destination = SettingsDestination.SynapsePlus,
                    keywords = listOf("premium", "verified", "badge", "subscription", "plus")
                ),
                SettingsCategory(
                    id = "account",
                    title = "Account",
                    subtitle = "Security notifications, change number",
                    icon = R.drawable.ic_person,
                    destination = SettingsDestination.Account,
                    keywords = listOf("security", "password", "email", "number", "profile", "delete")
                ),
                SettingsCategory(
                    id = "avatar",
                    title = "Avatar",
                    subtitle = "Create, edit, profile photo",
                    icon = R.drawable.ic_face,
                    destination = SettingsDestination.Avatar,
                    keywords = listOf("photo", "picture", "image", "profile")
                )
            )
        )

        val groupB = SettingsGroup(
            id = "group_b",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "privacy",
                    title = "Privacy",
                    subtitle = "Block contacts, disappearing messages",
                    icon = R.drawable.ic_shield_lock,
                    destination = SettingsDestination.Privacy,
                    keywords = listOf("block", "hide", "status", "last seen", "read receipts", "disappearing")
                ),
                SettingsCategory(
                    id = "favourites",
                    title = "Favourites",
                    subtitle = "Add, reorder, remove",
                    icon = R.drawable.ic_favorite,
                    destination = SettingsDestination.Favourites,
                    keywords = listOf("star", "bookmark", "save", "top")
                ),
                SettingsCategory(
                    id = "appearance",
                    title = "Appearance",
                    subtitle = "Theme, wallpapers, font size",
                    icon = R.drawable.ic_palette,
                    destination = SettingsDestination.Appearance,
                    keywords = listOf("theme", "dark mode", "light mode", "wallpaper", "font", "size", "colors")
                )
            )
        )

        val groupC = SettingsGroup(
            id = "group_c",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "notifications",
                    title = "Notifications",
                    subtitle = "Message, group & call tones",
                    icon = R.drawable.ic_notifications,
                    destination = SettingsDestination.Notifications,
                    keywords = listOf("sound", "tone", "ringtone", "alert", "vibrate", "badge", "mute")
                ),
                SettingsCategory(
                    id = "storage",
                    title = "Storage and Data",
                    subtitle = "Network usage, auto-download",
                    icon = R.drawable.data_usage_24px,
                    destination = SettingsDestination.Storage,
                    keywords = listOf("data", "usage", "download", "network", "cache", "space", "memory")
                )
            )
        )

        val groupD = SettingsGroup(
            id = "group_d",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "accessibility",
                    title = "Accessibility",
                    subtitle = "Increase contrast, animation",
                    icon = R.drawable.ic_accessibility,
                    destination = SettingsDestination.Accessibility,
                    keywords = listOf("contrast", "animation", "text", "vision", "hearing")
                ),
                SettingsCategory(
                    id = "language",
                    title = "App Language",
                    subtitle = "Language selection (e.g., English)",
                    icon = R.drawable.ic_public,
                    destination = SettingsDestination.Language,
                    keywords = listOf("language", "locale", "translate", "english", "spanish")
                ),
                SettingsCategory(
                    id = "about",
                    title = "About App",
                    subtitle = "Help center, contact us, privacy policy",
                    icon = R.drawable.ic_info_48px,
                    destination = SettingsDestination.About,
                    keywords = listOf("help", "contact", "support", "privacy policy", "terms", "version")
                )
            )
        )

        val groupE = SettingsGroup(
            id = "group_e",
            title = "Experiments",
            categories = listOf(
                SettingsCategory(
                    id = "storage_provider",
                    title = "Storage Providers",
                    subtitle = "Configure Cloudflare, Cloudinary, Supabase",
                    icon = R.drawable.ic_cloud_upload,
                    destination = SettingsDestination.StorageProvider,
                    keywords = listOf("cloud", "provider", "cloudflare", "cloudinary", "supabase")
                ),
                SettingsCategory(
                    id = "artificial_intelligence",
                    title = "Artificial Intelligence",
                    subtitle = "Configure AI providers and API keys",
                    icon = R.drawable.ic_ai_summary,
                    destination = SettingsDestination.ApiKey,
                    keywords = listOf("ai", "artificial intelligence", "api key", "openai", "gemini", "claude")
                )
            )
        )

        return listOf(groupA, groupB, groupC, groupD, groupE)
    }
}
