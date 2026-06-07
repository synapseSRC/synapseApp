package com.synapse.social.studioasinc.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.synapse.social.studioasinc.R



@Composable
internal fun PrivacyCheckupSection(isLoading: Boolean) {
    SettingsSection(title = stringResource(R.string.settings_privacy_checkup_section)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_privacy_checkup_section),
            subtitle = stringResource(R.string.settings_privacy_settings_subtitle),
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
    SettingsSection(title = stringResource(R.string.settings_profile_privacy_section)) {
        SettingsSelectionItem(
            title = stringResource(R.string.settings_last_seen_title),
            subtitle = stringResource(R.string.settings_last_seen_subtitle),
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
            title = stringResource(R.string.settings_profile_photo_title),
            subtitle = stringResource(R.string.settings_profile_photo_subtitle),
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
            title = stringResource(R.string.settings_about_title),
            subtitle = stringResource(R.string.settings_about_subtitle),
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
            title = stringResource(R.string.settings_status_title),
            subtitle = stringResource(R.string.settings_status_subtitle),
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
    SettingsSection(title = stringResource(R.string.settings_message_privacy_section)) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_read_receipts_title),
            subtitle = stringResource(R.string.settings_read_receipts_subtitle),
            imageVector = Icons.Filled.DoneAll,
            checked = readReceiptsEnabled,
            onCheckedChange = onReadReceiptsChanged,
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = stringResource(R.string.settings_disappearing_messages_title),
            subtitle = stringResource(R.string.settings_disappearing_messages_subtitle),
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
    SettingsSection(title = stringResource(R.string.settings_group_privacy_section)) {
        SettingsSelectionItem(
            title = stringResource(R.string.settings_groups_title),
            subtitle = stringResource(R.string.settings_groups_subtitle),
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
    SettingsSection(title = stringResource(R.string.settings_security_section)) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_app_lock_title),
            subtitle = stringResource(R.string.settings_app_lock_subtitle),
            imageVector = Icons.Filled.Lock,
            checked = appLockEnabled,
            onCheckedChange = onAppLockChanged,
            enabled = !isLoading
        )
        SettingsDivider()
        SettingsToggleItem(
            title = stringResource(R.string.settings_chat_lock_title),
            subtitle = stringResource(R.string.settings_chat_lock_subtitle),
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
    SettingsSection(title = stringResource(R.string.settings_active_sessions_title)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_active_sessions_title),
            subtitle = stringResource(R.string.settings_active_sessions_subtitle),
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
