package com.synapse.social.studioasinc.ui.settings

import androidx.compose.runtime.Composable
import com.synapse.social.studioasinc.R



@Composable
internal fun PrivacyCheckupSection(isLoading: Boolean) {
    SettingsSection(title = "Privacy Checkup") {
        SettingsNavigationItem(
            title = "Privacy Checkup",
            subtitle = "Review your privacy settings",
            icon = R.drawable.ic_security,
            onClick = { },
            enabled = !isLoading
        )
    }
}



@Composable
internal fun ProfilePrivacySection(
    privacySettings: PrivacySettings,
    isLoading: Boolean,
    onProfileVisibilityChanged: (ProfileVisibility) -> Unit,
    onContentVisibilityChanged: (ContentVisibility) -> Unit
) {
    SettingsSection(title = "Profile Privacy") {
        SettingsSelectionItem(
            title = "Last Seen",
            subtitle = "Control who can see when you were last online",
            icon = R.drawable.ic_visibility,
            options = ProfileVisibility.values().map { it.displayName() },
            selectedOption = privacySettings.profileVisibility.displayName(),
            onSelect = { option ->
                val visibility = ProfileVisibility.values().find { it.displayName() == option }
                if (visibility != null) {
                    onProfileVisibilityChanged(visibility)
                }
            },
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsSelectionItem(
            title = "Profile Photo",
            subtitle = "Control who can see your profile photo",
            icon = R.drawable.ic_person,
            options = ProfileVisibility.values().map { it.displayName() },
            selectedOption = privacySettings.profileVisibility.displayName(),
            onSelect = { option ->
                val visibility = ProfileVisibility.values().find { it.displayName() == option }
                if (visibility != null) {
                    onProfileVisibilityChanged(visibility)
                }
            },
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsSelectionItem(
            title = "About",
            subtitle = "Control who can see your about info",
            icon = R.drawable.ic_info,
            options = ProfileVisibility.values().map { it.displayName() },
            selectedOption = privacySettings.profileVisibility.displayName(),
            onSelect = { option ->
                 val visibility = ProfileVisibility.values().find { it.displayName() == option }
                if (visibility != null) {
                    onProfileVisibilityChanged(visibility)
                }
            },
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsSelectionItem(
            title = "Status",
            subtitle = "Control who can see your status updates",
            icon = R.drawable.ic_status,
            options = ContentVisibility.values().map { it.displayName() },
            selectedOption = privacySettings.contentVisibility.displayName(),
            onSelect = { option ->
                val visibility = ContentVisibility.values().find { it.displayName() == option }
                if (visibility != null) {
                    onContentVisibilityChanged(visibility)
                }
            },
            enabled = !isLoading
        )
    }
}



@Composable
internal fun MessagePrivacySection(
    readReceiptsEnabled: Boolean,
    onReadReceiptsChanged: (Boolean) -> Unit,
    isLoading: Boolean
) {
    SettingsSection(title = "Message Privacy") {
        SettingsToggleItem(
            title = "Read Receipts",
            subtitle = "Show when you've read messages",
            icon = R.drawable.ic_done_all,
            checked = readReceiptsEnabled,
            onCheckedChange = onReadReceiptsChanged,
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = "Disappearing Messages",
            subtitle = "Set default timer for new chats",
            icon = R.drawable.ic_timer,
            onClick = { },
            enabled = !isLoading
        )
    }
}



@Composable
internal fun GroupPrivacySection(
    privacySettings: PrivacySettings,
    isLoading: Boolean,
    onGroupPrivacyChanged: (GroupPrivacy) -> Unit
) {
    SettingsSection(title = "Group Privacy") {
        SettingsSelectionItem(
            title = "Groups",
            subtitle = "Control who can add you to groups",
            icon = R.drawable.ic_group,
            options = GroupPrivacy.values().map { it.displayName() },
            selectedOption = privacySettings.groupPrivacy.displayName(),
            onSelect = { option ->
                val privacy = GroupPrivacy.values().find { it.displayName() == option }
                if (privacy != null) {
                    onGroupPrivacyChanged(privacy)
                }
            },
            enabled = !isLoading
        )
    }
}



@Composable
internal fun SecuritySection(
    appLockEnabled: Boolean,
    onAppLockChanged: (Boolean) -> Unit,
    chatLockEnabled: Boolean,
    onChatLockChanged: (Boolean) -> Unit,
    isLoading: Boolean
) {
    SettingsSection(title = "Security") {
        SettingsToggleItem(
            title = "App Lock",
            subtitle = "Require authentication to open app",
            icon = R.drawable.ic_lock,
            checked = appLockEnabled,
            onCheckedChange = onAppLockChanged,
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsToggleItem(
            title = "Chat Lock",
            subtitle = "Lock individual chats with authentication",
            icon = R.drawable.ic_chat_lock,
            checked = chatLockEnabled,
            onCheckedChange = onChatLockChanged,
            enabled = !isLoading
        )
    }
}



@Composable
internal fun ActiveSessionsSection(
    onNavigateToActiveSessions: () -> Unit,
    isLoading: Boolean
) {
    SettingsSection(title = "Active Sessions") {
        SettingsNavigationItem(
            title = "Active Sessions",
            subtitle = "Manage your active sessions",
            icon = R.drawable.ic_key,
            onClick = onNavigateToActiveSessions,
            enabled = !isLoading
        )
    }
}



@Composable
internal fun ContactsSection(
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToMutedUsers: () -> Unit,
    isLoading: Boolean
) {
    SettingsSection(title = "Contacts") {
        SettingsNavigationItem(
            title = "Blocked Contacts",
            subtitle = "Manage blocked users",
            icon = R.drawable.ic_block,
            onClick = onNavigateToBlockedUsers,
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = "Muted Users",
            subtitle = "Manage muted users",
            icon = R.drawable.ic_notifications,
            onClick = onNavigateToMutedUsers,
            enabled = !isLoading
        )
    }
}
