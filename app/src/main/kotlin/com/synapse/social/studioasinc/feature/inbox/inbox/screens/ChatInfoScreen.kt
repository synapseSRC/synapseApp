package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatInfoViewModel
import com.synapse.social.studioasinc.feature.shared.components.UserAvatarWithStatus
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.util.TimestampFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    chatId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToCreateGroup: () -> Unit = {},
    onNavigateToDisappearingMessages: (String) -> Unit = {},
    onNavigateToSharedContent: (String, Int) -> Unit = { _, _ -> },
    onNavigateToChatPrivacy: (String) -> Unit = {},
    viewModel: ChatInfoViewModel = hiltViewModel()
) {
    LaunchedEffect(chatId, userId) {
        viewModel.loadChatInfo(chatId, userId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showClearChatDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showReportConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessageRes) {
        uiState.userMessageRes?.let { msgRes ->
            Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_info_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading && uiState.userProfile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                // Error banner if present
                uiState.errorMessageRes?.let { errorRes ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = stringResource(errorRes),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(Spacing.Medium)
                        )
                    }
                }

                // Compact Identity Header Section
                val profile = uiState.userProfile
                val displayName = profile?.displayName?.takeIf { it.isNotBlank() && it != "null" }
                    ?: profile?.username?.takeIf { it.isNotBlank() && it != "null" }
                    ?: stringResource(R.string.user)
                val username = profile?.username?.takeIf { it.isNotBlank() && it != "null" }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = Spacing.ExtraSmall)
                ) {
                    UserAvatarWithStatus(
                        userId = userId,
                        avatarUrl = profile?.avatar,
                        size = Sizes.AvatarLargeProfile,
                        showActiveStatus = true,
                        displayName = displayName
                    )

                    Spacer(modifier = Modifier.height(Spacing.Small))

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (username != null && username != displayName) {
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val statusText = when {
                        uiState.isOnline -> stringResource(R.string.chat_info_user_online)
                        profile?.lastSeen != null -> stringResource(
                            R.string.chat_info_user_last_seen,
                            TimestampFormatter.formatRelative(profile.lastSeen)
                        )
                        else -> stringResource(R.string.chat_info_user_offline)
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Compact Circular Quick Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.ExtraSmall),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularQuickAction(
                        icon = Icons.Default.Chat,
                        label = stringResource(R.string.chat_info_action_message),
                        onClick = onNavigateBack
                    )
                    CircularQuickAction(
                        icon = Icons.Default.Person,
                        label = stringResource(R.string.chat_info_action_profile),
                        onClick = { if (userId.isNotBlank()) onNavigateToProfile(userId) }
                    )
                    CircularQuickAction(
                        icon = if (uiState.isMuted) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                        label = if (uiState.isMuted) stringResource(R.string.unmuted) else stringResource(R.string.chat_info_action_mute),
                        onClick = { viewModel.toggleMuteNotifications() }
                    )
                }

                // Media, links, and docs Navigation Row
                val totalSharedCount = uiState.mediaCount + uiState.linkCount + uiState.fileCount
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSharedContent(chatId, 0) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PermMedia,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(Spacing.Medium))
                            Text(
                                text = stringResource(R.string.chat_info_media_links_docs),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (totalSharedCount > 0) {
                                Text(
                                    text = totalSharedCount.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Chat Settings Grouped Section
                SettingsSectionGroup(title = stringResource(R.string.chat_info_settings_title)) {
                    // Encryption
                    SettingsClickableItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.chat_info_encryption),
                        subtitle = stringResource(R.string.chat_info_encryption_subtitle),
                        onClick = { onNavigateToChatPrivacy(chatId) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Disappearing Messages
                    val disappearingLabel = when (uiState.disappearingMode) {
                        DisappearingMode.OFF -> stringResource(R.string.disappearing_mode_off)
                        DisappearingMode.TWENTY_FOUR_HOURS -> stringResource(R.string.disappearing_mode_24_hours)
                        DisappearingMode.SEVEN_DAYS -> stringResource(R.string.disappearing_mode_7_days)
                        else -> stringResource(R.string.disappearing_mode_off)
                    }

                    SettingsClickableItem(
                        icon = Icons.Default.Timer,
                        title = stringResource(R.string.chat_info_disappearing_messages),
                        subtitle = disappearingLabel,
                        onClick = { onNavigateToDisappearingMessages(chatId) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Chat Lock
                    SettingsSwitchItem(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.chat_info_chat_lock),
                        subtitle = stringResource(R.string.chat_info_chat_lock_subtitle),
                        checked = uiState.isLocked,
                        onCheckedChange = { viewModel.toggleChatLock(chatId) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Advanced Chat Privacy
                    SettingsClickableItem(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.chat_info_advanced_privacy),
                        subtitle = stringResource(R.string.chat_info_advanced_privacy_off),
                        onClick = { onNavigateToChatPrivacy(chatId) }
                    )
                }

                // Contact Actions Grouped Section
                SettingsSectionGroup(title = null) {
                    SettingsInfoItem(
                        icon = Icons.Default.Group,
                        title = stringResource(R.string.chat_info_no_groups_in_common),
                        subtitle = null
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SettingsClickableItem(
                        icon = Icons.Default.GroupAdd,
                        title = stringResource(R.string.chat_info_create_group_with, displayName),
                        subtitle = null,
                        onClick = onNavigateToCreateGroup
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SettingsClickableItem(
                        icon = Icons.Default.GroupAdd,
                        title = stringResource(R.string.chat_info_add_to_groups),
                        subtitle = stringResource(R.string.chat_info_add_to_groups_subtitle),
                        onClick = onNavigateToCreateGroup
                    )
                }

                // Danger Zone Grouped Section
                SettingsSectionGroup(
                    title = null,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                ) {
                    SettingsClickableItem(
                        icon = Icons.Default.DeleteForever,
                        title = stringResource(R.string.chat_info_clear_chat),
                        subtitle = null,
                        iconTint = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { showClearChatDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))

                    val blockTitle = if (uiState.isBlocked) {
                        stringResource(R.string.chat_info_unblock_user, displayName)
                    } else {
                        stringResource(R.string.chat_info_block_user, displayName)
                    }

                    SettingsClickableItem(
                        icon = Icons.Default.Block,
                        title = blockTitle,
                        subtitle = null,
                        iconTint = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { showBlockConfirmDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))

                    SettingsClickableItem(
                        icon = Icons.Default.Flag,
                        title = stringResource(R.string.chat_info_report_user, displayName),
                        subtitle = null,
                        iconTint = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { showReportConfirmDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.Medium))
            }
        }
    }

    // Confirmation Dialogs (Reserved for small, focused decisions only)
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text(stringResource(R.string.chat_info_clear_chat)) },
            text = { Text(stringResource(R.string.chat_info_clear_chat_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatDialog = false
                        viewModel.clearChat(chatId) {
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showBlockConfirmDialog) {
        val targetName = uiState.userProfile?.displayName ?: stringResource(R.string.user)
        val confirmTitle = if (uiState.isBlocked) {
            stringResource(R.string.chat_info_unblock_user, targetName)
        } else {
            stringResource(R.string.chat_info_block_user, targetName)
        }
        val confirmText = if (uiState.isBlocked) {
            stringResource(R.string.chat_info_unblock_user_confirm, targetName)
        } else {
            stringResource(R.string.chat_info_block_user_confirm, targetName)
        }

        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = { Text(confirmTitle) },
            text = { Text(confirmText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockConfirmDialog = false
                        viewModel.toggleBlockUser(userId)
                    }
                ) {
                    Text(
                        confirmTitle,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showReportConfirmDialog) {
        val targetName = uiState.userProfile?.displayName ?: stringResource(R.string.user)
        AlertDialog(
            onDismissRequest = { showReportConfirmDialog = false },
            title = { Text(stringResource(R.string.chat_info_report_user, targetName)) },
            text = { Text(stringResource(R.string.chat_info_report_user_confirm, targetName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReportConfirmDialog = false
                        Toast.makeText(context, context.getString(R.string.chat_info_msg_reported), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        stringResource(R.string.chat_info_report_user, targetName),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CircularQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.AvatarMedium)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(Sizes.IconDefault)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsSectionGroup(
    title: String?,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall)
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = containerColor
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    subtitle: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
