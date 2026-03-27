package com.synapse.social.studioasinc.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.synapse.social.studioasinc.R



@Composable
internal fun PrivacyCheckupSection(isLoading: Boolean) {
    SettingsSection(title = "Privacy Checkup") {
        SettingsNavigationItem(
            title = "Privacy Checkup",
            subtitle = "Review your privacy settings",
            imageVector = Icons.Filled.Security,
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
            icon = Icons.Filled.Visibility,
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
            icon = Icons.Filled.Person,
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
            icon = Icons.Filled.Info,
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
            icon = Icons.Filled.Circle,
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
            imageVector = Icons.Filled.DoneAll,
            checked = readReceiptsEnabled,
            onCheckedChange = onReadReceiptsChanged,
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = "Disappearing Messages",
            subtitle = "Set default timer for new chats",
            imageVector = Icons.Filled.Timer,
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
            icon = Icons.Filled.Group,
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
            imageVector = Icons.Filled.Lock,
            checked = appLockEnabled,
            onCheckedChange = onAppLockChanged,
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsToggleItem(
            title = "Chat Lock",
            subtitle = "Lock individual chats with authentication",
            imageVector = Icons.Filled.Lock,
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
            imageVector = Icons.Filled.Key,
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
    SettingsSection(title = stringResource(R.string.privacy_section_blocking)) {
        SettingsNavigationItem(
            title = stringResource(R.string.blocked_contacts),
            subtitle = stringResource(R.string.privacy_blocked_users_subtitle),
            imageVector = Icons.Filled.Block,
            onClick = onNavigateToBlockedUsers,
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = stringResource(R.string.privacy_muted_users),
            subtitle = stringResource(R.string.privacy_muted_users_subtitle),
            imageVector = Icons.Filled.Notifications,
            onClick = onNavigateToMutedUsers,
            enabled = !isLoading
        )
    }
}
