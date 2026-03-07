package com.synapse.social.studioasinc.ui.settings

import androidx.annotation.DrawableRes
import com.synapse.social.studioasinc.domain.model.ChatThemePreset
import com.synapse.social.studioasinc.domain.model.ChatWallpaper








enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class FontScale {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE;
    fun scaleFactor(): Float = when (this) {
        SMALL -> 0.85f
        MEDIUM -> 1.0f
        LARGE -> 1.15f
        EXTRA_LARGE -> 1.3f
    }
    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}





enum class ProfileVisibility {
    PUBLIC, FOLLOWERS_ONLY, PRIVATE;
    fun displayName(): String = name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
}

enum class ContentVisibility {
    EVERYONE, FOLLOWERS, ONLY_ME;
    fun displayName(): String = name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
}

enum class GroupPrivacy {
    EVERYONE, MY_CONTACTS, MY_CONTACTS_EXCEPT, NOBODY;
    fun displayName(): String = name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
}





enum class NotificationCategory {
    LIKES, COMMENTS, REPLIES, FOLLOWS, MESSAGES, MENTIONS, NEW_POSTS, SHARES, SYSTEM_UPDATES;
    fun displayName(): String = name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
}





enum class MediaAutoDownload {
    ALWAYS, WIFI_ONLY, NEVER;
    fun displayName(): String = name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
}

enum class MediaUploadQuality {
    STANDARD, HD;
    fun displayName(): String = when (this) {
        STANDARD -> "Standard Quality"
        HD -> "HD Quality"
    }
    fun description(): String = when (this) {
        STANDARD -> "Standard quality files are smaller and use less data."
        HD -> "HD quality files are clearer but use more data and take longer to send."
    }
}

enum class MediaType {
    PHOTO, AUDIO, VIDEO, DOCUMENT;
    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

data class AutoDownloadRules(
    val mobileData: Set<MediaType> = setOf(MediaType.PHOTO),
    val wifi: Set<MediaType> = MediaType.values().toSet(),
    val roaming: Set<MediaType> = emptySet()
)

data class StorageUsageBreakdown(
    val totalSize: Long,
    val usedSize: Long,
    val freeSize: Long,
    val appsAndOtherSize: Long,
    val synapseSize: Long
)

data class LargeFileInfo(
    val fileId: String,
    val fileName: String,
    val size: Long,
    val thumbnailUri: String?,
    val type: MediaType
)





data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val fontScale: FontScale = FontScale.MEDIUM,
    val postViewStyle: PostViewStyle = PostViewStyle.SWIPE
)

enum class PostViewStyle {
    SWIPE, GRID;
    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

data class PrivacySettings(
    val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
    val twoFactorEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val contentVisibility: ContentVisibility = ContentVisibility.EVERYONE,
    val groupPrivacy: GroupPrivacy = GroupPrivacy.EVERYONE,
    val readReceiptsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val chatLockEnabled: Boolean = false
)

data class NotificationPreferences(
    val globalEnabled: Boolean = DEFAULT_GLOBAL_ENABLED,
    val likesEnabled: Boolean = DEFAULT_LIKES_ENABLED,
    val commentsEnabled: Boolean = DEFAULT_COMMENTS_ENABLED,
    val repliesEnabled: Boolean = DEFAULT_REPLIES_ENABLED,
    val followsEnabled: Boolean = DEFAULT_FOLLOWS_ENABLED,
    val mentionsEnabled: Boolean = DEFAULT_MENTIONS_ENABLED,
    val newPostsEnabled: Boolean = DEFAULT_NEW_POSTS_ENABLED,
    val sharesEnabled: Boolean = DEFAULT_SHARES_ENABLED,
    val securityEnabled: Boolean = DEFAULT_SECURITY_ENABLED,
    val updatesEnabled: Boolean = DEFAULT_UPDATES_ENABLED,
    val quietHoursEnabled: Boolean = DEFAULT_QUIET_HOURS_ENABLED,
    val quietHoursStart: String = DEFAULT_QUIET_HOURS_START,
    val quietHoursEnd: String = DEFAULT_QUIET_HOURS_END,
    val doNotDisturb: Boolean = DEFAULT_DO_NOT_DISTURB,
    val dndUntil: String? = null,
    val inAppNotificationsEnabled: Boolean = DEFAULT_IN_APP_ENABLED,
    val remindersEnabled: Boolean = DEFAULT_REMINDERS_ENABLED,
    val highPriorityEnabled: Boolean = DEFAULT_HIGH_PRIORITY_ENABLED,
    val reactionNotificationsEnabled: Boolean = DEFAULT_REACTIONS_ENABLED,
    val messagesEnabled: Boolean = DEFAULT_MESSAGES_ENABLED
) {
    companion object {
        const val DEFAULT_GLOBAL_ENABLED = true
        const val DEFAULT_LIKES_ENABLED = true
        const val DEFAULT_COMMENTS_ENABLED = true
        const val DEFAULT_REPLIES_ENABLED = true
        const val DEFAULT_FOLLOWS_ENABLED = true
        const val DEFAULT_MENTIONS_ENABLED = true
        const val DEFAULT_NEW_POSTS_ENABLED = true
        const val DEFAULT_SHARES_ENABLED = true
        const val DEFAULT_SECURITY_ENABLED = true
        const val DEFAULT_UPDATES_ENABLED = true
        const val DEFAULT_QUIET_HOURS_ENABLED = false
        const val DEFAULT_QUIET_HOURS_START = "22:00"
        const val DEFAULT_QUIET_HOURS_END = "08:00"
        const val DEFAULT_DO_NOT_DISTURB = false
        const val DEFAULT_IN_APP_ENABLED = true
        const val DEFAULT_REMINDERS_ENABLED = false
        const val DEFAULT_HIGH_PRIORITY_ENABLED = true
        const val DEFAULT_REACTIONS_ENABLED = true
        const val DEFAULT_MESSAGES_ENABLED = true
    }

    fun isEnabled(category: NotificationCategory): Boolean = when (category) {
        NotificationCategory.LIKES -> likesEnabled
        NotificationCategory.COMMENTS -> commentsEnabled
        NotificationCategory.REPLIES -> repliesEnabled
        NotificationCategory.FOLLOWS -> followsEnabled
        NotificationCategory.MESSAGES -> messagesEnabled
        NotificationCategory.MENTIONS -> mentionsEnabled
        NotificationCategory.NEW_POSTS -> newPostsEnabled
        NotificationCategory.SHARES -> sharesEnabled
        NotificationCategory.SYSTEM_UPDATES -> updatesEnabled
    }

    fun withCategory(category: NotificationCategory, enabled: Boolean): NotificationPreferences {
        return when (category) {
            NotificationCategory.LIKES -> copy(likesEnabled = enabled)
            NotificationCategory.COMMENTS -> copy(commentsEnabled = enabled)
            NotificationCategory.REPLIES -> copy(repliesEnabled = enabled)
            NotificationCategory.FOLLOWS -> copy(followsEnabled = enabled)
            NotificationCategory.MESSAGES -> copy(messagesEnabled = enabled)
            NotificationCategory.MENTIONS -> copy(mentionsEnabled = enabled)
            NotificationCategory.NEW_POSTS -> copy(newPostsEnabled = enabled)
            NotificationCategory.SHARES -> copy(sharesEnabled = enabled)
            NotificationCategory.SYSTEM_UPDATES -> copy(updatesEnabled = enabled)
        }
    }
}

data class SettingsCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val icon: Int,
    val destination: SettingsDestination,
    val keywords: List<String> = emptyList()
)

data class SettingsGroup(
    val id: String,
    val title: String? = null,
    val categories: List<SettingsCategory>
)

data class UserProfileSummary(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String?
)
