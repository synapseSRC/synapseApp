@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.synapse.social.studioasinc.feature.inbox.inbox

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.UserStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinRequest
import kotlinx.coroutines.launch

private const val FLOW_MEMBERS = 0
private const val FLOW_IDENTITY = 1
private const val FLOW_PERMISSIONS = 2
private const val FLOW_HUB = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val selectedUsers by viewModel.selectedUsers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val groupDescription by viewModel.groupDescription.collectAsStateWithLifecycle()
    val iconConcept by viewModel.iconConcept.collectAsStateWithLifecycle()
    val purpose by viewModel.purpose.collectAsStateWithLifecycle()
    val options by viewModel.options.collectAsStateWithLifecycle()
    val preset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val loadingUsers by viewModel.isLoadingUsers.collectAsStateWithLifecycle()
    val creating by viewModel.isCreating.collectAsStateWithLifecycle()
    val generating by viewModel.isGeneratingIdentity.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val createdChatId by viewModel.createdChatId.collectAsStateWithLifecycle()
    var stage by rememberSaveable { mutableStateOf(FLOW_MEMBERS) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    fun goBack() {
        if (stage == FLOW_MEMBERS) onNavigateBack()
        else stage = if (stage == FLOW_HUB) FLOW_IDENTITY else stage - 1
    }

    BackHandler(enabled = stage != FLOW_MEMBERS, onBack = ::goBack)
    LaunchedEffect(createdChatId) {
        if (createdChatId != null) stage = FLOW_HUB
    }

    GroupCreationTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (stage) {
                                FLOW_MEMBERS -> stringResource(R.string.group_select_members_title)
                                FLOW_IDENTITY -> stringResource(R.string.group_identity_title)
                                FLOW_PERMISSIONS -> stringResource(R.string.group_permissions_title)
                                else -> groupName.ifBlank { stringResource(R.string.group_hub_title) }
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = ::goBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (stage == FLOW_MEMBERS) {
                            IconButton(onClick = { scope.launch { focusRequester.requestFocus() } }) {
                                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                when (stage) {
                    FLOW_MEMBERS -> FloatingActionButton(
                        onClick = { if (selectedUsers.isNotEmpty()) stage = FLOW_IDENTITY },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = stringResource(R.string.group_continue_identity))
                    }
                    FLOW_HUB -> FloatingActionButton(
                        onClick = { createdChatId?.let(onGroupCreated) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = stringResource(R.string.group_open_chat))
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) { scaffoldPadding ->
            AnimatedContent(
                targetState = stage,
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
                transitionSpec = {
                    if (targetState == FLOW_HUB || initialState == FLOW_HUB) {
                        fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                    } else {
                        val movingForward = targetState > initialState
                        (slideInHorizontally(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) { width -> if (movingForward) width else -width } +
                            fadeIn(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) { width -> if (movingForward) -width else width } +
                                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)))
                    }
                },
                label = "groupCreationStage"
            ) { visibleStage ->
                when (visibleStage) {
                    FLOW_MEMBERS -> MembersStage(
                        users = users,
                        selectedUsers = selectedUsers,
                        searchQuery = searchQuery,
                        isLoading = loadingUsers,
                        error = error,
                        focusRequester = focusRequester,
                        onSearch = viewModel::onSearchQueryChange,
                        onToggleUser = viewModel::toggleUserSelection,
                        onRemoveUser = viewModel::removeMember,
                        onClearError = viewModel::clearError
                    )
                    FLOW_IDENTITY -> IdentityStage(
                        selectedCount = selectedUsers.size,
                        preset = preset,
                        hasCreatedGroup = createdChatId != null,
                        purpose = purpose,
                        groupName = groupName,
                        description = groupDescription,
                        iconConcept = iconConcept,
                        permissions = options,
                        isGenerating = generating,
                        isCreating = creating,
                        error = error,
                        onPurposeChange = viewModel::updatePurpose,
                        onGroupNameChange = viewModel::updateGroupName,
                        onDescriptionChange = viewModel::updateGroupDescription,
                        onRegenerate = viewModel::generateIdentity,
                        onOpenPermissions = { stage = FLOW_PERMISSIONS },
                        onCreate = viewModel::createGroup,
                        onReturnToHub = { stage = FLOW_HUB },
                        onClearError = viewModel::clearError
                    )
                    FLOW_PERMISSIONS -> PermissionsStage(
                        options = options,
                        preset = preset,
                        onSelectPreset = viewModel::selectPreset,
                        onSendMessagesChange = viewModel::updateSendMessages,
                        onMemberSettingsChange = viewModel::updateMemberSettingsEdits,
                        onApprovalChange = viewModel::updateJoinApproval,
                        onInvitesChange = viewModel::updateMemberInvites,
                        onDisappearingChange = viewModel::updateDisappearingMode,
                        error = error,
                        onSave = { stage = FLOW_IDENTITY },
                        onClearError = viewModel::clearError
                    )
                    else -> GroupHubStage(
                        groupName = groupName,
                        inviteCode = viewModel.inviteCode.collectAsStateWithLifecycle().value,
                        requireJoinApproval = options.requireJoinApproval,
                        members = viewModel.members.collectAsStateWithLifecycle().value,
                        requests = viewModel.pendingRequests.collectAsStateWithLifecycle().value,
                        requestsLoading = viewModel.isLoadingRequests.collectAsStateWithLifecycle().value,
                        inviteUpdating = viewModel.isUpdatingInvite.collectAsStateWithLifecycle().value,
                        currentUserId = viewModel.currentUserId,
                        error = error,
                        onRegenerateInvite = viewModel::regenerateInvite,
                        onOpenRequests = viewModel::refreshPendingRequests,
                        onApproveRequest = viewModel::approveRequest,
                        onDeclineRequest = viewModel::declineRequest,
                        onChangeRole = viewModel::changeMemberRole,
                        onRemoveMember = viewModel::removeGroupMember,
                        onClearError = viewModel::clearError
                    )
                }
            }
        }
    }
}

