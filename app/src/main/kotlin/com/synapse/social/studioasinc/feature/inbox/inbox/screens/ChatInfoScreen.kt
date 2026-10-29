package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatInfoViewModel
import com.synapse.social.studioasinc.feature.shared.components.UserAvatarWithStatus
import com.synapse.social.studioasinc.feature.shared.components.pressScale
import com.synapse.social.studioasinc.feature.shared.theme.ChatInfoMetrics
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
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
    // Inherit the activity SynapseTheme so appearance settings and dynamic colors apply.
    LaunchedEffect(chatId, userId) { viewModel.loadChatInfo(chatId, userId) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showReportConfirmDialog by remember { mutableStateOf(false) }
    var showGroupsDialog by remember { mutableStateOf(false) }
    var showEncryptionSheet by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    val profile = uiState.userProfile
    val displayName = profile?.displayName?.takeIf { it.isNotBlank() && it != "null" }
        ?: profile?.username?.takeIf { it.isNotBlank() && it != "null" }
        ?: stringResource(R.string.user)
    val username = profile?.username?.takeIf { it.isNotBlank() && it != "null" }
    val sharedContentErrorRes = uiState.sharedContentErrorRes
    val commonGroupsErrorRes = uiState.commonGroupsErrorRes

    LaunchedEffect(uiState.userMessageRes) {
        uiState.userMessageRes?.let { messageRes ->
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contact_info_title)) },
                navigationIcon = {
                    val interaction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onNavigateBack,
                        interactionSource = interaction,
                        modifier = Modifier.pressScale(interaction)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    Box {
                        val interaction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = { moreMenuExpanded = true },
                            interactionSource = interaction,
                            modifier = Modifier.pressScale(interaction)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.chat_info_more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            val shareInteraction = remember { MutableInteractionSource() }
                            DropdownMenuItem(
                                modifier = Modifier.pressScale(shareInteraction),
                                interactionSource = shareInteraction,
                                text = { Text(stringResource(R.string.chat_info_share_contact)) },
                                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    val contactText = buildString {
                                        append(displayName)
                                        username?.let { append(" (@").append(it).append(')') }
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, contactText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, null))
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && profile == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space16),
                verticalArrangement = Arrangement.spacedBy(Spacing.space16)
            ) {
                uiState.errorMessageRes?.let { messageRes ->
                    item(key = "error") {
                        ErrorBanner(
                            message = stringResource(messageRes),
                            canRetry = uiState.canRetryLoad,
                            onRetry = { viewModel.retryLoadChatInfo(chatId, userId) }
                        )
                    }
                }
                item(key = "contact-header") {
                    ContactHeader(
                        userId = userId,
                        avatarUrl = profile?.avatar,
                        displayName = displayName,
                        username = username,
                        isOnline = uiState.isOnline,
                        lastSeen = profile?.lastSeen
                    )
                }
                item(key = "actions") {
                    QuickActions(
                        isMuted = uiState.isMuted,
                        isMuteUpdating = uiState.isMuteUpdating,
                        onMessage = onNavigateBack,
                        onProfile = { if (userId.isNotBlank()) onNavigateToProfile(userId) },
                        onMute = { viewModel.toggleMuteNotifications(chatId) }
                    )
                }
                item(key = "shared-media") {
                    val total = uiState.mediaCount + uiState.linkCount + uiState.fileCount
                    val summary = when {
                        uiState.isLoadingSharedContent -> stringResource(R.string.chat_info_media_loading)
                        sharedContentErrorRes != null -> stringResource(sharedContentErrorRes)
                        total == 0 -> stringResource(R.string.chat_info_media_empty)
                        uiState.isSharedContentSample -> stringResource(R.string.chat_info_media_partial_summary, total)
                        else -> stringResource(
                            R.string.chat_info_media_summary,
                            uiState.mediaCount,
                            uiState.linkCount,
                            uiState.fileCount
                        )
                    }
                    SettingsItem(
                        icon = Icons.Rounded.PermMedia,
                        title = stringResource(R.string.chat_info_media_links_docs),
                        supportingText = summary,
                        shape = RoundedCornerShape(ChatInfoMetrics.cardCorner),
                        trailingContent = if (uiState.sharedContentErrorRes != null) {
                            {
                                PressScaleTextButton(onClick = { viewModel.retrySharedContent(chatId) }) {
                                    Text(stringResource(R.string.chat_info_retry))
                                }
                            }
                        } else null,
                        onClick = { onNavigateToSharedContent(chatId, 0) }
                    )
                }
                item(key = "privacy-heading") {
                    SectionHeading(stringResource(R.string.chat_info_privacy_section_title))
                }
                item(key = "privacy-settings") {
                    Column(verticalArrangement = Arrangement.spacedBy(ChatInfoMetrics.listItemGap)) {
                        SettingsItem(
                            icon = Icons.Rounded.Lock,
                            title = stringResource(R.string.chat_info_encryption_title),
                            supportingText = stringResource(R.string.chat_info_encryption_secure_summary),
                            trailingIcon = Icons.Rounded.VerifiedUser,
                            position = ItemPosition.FIRST,
                            onClick = { showEncryptionSheet = true }
                        )
                        val disappearingLabel = when (uiState.disappearingMode) {
                            DisappearingMode.OFF -> stringResource(R.string.disappearing_mode_off)
                            DisappearingMode.TWENTY_FOUR_HOURS -> stringResource(R.string.disappearing_mode_24_hours)
                            DisappearingMode.SEVEN_DAYS -> stringResource(R.string.disappearing_mode_7_days)
                            else -> stringResource(R.string.disappearing_mode_off)
                        }
                        SettingsItem(
                            icon = Icons.Rounded.Timer,
                            title = stringResource(R.string.chat_info_disappearing_messages),
                            supportingText = disappearingLabel,
                            position = ItemPosition.MIDDLE,
                            onClick = { onNavigateToDisappearingMessages(chatId) }
                        )
                        SettingsItem(
                            icon = Icons.Rounded.Fingerprint,
                            title = stringResource(R.string.chat_info_chat_lock),
                            supportingText = stringResource(R.string.chat_info_chat_lock_biometric_summary),
                            position = ItemPosition.MIDDLE,
                            trailingContent = {
                                val lockInteraction = remember { MutableInteractionSource() }
                                Switch(
                                    checked = uiState.isLocked,
                                    onCheckedChange = { viewModel.toggleChatLock(chatId) },
                                    interactionSource = lockInteraction,
                                    modifier = Modifier.pressScale(lockInteraction)
                                )
                        },
                            onClick = { viewModel.toggleChatLock(chatId) }
                        )
                        SettingsItem(
                            icon = Icons.Rounded.Security,
                            title = stringResource(R.string.chat_info_advanced_privacy_title),
                            supportingText = stringResource(R.string.chat_info_advanced_privacy_summary),
                            trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            position = ItemPosition.LAST,
                            onClick = { onNavigateToChatPrivacy(chatId) }
                        )
                    }
                }
                item(key = "groups") {
                    val groupSummary = when {
                        commonGroupsErrorRes != null -> stringResource(commonGroupsErrorRes)
                        uiState.isLoadingCommonGroups -> stringResource(R.string.chat_info_groups_loading)
                        uiState.commonGroups.isEmpty() -> stringResource(R.string.chat_info_no_groups_in_common)
                        uiState.commonGroups.size == 1 -> stringResource(
                            R.string.chat_info_common_groups_one,
                            uiState.commonGroups.first().participantName
                        )
                        else -> stringResource(
                            R.string.chat_info_common_groups_many,
                            uiState.commonGroups.size,
                            uiState.commonGroups.joinToString { it.participantName }
                        )
                    }
                    SettingsItem(
                        icon = Icons.Rounded.Group,
                        title = stringResource(R.string.chat_info_groups_in_common),
                        supportingText = groupSummary,
                        trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        onClick = { showGroupsDialog = true }
                    )
                }
                item(key = "contact-actions-heading") {
                    SectionHeading(stringResource(R.string.chat_info_contact_actions_title))
                }
                item(key = "contact-actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(ChatInfoMetrics.listItemGap)) {
                        SettingsItem(
                            icon = Icons.Rounded.DeleteOutline,
                            title = stringResource(R.string.chat_info_clear_history_title),
                            supportingText = stringResource(R.string.chat_info_clear_history_subtitle),
                            iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                            iconTint = MaterialTheme.colorScheme.onErrorContainer,
                            textColor = MaterialTheme.colorScheme.error,
                            position = ItemPosition.FIRST,
                            onClick = { showClearChatDialog = true }
                        )
                        SettingsItem(
                            icon = Icons.Rounded.Block,
                            title = if (uiState.isBlocked) {
                                stringResource(R.string.chat_info_unblock_user, displayName)
                            } else {
                                stringResource(R.string.chat_info_block_user, displayName)
                            },
                            supportingText = stringResource(R.string.chat_info_block_user_subtitle),
                            iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                            iconTint = MaterialTheme.colorScheme.onErrorContainer,
                            textColor = MaterialTheme.colorScheme.error,
                            position = ItemPosition.MIDDLE,
                            onClick = { showBlockConfirmDialog = true }
                        )
                        SettingsItem(
                            icon = Icons.Rounded.ReportProblem,
                            title = stringResource(R.string.chat_info_report_contact),
                            supportingText = stringResource(R.string.chat_info_report_contact_subtitle),
                            iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                            iconTint = MaterialTheme.colorScheme.onErrorContainer,
                            textColor = MaterialTheme.colorScheme.error,
                            position = ItemPosition.LAST,
                            onClick = { showReportConfirmDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text(stringResource(R.string.chat_info_clear_history_title)) },
            text = { Text(stringResource(R.string.chat_info_clear_chat_confirm)) },
            confirmButton = {
                PressScaleTextButton(onClick = {
                    showClearChatDialog = false
                    viewModel.clearChat(chatId) { onNavigateBack() }
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showClearChatDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showBlockConfirmDialog) {
        val title = if (uiState.isBlocked) {
            stringResource(R.string.chat_info_unblock_user, displayName)
        } else {
            stringResource(R.string.chat_info_block_user, displayName)
        }
        val message = if (uiState.isBlocked) {
            stringResource(R.string.chat_info_unblock_user_confirm, displayName)
        } else {
            stringResource(R.string.chat_info_block_user_confirm, displayName)
        }
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                PressScaleTextButton(onClick = {
                    showBlockConfirmDialog = false
                    viewModel.toggleBlockUser(userId)
                }) { Text(title, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showReportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReportConfirmDialog = false },
            title = { Text(stringResource(R.string.chat_info_report_contact)) },
            text = { Text(stringResource(R.string.chat_info_report_user_confirm, displayName)) },
            confirmButton = {
                PressScaleTextButton(onClick = {
                    showReportConfirmDialog = false
                    viewModel.reportUser(userId, context.getString(R.string.chat_info_report_reason))
                }) {
                    Text(stringResource(R.string.chat_info_report_contact), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showReportConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showGroupsDialog) {
        AlertDialog(
            onDismissRequest = { showGroupsDialog = false },
            title = { Text(stringResource(R.string.chat_info_groups_dialog_title)) },
            text = {
                when {
                    uiState.isLoadingCommonGroups -> Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space16),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                        uiState.commonGroupsErrorRes?.let { errorRes ->
                            Text(stringResource(errorRes))
                            PressScaleTextButton(onClick = { viewModel.retryCommonGroups(userId) }) {
                                Text(stringResource(R.string.chat_info_retry))
                            }
                        }
                        if (uiState.commonGroups.isEmpty()) {
                            if (uiState.commonGroupsErrorRes == null) {
                                Text(stringResource(R.string.chat_info_groups_dialog_empty))
                            }
                        } else {
                            CommonGroupList(uiState.commonGroups)
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.commonGroups.isEmpty() && !uiState.isLoadingCommonGroups && uiState.commonGroupsErrorRes == null) {
                    PressScaleTextButton(onClick = {
                        showGroupsDialog = false
                        onNavigateToCreateGroup()
                    }) { Text(stringResource(R.string.chat_info_action_create_group)) }
                } else {
                    PressScaleTextButton(onClick = { showGroupsDialog = false }) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showGroupsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEncryptionSheet) {
        ChatEncryptionSheet(
            chatId = chatId,
            participantName = displayName,
            onDismiss = { showEncryptionSheet = false }
        )
    }
}

@Composable
private fun ContactHeader(
    userId: String,
    avatarUrl: String?,
    displayName: String,
    username: String?,
    isOnline: Boolean,
    lastSeen: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatarWithStatus(
            userId = userId,
            avatarUrl = avatarUrl,
            size = ChatInfoMetrics.contactAvatarSize,
            showActiveStatus = true,
            displayName = displayName
        )
        Spacer(Modifier.height(Spacing.space8))
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!username.isNullOrBlank() && username != displayName) {
            Text(
                text = "@$username",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val status = when {
            isOnline -> stringResource(R.string.chat_info_user_online)
            lastSeen != null -> stringResource(
                R.string.chat_info_user_last_seen,
                TimestampFormatter.formatRelative(lastSeen)
            )
            else -> stringResource(R.string.chat_info_user_offline)
        }
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActions(
    isMuted: Boolean,
    isMuteUpdating: Boolean,
    onMessage: () -> Unit,
    onProfile: () -> Unit,
    onMute: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChatInfoMetrics.connectedActionGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickActionButton(
            label = stringResource(R.string.chat_info_action_message),
            icon = Icons.Rounded.Chat,
            position = ItemPosition.FIRST,
            onClick = onMessage,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = stringResource(R.string.chat_info_action_profile),
            icon = Icons.Rounded.Person,
            position = ItemPosition.MIDDLE,
            onClick = onProfile,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = stringResource(if (isMuted) R.string.chat_info_action_muted else R.string.chat_info_action_mute),
            icon = if (isMuted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
            position = ItemPosition.LAST,
            checked = isMuted,
            enabled = !isMuteUpdating,
            onClick = onMute,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    position: ItemPosition,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val buttonModifier = modifier.height(ChatInfoMetrics.actionHeight).pressScale(interaction)
    val content: @Composable RowScope.() -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ChatInfoMetrics.actionIconSize))
        Spacer(Modifier.size(Spacing.space8))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
    if (checked) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = connectedButtonShape(position),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = Spacing.space8),
            interactionSource = interaction,
            content = content
        )
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = connectedButtonShape(position),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            contentPadding = PaddingValues(horizontal = Spacing.space8),
            interactionSource = interaction,
            content = content
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    supportingText: String,
    position: ItemPosition = ItemPosition.SINGLE,
    shape: Shape = settingsItemShape(position),
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    trailingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .then(
            if (onClick != null) {
                Modifier.clickable(interactionSource = interaction, onClick = onClick)
            } else {
                Modifier
            }
        )
        .pressScale(interaction)
        .heightIn(min = ChatInfoMetrics.listItemMinHeight)

    ListItem(
        modifier = rowModifier,
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        },
        supportingContent = {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(ChatInfoMetrics.iconContainerSize),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = iconContainerColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(ChatInfoMetrics.listIconSize)
                        )
                    }
                }
            }
        },
        trailingContent = {
            if (trailingContent != null) {
                trailingContent()
            } else if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    )
}

@Composable
private fun PressScaleTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier.pressScale(interaction),
        interactionSource = interaction,
        content = content
    )
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space4)
    )
}

@Composable
private fun CommonGroupList(groups: List<Conversation>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        groups.forEach { group ->
            ListItem(
                headlineContent = { Text(group.participantName) },
                leadingContent = { Icon(Icons.Rounded.Group, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, canRetry: Boolean, onRetry: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ChatInfoMetrics.cardCorner),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (canRetry) {
                PressScaleTextButton(onClick = onRetry) {
                    Text(stringResource(R.string.chat_info_retry))
                }
            }
        }
    }
}

private enum class ItemPosition { SINGLE, FIRST, MIDDLE, LAST }

private fun connectedButtonShape(position: ItemPosition): Shape = when (position) {
    ItemPosition.FIRST -> RoundedCornerShape(topStart = ChatInfoMetrics.listOuterCorner, bottomStart = ChatInfoMetrics.listOuterCorner, topEnd = ChatInfoMetrics.listInnerCorner, bottomEnd = ChatInfoMetrics.listInnerCorner)
    ItemPosition.MIDDLE -> RoundedCornerShape(ChatInfoMetrics.listInnerCorner)
    ItemPosition.LAST -> RoundedCornerShape(topStart = ChatInfoMetrics.listInnerCorner, bottomStart = ChatInfoMetrics.listInnerCorner, topEnd = ChatInfoMetrics.listOuterCorner, bottomEnd = ChatInfoMetrics.listOuterCorner)
    ItemPosition.SINGLE -> RoundedCornerShape(ChatInfoMetrics.listOuterCorner)
}

private fun settingsItemShape(position: ItemPosition): Shape = when (position) {
    ItemPosition.FIRST -> RoundedCornerShape(topStart = ChatInfoMetrics.listOuterCorner, topEnd = ChatInfoMetrics.listOuterCorner, bottomStart = ChatInfoMetrics.listInnerCorner, bottomEnd = ChatInfoMetrics.listInnerCorner)
    ItemPosition.MIDDLE -> RoundedCornerShape(ChatInfoMetrics.listInnerCorner)
    ItemPosition.LAST -> RoundedCornerShape(topStart = ChatInfoMetrics.listInnerCorner, topEnd = ChatInfoMetrics.listInnerCorner, bottomStart = ChatInfoMetrics.listOuterCorner, bottomEnd = ChatInfoMetrics.listOuterCorner)
    ItemPosition.SINGLE -> RoundedCornerShape(ChatInfoMetrics.listOuterCorner)
}
