package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private val SocialNotificationTypes = listOf(
    NotificationCategory.LIKES to "Likes",
    NotificationCategory.COMMENTS to "Comments",
    NotificationCategory.REPLIES to "Replies",
    NotificationCategory.FOLLOWS to "New Followers",
    NotificationCategory.MENTIONS to "Mentions"
)

private val ContentNotificationTypes = listOf(
    NotificationCategory.NEW_POSTS to "New Posts from Followed Users",
    NotificationCategory.SHARES to "Shares of Your Posts"
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
            title = "Start Quiet Hours",
            initialTime = notificationPreferences.quietHoursStart,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { time ->
                viewModel.setQuietHours(time, notificationPreferences.quietHoursEnd)
                showStartTimePicker = false
                showEndTimePicker = true
            },
            confirmText = "Next"
        )
    }

    if (showEndTimePicker) {
        QuietHoursTimePickerDialog(
            title = "End Quiet Hours",
            initialTime = notificationPreferences.quietHoursEnd,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { time ->
                viewModel.setQuietHours(notificationPreferences.quietHoursStart, time)
                showEndTimePicker = false
            },
            confirmText = "Done"
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
        topBar = {
            MediumTopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = onClearError) {
                            Text("Dismiss")
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
            contentPadding = PaddingValues(bottom = 32.dp),
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
    SettingsSection(title = "Global Settings") {
        SettingsToggleItem(
            imageVector = Icons.Default.Notifications,
            title = "Enable Notifications",
            subtitle = if (notificationPreferences.globalEnabled) "On" else "Off",
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
    SettingsSection(title = "Social Interactions") {
        SocialNotificationTypes.forEachIndexed { index, (category, label) ->
            val isEnabled = notificationPreferences.isEnabled(category)
            SettingsToggleItem(
                imageVector = when (category) {
                    NotificationCategory.LIKES -> Icons.Default.EmojiEmotions
                    NotificationCategory.FOLLOWS -> Icons.Default.Group
                    else -> Icons.Default.Notifications
                },
                title = label,
                subtitle = if (isEnabled) "Enabled" else "Disabled",
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
    SettingsSection(title = "Content Updates") {
        ContentNotificationTypes.forEachIndexed { index, (category, label) ->
            val isEnabled = notificationPreferences.isEnabled(category)
            SettingsToggleItem(
                imageVector = Icons.Default.ContentCopy,
                title = label,
                subtitle = if (isEnabled) "Enabled" else "Disabled",
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
    SettingsSection(title = "System & Security") {
        SettingsToggleItem(
            imageVector = Icons.Default.Info,
            title = "Security Alerts",
            subtitle = "Always enabled",
            checked = true,
            onCheckedChange = { },
            enabled = false
        )
        SettingsDivider()
        SettingsToggleItem(
            imageVector = Icons.Default.Settings,
            title = "App Updates",
            subtitle = if (notificationPreferences.updatesEnabled) "Enabled" else "Disabled",
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
    SettingsSection(title = "Advanced Settings") {
        QuietHoursItem(
            notificationPreferences = notificationPreferences,
            onToggleQuietHours = onToggleQuietHours,
            onShowStartTimePicker = onShowStartTimePicker
        )
        SettingsDivider()
        SettingsToggleItem(
            imageVector = Icons.Default.DoNotDisturb,
            title = "Do Not Disturb",
            subtitle = if (notificationPreferences.doNotDisturb) "Active" else "Inactive",
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
                    text = "Quiet Hours",
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = if (notificationPreferences.quietHoursEnabled)
                        "${notificationPreferences.quietHoursStart} - ${notificationPreferences.quietHoursEnd}"
                    else "Disabled",
                    style = SettingsTypography.itemSubtitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
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
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
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
