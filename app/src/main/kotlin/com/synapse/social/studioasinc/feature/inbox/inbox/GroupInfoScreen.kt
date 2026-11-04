@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.synapse.social.studioasinc.feature.inbox.inbox

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.UserAvatar
import com.synapse.social.studioasinc.feature.shared.components.pressScale
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.ChatInfoMetrics
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinRequest
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.ui.settings.SettingsClickableItem
import com.synapse.social.studioasinc.ui.settings.SettingsItemPosition
import com.synapse.social.studioasinc.ui.settings.SettingsListItem
import com.synapse.social.studioasinc.ui.settings.SettingsNavigationItem
import com.synapse.social.studioasinc.ui.settings.SettingsToggleItem
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

/**
 * WhatsApp-style group information screen rendered with the M3 Expressive contained-list treatment.
 *
 * The screen is a single [LazyColumn] split into six sections: header, quick actions, about,
 * invite link, pending join requests, group settings and the danger zone. The participant roster —
 * both the list and its search — lives entirely on [GroupParticipantsScreen]; this screen only
 * carries the entry point to it inside the settings group. All copy comes from string resources and
 * all metrics from the shared design tokens.
 */
@Composable
fun GroupInfoScreen(
    chatId: String,
    groupName: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToSharedContent: (String) -> Unit = {},
    onNavigateToParticipants: () -> Unit = {},
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val clipLabel = stringResource(R.string.clip_label_group_invite)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Group avatar picker: pick an image then crop to a 1:1 square before uploading.
    // CropImageContract is deprecated upstream (com.canhub.cropper) with no drop-in replacement;
    // rolling a custom ActivityResultContract is a separate project, not a warning fix.
    @Suppress("DEPRECATION")
    val avatarCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { viewModel.onGroupAvatarPicked(it) }
        }
    }
    @Suppress("DEPRECATION")
    val avatarPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            avatarCropLauncher.launch(
                CropImageContractOptions(
                    uri,
                    CropImageOptions(aspectRatioX = 1, aspectRatioY = 1, fixAspectRatio = true)
                )
            )
        }
    }

    LaunchedEffect(chatId) { viewModel.loadGroupInfo(chatId) }

    // One-shot error / success notices surface through the snackbar and are then consumed.
    val errorRes = uiState.errorRes
    LaunchedEffect(errorRes) {
        if (errorRes != null) {
            snackbarHostState.showSnackbar(context.getString(errorRes))
            viewModel.clearError()
        }
    }
    val successRes = uiState.successMessageRes
    LaunchedEffect(successRes) {
        if (successRes != null) {
            snackbarHostState.showSnackbar(context.getString(successRes))
            viewModel.clearSuccess()
        }
    }

    val groupInfo = uiState.groupInfo
    val members = uiState.members
    val isAdmin = uiState.currentUserIsAdmin
    val canEditGroupInfo = uiState.canEditGroupInfo
    val isCreator = uiState.currentUserIsCreator
    val inviteCode = uiState.inviteCode
    val allowMemberInvites = groupInfo?.allowMemberInvites ?: false
    val allowMemberSettingsEdits = groupInfo?.allowMemberSettingsEdits ?: false
    val requireJoinApproval = groupInfo?.requireJoinApproval ?: false

    // `groupName` (route arg) is only a fallback until the loaded info arrives.
    val displayName = groupInfo?.name?.takeIf { it.isNotBlank() } ?: groupName
    val creatorUser = members.firstOrNull { it.first.uid == groupInfo?.createdBy }?.first
    val creatorName = if (creatorUser != null) userDisplayName(creatorUser) else null
    val memberCountText = stringResource(R.string.group_member_count_subtitle, members.size)
    val subtitle = if (creatorName != null) {
        "$memberCountText \u2022 ${stringResource(R.string.group_created_by, creatorName)}"
    } else {
        memberCountText
    }
    val createdDate = TimestampFormatter.formatLastSeen(groupInfo?.createdAt).takeIf { it.isNotBlank() }
    val createdText = if (createdDate != null) stringResource(R.string.group_created_on, createdDate) else null

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditDescriptionDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReportConfirmDialog by remember { mutableStateOf(false) }
    var showInviteRevokeDialog by remember { mutableStateOf(false) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<User?>(null) }

    val description = groupInfo?.description
    val showDescription = !description.isNullOrBlank() || canEditGroupInfo
    val inviteLink = inviteCode?.let { INVITE_BASE_URL + it }
    val showInvite = inviteLink != null && (isAdmin || allowMemberInvites)
    val pendingCount = uiState.pendingRequests.size
    val showPending = isAdmin && requireJoinApproval

    Scaffold(
        containerColor = AppColors.screenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_info_title)) },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.screenBackground,
                    scrolledContainerColor = AppColors.cardBackground
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && groupInfo == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            groupInfo == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(Spacing.space16),
                    contentAlignment = Alignment.Center
                ) {
                    val message = if (errorRes != null) {
                        stringResource(errorRes)
                    } else {
                        stringResource(R.string.group_error_create)
                    }
                    ErrorBanner(message = message, onRetry = { viewModel.loadGroupInfo(chatId) })
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space16),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space16)
                ) {
                    item(key = "header") {
                        GroupHeader(
                            avatarUrl = groupInfo.avatarUrl,
                            displayName = displayName,
                            subtitle = subtitle,
                            createdText = createdText,
                            canEdit = canEditGroupInfo,
                            isAvatarUploading = uiState.isAvatarUploading,
                            onEditName = { showEditNameDialog = true },
                            onEditAvatar = {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    item(key = "actions") {
                        GroupQuickActions(
                            isMuted = uiState.isMuted,
                            isMuteUpdating = uiState.isMuteUpdating,
                            isAdmin = isAdmin,
                            onToggleMute = { viewModel.toggleMute() },
                            onAddMember = {
                                showAddMemberDialog = true
                                viewModel.loadAddableContacts()
                            }
                        )
                    }

                    if (showDescription) {
                        item(key = "description") {
                            GroupDescriptionCard(
                                description = description,
                                canEdit = canEditGroupInfo,
                                onClick = { showEditDescriptionDialog = true }
                            )
                        }
                    }

                    if (inviteLink != null && showInvite) {
                        item(key = "invite") {
                            GroupInviteCard(
                                inviteLink = inviteLink,
                                isAdmin = isAdmin,
                                onCopy = {
                                    scope.launch {
                                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, inviteLink)))
                                    }
                                    Toast.makeText(context, R.string.group_link_copied, Toast.LENGTH_SHORT).show()
                                },
                                onShare = {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, inviteLink)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(send, context.getString(R.string.group_share_link))
                                    )
                                },
                                onRevoke = { showInviteRevokeDialog = true }
                            )
                        }
                    }

                    if (showPending) {
                        item(key = "pending") {
                            SettingsNavigationItem(
                                title = stringResource(R.string.group_pending_requests, pendingCount),
                                subtitle = stringResource(R.string.group_pending_requests_support),
                                imageVector = Icons.Filled.GroupAdd,
                                onClick = { showRequestsDialog = true },
                                indicator = if (pendingCount > 0) {
                                    { PendingRequestBadge(pendingCount) }
                                } else {
                                    null
                                }
                            )
                        }
                    }

                    item(key = "settings-heading") {
                        SectionHeading(stringResource(R.string.group_settings_section))
                    }

                    item(key = "settings") {
                        GroupSettingsSection(
                            isAdmin = isAdmin,
                            allowMemberSettingsEdits = allowMemberSettingsEdits,
                            disappearingMode = groupInfo.disappearingMode,
                            onlyAdminsCanMessage = groupInfo.onlyAdminsCanMessage,
                            requireJoinApproval = requireJoinApproval,
                            allowMemberInvites = allowMemberInvites,
                            memberCount = members.size,
                            onOpenDisappearing = { showDisappearingDialog = true },
                            onToggleOnlyAdmins = { viewModel.toggleOnlyAdminsCanMessage(it) },
                            onToggleMemberSettings = { viewModel.toggleAllowMemberSettingsEdits(it) },
                            onToggleApproval = { viewModel.toggleRequireJoinApproval(it) },
                            onToggleInvites = { viewModel.toggleAllowMemberInvites(it) },
                            onViewAllParticipants = onNavigateToParticipants
                        )
                    }

                    item(key = "danger-heading") {
                        SectionHeading(
                            text = stringResource(R.string.group_danger_zone),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    item(key = "danger") {
                        val dangerCount = 2 + (if (isCreator) 1 else 0)
                        Column(verticalArrangement = Arrangement.spacedBy(ChatInfoMetrics.listItemGap)) {
                            DangerActionItem(
                                icon = Icons.Rounded.ReportProblem,
                                title = stringResource(R.string.group_report),
                                position = itemPositionAt(0, dangerCount),
                                onClick = { showReportConfirmDialog = true }
                            )
                            DangerActionItem(
                                icon = Icons.AutoMirrored.Filled.Logout,
                                title = stringResource(R.string.leave_group),
                                position = itemPositionAt(1, dangerCount),
                                onClick = { showLeaveConfirmDialog = true }
                            )
                            if (isCreator) {
                                DangerActionItem(
                                    icon = Icons.Rounded.Delete,
                                    title = stringResource(R.string.group_delete),
                                    position = itemPositionAt(2, dangerCount),
                                    onClick = { showDeleteConfirmDialog = true }
                                )
                            }
                        }
                    }

                    item(key = "bottom-spacer") { Spacer(Modifier.height(Spacing.space32)) }
                }
            }
        }
    }

    if (showEditNameDialog) {
        var nameText by remember { mutableStateOf(groupInfo?.name ?: groupName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(stringResource(R.string.group_edit_name)) },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { if (it.length <= GROUP_NAME_MAX_LENGTH) nameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    placeholder = { Text(stringResource(R.string.group_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                PressScaleTextButton(
                    onClick = {
                        viewModel.updateGroupName(nameText.trim())
                        showEditNameDialog = false
                    },
                    enabled = nameText.isNotBlank()
                ) { Text(stringResource(R.string.group_save)) }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showEditNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEditDescriptionDialog) {
        var descriptionText by remember { mutableStateOf(groupInfo?.description ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDescriptionDialog = false },
            title = { Text(stringResource(R.string.group_edit_description)) },
            text = {
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { if (it.length <= GROUP_DESCRIPTION_MAX_LENGTH) descriptionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    placeholder = { Text(stringResource(R.string.group_description_label)) },
                    minLines = DESCRIPTION_FIELD_MIN_LINES
                )
            },
            confirmButton = {
                PressScaleTextButton(
                    onClick = {
                        viewModel.updateGroupDescription(descriptionText.trim())
                        showEditDescriptionDialog = false
                    }
                ) { Text(stringResource(R.string.group_save)) }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showEditDescriptionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDisappearingDialog) {
        val currentMode = groupInfo?.disappearingMode ?: DisappearingMode.OFF
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text(stringResource(R.string.group_disappearing_messages)) },
            text = {
                Column {
                    DisappearingMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDisappearingMode(mode)
                                    showDisappearingDialog = false
                                }
                                .padding(vertical = Spacing.space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == currentMode,
                                onClick = {
                                    viewModel.setDisappearingMode(mode)
                                    showDisappearingDialog = false
                                }
                            )
                            Spacer(Modifier.width(Spacing.space8))
                            Text(disappearingLabel(mode), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                PressScaleTextButton(onClick = { showDisappearingDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text(stringResource(R.string.group_remove_member_title)) },
            text = { Text(stringResource(R.string.group_remove_member_body, userDisplayName(member))) },
            confirmButton = {
                PressScaleTextButton(
                    onClick = {
                        viewModel.removeMember(member.uid)
                        memberToRemove = null
                    }
                ) {
                    Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { memberToRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text(stringResource(R.string.leave_group)) },
            text = { Text(stringResource(R.string.leave_group_confirm)) },
            confirmButton = {
                PressScaleTextButton(
                    onClick = {
                        viewModel.leaveGroup {
                            showLeaveConfirmDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.leave_group), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.group_delete_confirm_title)) },
            text = { Text(stringResource(R.string.group_delete_confirm_body)) },
            confirmButton = {
                PressScaleTextButton(
                    onClick = {
                        viewModel.deleteGroup {
                            showDeleteConfirmDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.group_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showReportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReportConfirmDialog = false },
            title = { Text(stringResource(R.string.group_report_title)) },
            text = { Text(stringResource(R.string.group_report_body)) },
            confirmButton = {
                PressScaleTextButton(
                    onClick = {
                        viewModel.reportGroup(GROUP_REPORT_REASON)
                        showReportConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.group_report), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showReportConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showInviteRevokeDialog) {
        AlertDialog(
            onDismissRequest = { showInviteRevokeDialog = false },
            title = { Text(stringResource(R.string.group_revoke_title)) },
            text = { Text(stringResource(R.string.group_revoke_body)) },
            confirmButton = {
                PressScaleTextButton(
                    onClick = {
                        showInviteRevokeDialog = false
                        viewModel.rotateInviteCode()
                    }
                ) { Text(stringResource(R.string.group_revoke_confirm)) }
            },
            dismissButton = {
                PressScaleTextButton(onClick = { showInviteRevokeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showRequestsDialog) {
        JoinRequestsDialog(
            requests = uiState.pendingRequests,
            onApprove = { viewModel.approveRequest(it) },
            onDecline = { viewModel.declineRequest(it) },
            onDismiss = { showRequestsDialog = false }
        )
    }

    if (showAddMemberDialog) {
        val addMemberSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
        ModalBottomSheet(
            onDismissRequest = { showAddMemberDialog = false },
            sheetState = addMemberSheetState,
            containerColor = AppColors.screenBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                Text(
                    text = stringResource(R.string.group_add_members_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.space16, vertical = Spacing.space8)
                )
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    MembersStage(
                        users = uiState.addableUsers,
                        selectedUsers = uiState.selectedUsersToAdd,
                        searchQuery = uiState.addMemberSearchQuery,
                        isLoading = uiState.isLoadingContacts,
                        error = null,
                        onSearch = viewModel::onAddMemberSearch,
                        onToggleUser = viewModel::toggleAddMemberSelection,
                        onRemoveUser = viewModel::removeAddMemberSelection,
                        onClearSelection = viewModel::clearAddMemberSelection,
                        onClearError = {}
                    )
                }
                Button(
                    onClick = { viewModel.confirmAddMembers { showAddMemberDialog = false } },
                    enabled = uiState.selectedUsersToAdd.isNotEmpty(),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.space16)
                ) {
                    Text(stringResource(R.string.group_add_members_confirm, uiState.selectedUsersToAdd.size))
                }
            }
        }
    }
}

// region Section composables

@Composable
private fun GroupHeader(
    avatarUrl: String?,
    displayName: String,
    subtitle: String,
    createdText: String?,
    canEdit: Boolean,
    isAvatarUploading: Boolean = false,
    onEditName: () -> Unit,
    onEditAvatar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            UserAvatar(
                avatarUrl = avatarUrl,
                size = ChatInfoMetrics.contactAvatarSize
            )
            if (isAvatarUploading) {
                Box(
                    modifier = Modifier
                        .size(ChatInfoMetrics.contactAvatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ChatInfoMetrics.actionIconSize),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (canEdit) {
                val badgeInteraction = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(ChatInfoMetrics.iconContainerSize)
                        .pressScale(badgeInteraction),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(interactionSource = badgeInteraction, onClick = onEditAvatar),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.group_edit_avatar),
                            modifier = Modifier.size(ChatInfoMetrics.actionIconSize)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.space12))
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = if (canEdit) Modifier.clickable(onClick = onEditName) else Modifier
        )
        Spacer(Modifier.height(Spacing.space4))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (createdText != null) {
            Spacer(Modifier.height(Spacing.space4))
            Text(
                text = createdText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GroupQuickActions(
    isMuted: Boolean,
    isMuteUpdating: Boolean,
    isAdmin: Boolean,
    onToggleMute: () -> Unit,
    onAddMember: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChatInfoMetrics.connectedActionGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupQuickAction(
            label = stringResource(if (isMuted) R.string.group_unmute_notifications else R.string.group_mute_notifications),
            icon = if (isMuted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
            position = if (isAdmin) ItemPosition.FIRST else ItemPosition.SINGLE,
            enabled = !isMuteUpdating,
            onClick = onToggleMute,
            modifier = Modifier.weight(1f)
        )
        if (isAdmin) {
            GroupQuickAction(
                label = stringResource(R.string.group_add_member),
                icon = Icons.Filled.PersonAdd,
                position = ItemPosition.LAST,
                onClick = onAddMember,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GroupQuickAction(
    label: String,
    icon: ImageVector,
    position: ItemPosition,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .height(ChatInfoMetrics.actionHeight)
            .pressScale(interaction),
        shape = connectedButtonShape(position),
        color = AppColors.cardBackground,
        contentColor = AppColors.itemContent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = interaction, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(ChatInfoMetrics.actionIconSize)
            )
        }
    }
}

@Composable
private fun GroupDescriptionCard(
    description: String?,
    canEdit: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = AppShapes.sectionShape,
        colors = CardDefaults.cardColors(containerColor = AppColors.cardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (canEdit) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(Spacing.space16)
        ) {
            SectionHeading(stringResource(R.string.group_description_section))
            Spacer(Modifier.height(Spacing.space4))
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = stringResource(R.string.group_tap_to_add_description),
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GroupInviteCard(
    inviteLink: String,
    isAdmin: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRevoke: () -> Unit
) {
    Card(
        shape = AppShapes.sectionShape,
        colors = CardDefaults.cardColors(containerColor = AppColors.cardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.space16),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.group_invite_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.space4))
            Text(
                text = inviteLink,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Spacing.space12))
            val qrForeground = MaterialTheme.colorScheme.onSurface.toArgb()
            val qrBackground = MaterialTheme.colorScheme.surface.toArgb()
            val qr = remember(inviteLink, qrForeground, qrBackground) {
                createQrBitmap(inviteLink, qrForeground, qrBackground)
            }
            if (qr != null) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = stringResource(R.string.group_invite_qr_description),
                    modifier = Modifier.size(GroupQrCodeSize)
                )
            } else {
                Text(stringResource(R.string.group_qr_unavailable), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(Spacing.space12))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                FilledTonalButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f).height(Sizes.ButtonHeightM3Medium)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(Spacing.space4))
                    Text(stringResource(R.string.group_copy_link), maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).height(Sizes.ButtonHeightM3Medium)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(Spacing.space4))
                    Text(stringResource(R.string.group_share_link), maxLines = 1)
                }
            }
            if (isAdmin) {
                TextButton(onClick = onRevoke, modifier = Modifier.padding(top = Spacing.space4)) {
                    Text(stringResource(R.string.group_revoke_regenerate))
                }
            }
        }
    }
}

@Composable
private fun PendingRequestBadge(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.space8)
        )
    }
}

@Composable
private fun GroupSettingsSection(
    isAdmin: Boolean,
    allowMemberSettingsEdits: Boolean,
    disappearingMode: DisappearingMode,
    onlyAdminsCanMessage: Boolean,
    requireJoinApproval: Boolean,
    allowMemberInvites: Boolean,
    memberCount: Int,
    onOpenDisappearing: () -> Unit,
    onToggleOnlyAdmins: (Boolean) -> Unit,
    onToggleMemberSettings: (Boolean) -> Unit,
    onToggleApproval: (Boolean) -> Unit,
    onToggleInvites: (Boolean) -> Unit,
    onViewAllParticipants: () -> Unit
) {
    val showOnlyAdmins = isAdmin || allowMemberSettingsEdits
    // The participants entry closes the group for every member, so it always counts as a row.
    val count = 2 + (if (showOnlyAdmins) 1 else 0) + (if (isAdmin) SETTINGS_ADMIN_TOGGLE_COUNT else 0)

    // Positions are resolved with plain (non-composable) code so a skipped row cannot desync the stack.
    val disappearingPos = settingsPositionAt(0, count)
    var slot = 1
    val onlyAdminsPos = if (showOnlyAdmins) settingsPositionAt(slot++, count) else SettingsItemPosition.Single
    val memberSettingsPos = if (isAdmin) settingsPositionAt(slot++, count) else SettingsItemPosition.Single
    val approvalPos = if (isAdmin) settingsPositionAt(slot++, count) else SettingsItemPosition.Single
    val invitesPos = if (isAdmin) settingsPositionAt(slot++, count) else SettingsItemPosition.Single
    val participantsPos = settingsPositionAt(slot, count)

    Column(verticalArrangement = Arrangement.spacedBy(ChatInfoMetrics.listItemGap)) {
        SettingsClickableItem(
            title = stringResource(R.string.group_disappearing_messages),
            subtitle = disappearingLabel(disappearingMode),
            imageVector = Icons.Filled.Timer,
            onClick = onOpenDisappearing,
            position = disappearingPos
        )
        if (showOnlyAdmins) {
            SettingsToggleItem(
                title = stringResource(R.string.group_only_admins_message),
                imageVector = Icons.Filled.Lock,
                checked = onlyAdminsCanMessage,
                onCheckedChange = onToggleOnlyAdmins,
                position = onlyAdminsPos
            )
        }
        if (isAdmin) {
            SettingsToggleItem(
                title = stringResource(R.string.group_allow_member_settings),
                imageVector = Icons.Filled.Tune,
                checked = allowMemberSettingsEdits,
                onCheckedChange = onToggleMemberSettings,
                position = memberSettingsPos
            )
            SettingsToggleItem(
                title = stringResource(R.string.group_require_approval),
                imageVector = Icons.Filled.HowToReg,
                checked = requireJoinApproval,
                onCheckedChange = onToggleApproval,
                position = approvalPos
            )
            SettingsToggleItem(
                title = stringResource(R.string.group_allow_invites),
                imageVector = Icons.Filled.Share,
                checked = allowMemberInvites,
                onCheckedChange = onToggleInvites,
                position = invitesPos
            )
        }
        SettingsNavigationItem(
            title = stringResource(R.string.group_view_all_participants, memberCount),
            subtitle = stringResource(R.string.group_participants_entry_support),
            imageVector = Icons.Filled.Groups,
            onClick = onViewAllParticipants,
            position = participantsPos
        )
    }
}

@Composable
internal fun GroupMemberRow(
    user: User,
    isAdmin: Boolean,
    isMe: Boolean,
    isCreator: Boolean,
    canManage: Boolean,
    position: SettingsItemPosition,
    onProfileClick: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember(user.uid) { mutableStateOf(false) }
    val title = if (isMe) stringResource(R.string.group_you) else userDisplayName(user)
    val role = when {
        isCreator -> stringResource(R.string.group_creator_admin)
        isAdmin -> stringResource(R.string.group_admin_role)
        else -> stringResource(R.string.group_member_role)
    }
    SettingsListItem(
        title = title,
        subtitle = role,
        position = position,
        onClick = onProfileClick,
        leadingContent = {
            UserAvatar(
                avatarUrl = user.avatar,
                size = ChatInfoMetrics.iconContainerSize,
                displayName = userDisplayName(user)
            )
        },
        trailingContent = {
            if (canManage && !isMe) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.group_member_actions)
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (isAdmin) R.string.group_demote_admin else R.string.group_promote_admin
                                    )
                                )
                            },
                            onClick = {
                                if (isAdmin) onDemote() else onPromote()
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.remove_member),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onRemove()
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DangerActionItem(
    icon: ImageVector,
    title: String,
    position: ItemPosition = ItemPosition.SINGLE,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(settingsItemShape(position))
            .clickable(interactionSource = interaction, onClick = onClick)
            .pressScale(interaction)
            .heightIn(min = ChatInfoMetrics.listItemMinHeight),
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(ChatInfoMetrics.iconContainerSize),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(ChatInfoMetrics.listIconSize)
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = AppColors.cardBackground)
    )
}

// endregion

// region Shared helpers

@Composable
internal fun SectionHeading(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = Spacing.space4, bottom = Spacing.space4)
    )
}

@Composable
private fun PressScaleTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier.pressScale(interaction),
        enabled = enabled,
        interactionSource = interaction,
        content = content
    )
}

@Composable
internal fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ChatInfoMetrics.cardCorner),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            PressScaleTextButton(onClick = onRetry) {
                Text(stringResource(R.string.chat_info_retry))
            }
        }
    }
}

@Composable
private fun JoinRequestsDialog(
    requests: List<GroupJoinRequest>,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_pending_dialog_title)) },
        text = {
            if (requests.isEmpty()) {
                Text(stringResource(R.string.group_no_pending_requests))
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = Sizes.HeightSheetContent)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space12)
                ) {
                    requests.forEach { request ->
                        Column {
                            Text(userDisplayName(request.user), style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = request.user.username?.let { "@$it" }
                                    ?: stringResource(R.string.group_member_request),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                                TextButton(onClick = { onApprove(request.id) }) {
                                    Text(stringResource(R.string.group_approve))
                                }
                                TextButton(onClick = { onDecline(request.id) }) {
                                    Text(
                                        text = stringResource(R.string.group_decline),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            PressScaleTextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        }
    )
}

private enum class ItemPosition { SINGLE, FIRST, MIDDLE, LAST }

private fun connectedButtonShape(position: ItemPosition): Shape = when (position) {
    ItemPosition.FIRST -> RoundedCornerShape(
        topStart = ChatInfoMetrics.listOuterCorner,
        bottomStart = ChatInfoMetrics.listOuterCorner,
        topEnd = ChatInfoMetrics.listInnerCorner,
        bottomEnd = ChatInfoMetrics.listInnerCorner
    )

    ItemPosition.MIDDLE -> RoundedCornerShape(ChatInfoMetrics.listInnerCorner)
    ItemPosition.LAST -> RoundedCornerShape(
        topStart = ChatInfoMetrics.listInnerCorner,
        bottomStart = ChatInfoMetrics.listInnerCorner,
        topEnd = ChatInfoMetrics.listOuterCorner,
        bottomEnd = ChatInfoMetrics.listOuterCorner
    )

    ItemPosition.SINGLE -> RoundedCornerShape(ChatInfoMetrics.listOuterCorner)
}

private fun settingsItemShape(position: ItemPosition): Shape = when (position) {
    ItemPosition.FIRST -> RoundedCornerShape(
        topStart = ChatInfoMetrics.listOuterCorner,
        topEnd = ChatInfoMetrics.listOuterCorner,
        bottomStart = ChatInfoMetrics.listInnerCorner,
        bottomEnd = ChatInfoMetrics.listInnerCorner
    )

    ItemPosition.MIDDLE -> RoundedCornerShape(ChatInfoMetrics.listInnerCorner)
    ItemPosition.LAST -> RoundedCornerShape(
        topStart = ChatInfoMetrics.listInnerCorner,
        topEnd = ChatInfoMetrics.listInnerCorner,
        bottomStart = ChatInfoMetrics.listOuterCorner,
        bottomEnd = ChatInfoMetrics.listOuterCorner
    )

    ItemPosition.SINGLE -> RoundedCornerShape(ChatInfoMetrics.listOuterCorner)
}

internal fun settingsPositionAt(index: Int, count: Int): SettingsItemPosition = when {
    count <= 1 -> SettingsItemPosition.Single
    index <= 0 -> SettingsItemPosition.Top
    index >= count - 1 -> SettingsItemPosition.Bottom
    else -> SettingsItemPosition.Middle
}

private fun itemPositionAt(index: Int, count: Int): ItemPosition = when {
    count <= 1 -> ItemPosition.SINGLE
    index <= 0 -> ItemPosition.FIRST
    index >= count - 1 -> ItemPosition.LAST
    else -> ItemPosition.MIDDLE
}

private fun createQrBitmap(value: String, foreground: Int, background: Int): Bitmap? = try {
    val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, QR_PIXEL_SIZE, QR_PIXEL_SIZE)
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        val x = index % matrix.width
        val y = index / matrix.width
        if (matrix[x, y]) foreground else background
    }
    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
} catch (_: Exception) {
    null
}

// endregion

private const val INVITE_BASE_URL = "https://synapse.chat/g/"
private const val GROUP_REPORT_REASON = "user_report"
private const val GROUP_NAME_MAX_LENGTH = 60
private const val GROUP_DESCRIPTION_MAX_LENGTH = 240
private const val DESCRIPTION_FIELD_MIN_LINES = 3
private const val SETTINGS_ADMIN_TOGGLE_COUNT = 3
private const val QR_PIXEL_SIZE = 420

/** Invite QR rendered inside the card; the shared [Sizes.QRCodeSize] token is dialog-sized. */
private val GroupQrCodeSize = 160.dp
