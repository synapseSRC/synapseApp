package com.synapse.social.studioasinc.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.synapse.social.studioasinc.R



@Composable
internal fun PrivacyCheckupSection(
    isLoading: Boolean,
    onNavigateToPrivacyCheckup: () -> Unit = {}
) {
    SettingsSection(title = stringResource(R.string.settings_privacy_checkup_section)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_privacy_checkup_section),
            subtitle = stringResource(R.string.settings_privacy_settings_subtitle),
            imageVector = Icons.Filled.Security,
            onClick = onNavigateToPrivacyCheckup,
            enabled = !isLoading
        )
    }
}



@Composable
internal fun ProfilePrivacySection(
    privacySettings: PrivacySettings,
    isLoading: Boolean,
    onNavigateToLastSeen: () -> Unit,
    onNavigateToProfilePhoto: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToStatus: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_profile_privacy_section)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_last_seen_title),
            subtitle = stringResource(R.string.settings_last_seen_subtitle),
            imageVector = Icons.Filled.Visibility,
            onClick = onNavigateToLastSeen,
            enabled = !isLoading,
            position = SettingsItemPosition.Top
        )
        SettingsNavigationItem(
            title = stringResource(R.string.settings_profile_photo_title),
            subtitle = stringResource(R.string.settings_profile_photo_subtitle),
            imageVector = Icons.Filled.Person,
            onClick = onNavigateToProfilePhoto,
            enabled = !isLoading,
            position = SettingsItemPosition.Middle
        )
        SettingsNavigationItem(
            title = stringResource(R.string.settings_about_privacy_title),
            subtitle = stringResource(R.string.settings_about_subtitle),
            imageVector = Icons.Filled.Info,
            onClick = onNavigateToAbout,
            enabled = !isLoading,
            position = SettingsItemPosition.Middle
        )
        SettingsNavigationItem(
            title = stringResource(R.string.settings_status_title),
            subtitle = stringResource(R.string.settings_status_subtitle),
            imageVector = Icons.Filled.Circle,
            onClick = onNavigateToStatus,
            enabled = !isLoading,
            position = SettingsItemPosition.Bottom
        )
    }
}



@Composable
internal fun MessagePrivacySection(
    readReceiptsEnabled: Boolean,
    onReadReceiptsChanged: (Boolean) -> Unit,
    onNavigateToDisappearingMessages: () -> Unit = {},
    isLoading: Boolean
) {
    SettingsSection(title = stringResource(R.string.settings_message_privacy_section)) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_read_receipts_title),
            subtitle = stringResource(R.string.settings_read_receipts_subtitle),
            imageVector = Icons.Filled.DoneAll,
            checked = readReceiptsEnabled,
            onCheckedChange = onReadReceiptsChanged,
            enabled = !isLoading,
            position = SettingsItemPosition.Top
        )
        SettingsNavigationItem(
            title = stringResource(R.string.settings_disappearing_messages_title),
            subtitle = stringResource(R.string.settings_disappearing_messages_subtitle),
            imageVector = Icons.Filled.Timer,
            onClick = onNavigateToDisappearingMessages,
            enabled = !isLoading,
            position = SettingsItemPosition.Bottom
        )
    }
}



@Composable
internal fun GroupPrivacySection(
    privacySettings: PrivacySettings,
    isLoading: Boolean,
    onNavigateToGroups: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_group_privacy_section)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_groups_title),
            subtitle = privacySettings.groupPrivacy.displayName(),
            imageVector = Icons.Filled.Group,
            onClick = onNavigateToGroups,
            enabled = !isLoading
        )
    }
}



@Composable
internal fun SecuritySection(
    appLockEnabled: Boolean,
    onNavigateToAppLock: () -> Unit,
    chatLockEnabled: Boolean,
    onChatLockChanged: (Boolean) -> Unit,
    isLoading: Boolean
) {
    SettingsSection(title = stringResource(R.string.settings_security_section)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_app_lock_title),
            subtitle = if (appLockEnabled) {
                stringResource(R.string.settings_app_lock_enabled)
            } else {
                stringResource(R.string.settings_app_lock_disabled)
            },
            imageVector = Icons.Filled.Lock,
            onClick = onNavigateToAppLock,
            enabled = !isLoading,
            position = SettingsItemPosition.Top
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_chat_lock_title),
            subtitle = stringResource(R.string.settings_chat_lock_subtitle),
            imageVector = Icons.Filled.Lock,
            checked = chatLockEnabled,
            onCheckedChange = onChatLockChanged,
            enabled = !isLoading,
            position = SettingsItemPosition.Bottom
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
            enabled = !isLoading,
            position = SettingsItemPosition.Top
        )
        SettingsNavigationItem(
            title = stringResource(R.string.privacy_muted_users),
            subtitle = stringResource(R.string.privacy_muted_users_subtitle),
            imageVector = Icons.Filled.Notifications,
            onClick = onNavigateToMutedUsers,
            enabled = !isLoading,
            position = SettingsItemPosition.Bottom
        )
    }
}
