package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.clickable
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.window.Dialog
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

private val SocialNotificationTypes = listOf(
    NotificationCategory.LIKES to R.string.settings_notification_likes_title,
    NotificationCategory.COMMENTS to R.string.settings_notification_comments_title,
    NotificationCategory.REPLIES to R.string.settings_notification_replies_title,
    NotificationCategory.FOLLOWS to R.string.settings_notification_follows_title,
    NotificationCategory.MENTIONS to R.string.settings_notification_mentions_title
)

private val ContentNotificationTypes = listOf(
    NotificationCategory.NEW_POSTS to R.string.settings_notification_new_posts_title,
    NotificationCategory.SHARES to R.string.settings_notification_shares_title
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit
) {
    val notificationPreferences by viewModel.notificationPreferences.collectAsState()
    val error by viewModel.error.collectAsState()

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    NotificationSettingsContent(
        notificationPreferences = notificationPreferences,
        error = error,
        onBackClick = onBackClick,
        onClearError = viewModel::clearError,
        onToggleGlobal = viewModel::toggleGlobalNotifications,
        onToggleCategory = viewModel::toggleNotificationCategory,
        onToggleQuietHours = viewModel::toggleQuietHours,
        onToggleDoNotDisturb = viewModel::toggleDoNotDisturb,
        onShowStartTimePicker = { showStartTimePicker = true }
    )

    if (showStartTimePicker) {
        QuietHoursTimePickerDialog(
            title = stringResource(R.string.settings_quiet_hours_start_title),
            initialTime = notificationPreferences.quietHoursStart,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { time ->
                viewModel.setQuietHours(time, notificationPreferences.quietHoursEnd)
                showStartTimePicker = false
                showEndTimePicker = true
            },
            confirmText = stringResource(R.string.settings_next_button)
        )
    }

    if (showEndTimePicker) {
        QuietHoursTimePickerDialog(
            title = stringResource(R.string.settings_quiet_hours_end_title),
            initialTime = notificationPreferences.quietHoursEnd,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { time ->
                viewModel.setQuietHours(notificationPreferences.quietHoursStart, time)
                showEndTimePicker = false
            },
            confirmText = stringResource(R.string.settings_done_button)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsContent(
    notificationPreferences: NotificationPreferences,
    error: String?,
    onBackClick: () -> Unit,
    onClearError: () -> Unit,
    onToggleGlobal: (Boolean) -> Unit,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onToggleDoNotDisturb: (Boolean) -> Unit,
    onShowStartTimePicker: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = SettingsColors.screenBackground,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier.padding(Spacing.Medium),
                    action = {
                        TextButton(onClick = onClearError) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SettingsSpacing.screenPadding),
            contentPadding = PaddingValues(bottom = Spacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            item {
                GlobalSettingsSection(
                    notificationPreferences = notificationPreferences,
                    onToggle = onToggleGlobal
                )
            }

            item {
                SocialInteractionsSection(
                    notificationPreferences = notificationPreferences,
                    onToggleCategory = onToggleCategory
                )
            }

            item {
                ContentUpdatesSection(
                    notificationPreferences = notificationPreferences,
                    onToggleCategory = onToggleCategory
                )
            }

            item {
                SystemSecuritySection(
                    notificationPreferences = notificationPreferences,
                    onToggleCategory = onToggleCategory
                )
            }

            item {
                AdvancedSettingsSection(
                    notificationPreferences = notificationPreferences,
                    onToggleQuietHours = onToggleQuietHours,
                    onToggleDoNotDisturb = onToggleDoNotDisturb,
                    onShowStartTimePicker = onShowStartTimePicker
                )
            }
        }
    }
}

@Composable
private fun GlobalSettingsSection(
    notificationPreferences: NotificationPreferences,
    onToggle: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_global_settings_title)) {
        SettingsToggleItem(
            imageVector = Icons.Default.Notifications,
            title = stringResource(R.string.settings_enable_notifications_title),
            subtitle = if (notificationPreferences.globalEnabled) stringResource(R.string.settings_on) else stringResource(R.string.settings_off),
            checked = notificationPreferences.globalEnabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun SocialInteractionsSection(
    notificationPreferences: NotificationPreferences,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_social_interactions_title)) {
        SocialNotificationTypes.forEachIndexed { index, (category, label) ->
            val isEnabled = notificationPreferences.isEnabled(category)
            SettingsToggleItem(
                imageVector = when (category) {
                    NotificationCategory.LIKES -> Icons.Default.EmojiEmotions
                    NotificationCategory.FOLLOWS -> Icons.Default.Group
                    else -> Icons.Default.Notifications
                },
                title = stringResource(label),
                subtitle = if (isEnabled) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
                checked = isEnabled,
                onCheckedChange = { onToggleCategory(category, it) },
                enabled = notificationPreferences.globalEnabled
            )
            if (index < SocialNotificationTypes.size - 1) {
                SettingsDivider()
            }
        }
    }
}

@Composable
private fun ContentUpdatesSection(
    notificationPreferences: NotificationPreferences,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_content_updates_title)) {
        ContentNotificationTypes.forEachIndexed { index, (category, label) ->
            val isEnabled = notificationPreferences.isEnabled(category)
            SettingsToggleItem(
                imageVector = Icons.Default.ContentCopy,
                title = stringResource(label),
                subtitle = if (isEnabled) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
                checked = isEnabled,
                onCheckedChange = { onToggleCategory(category, it) },
                enabled = notificationPreferences.globalEnabled
            )
            if (index < ContentNotificationTypes.size - 1) {
                SettingsDivider()
            }
        }
    }
}

@Composable
private fun SystemSecuritySection(
    notificationPreferences: NotificationPreferences,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_system_security_section)) {
        SettingsToggleItem(
            imageVector = Icons.Default.Info,
            title = stringResource(R.string.settings_security_alerts_title),
            subtitle = stringResource(R.string.settings_security_alerts_subtitle),
            checked = true,
            onCheckedChange = { },
            enabled = false
        )
        SettingsDivider()
        SettingsToggleItem(
            imageVector = Icons.Default.Settings,
            title = stringResource(R.string.settings_app_updates_title),
            subtitle = if (notificationPreferences.updatesEnabled) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
            checked = notificationPreferences.updatesEnabled,
            onCheckedChange = { onToggleCategory(NotificationCategory.SYSTEM_UPDATES, it) },
            enabled = notificationPreferences.globalEnabled
        )
    }
}

@Composable
private fun AdvancedSettingsSection(
    notificationPreferences: NotificationPreferences,
    onToggleQuietHours: (Boolean) -> Unit,
    onToggleDoNotDisturb: (Boolean) -> Unit,
    onShowStartTimePicker: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_advanced_settings_title)) {
        QuietHoursItem(
            notificationPreferences = notificationPreferences,
            onToggleQuietHours = onToggleQuietHours,
            onShowStartTimePicker = onShowStartTimePicker
        )
        SettingsDivider()
        SettingsToggleItem(
            imageVector = Icons.Default.DoNotDisturb,
            title = stringResource(R.string.settings_do_not_disturb_title),
            subtitle = if (notificationPreferences.doNotDisturb) stringResource(R.string.settings_active) else stringResource(R.string.settings_inactive),
            checked = notificationPreferences.doNotDisturb,
            onCheckedChange = onToggleDoNotDisturb,
            enabled = notificationPreferences.globalEnabled
        )
    }
}

@Composable
private fun QuietHoursItem(
    notificationPreferences: NotificationPreferences,
    onToggleQuietHours: (Boolean) -> Unit,
    onShowStartTimePicker: () -> Unit
) {
    val enabled = notificationPreferences.globalEnabled

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.itemShape,
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onShowStartTimePicker)
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
                .heightIn(min = SettingsSpacing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(SettingsSpacing.iconSize),
                tint = SettingsColors.itemIcon
            )
            Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_quiet_hours_title),
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = if (notificationPreferences.quietHoursEnabled)
                        "${notificationPreferences.quietHoursStart} - ${notificationPreferences.quietHoursEnd}"
                    else stringResource(R.string.settings_disabled),
                    style = SettingsTypography.itemSubtitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.Medium))
            Switch(
                checked = notificationPreferences.quietHoursEnabled,
                onCheckedChange = onToggleQuietHours,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietHoursTimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    confirmText: String
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.split(":").getOrNull(0)?.toIntOrNull() ?: 0,
        initialMinute = initialTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(Spacing.Medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(Spacing.Medium))
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = {
                        val time = "${state.hour.toString().padStart(2, '0')}:${
                            state.minute.toString().padStart(2, '0')
                        }"
                        onConfirm(time)
                    }) { Text(confirmText) }
                }
            }
        }
    }
}
