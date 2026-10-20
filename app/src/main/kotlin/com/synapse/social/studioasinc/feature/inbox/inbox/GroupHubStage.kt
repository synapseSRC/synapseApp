@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.synapse.social.studioasinc.feature.inbox.inbox

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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

@Composable
internal fun GroupHubStage(
    groupName: String,
    inviteCode: String?,
    requireJoinApproval: Boolean,
    members: List<Pair<User, Boolean>>,
    requests: List<GroupJoinRequest>,
    requestsLoading: Boolean,
    inviteUpdating: Boolean,
    currentUserId: String?,
    error: GroupCreationError?,
    onRegenerateInvite: () -> Unit,
    onOpenRequests: () -> Unit,
    onApproveRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onChangeRole: (String, Boolean) -> Unit,
    onRemoveMember: (String) -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showRevokeDialog by remember { mutableStateOf(false) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<User?>(null) }
    val inviteLink = inviteCode?.let { INVITE_BASE_URL + it }

    if (showRevokeDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = false },
            title = { Text(stringResource(R.string.group_revoke_title)) },
            text = { Text(stringResource(R.string.group_revoke_body)) },
            confirmButton = {
                TextButton(onClick = { showRevokeDialog = false; onRegenerateInvite() }) {
                    Text(stringResource(R.string.group_revoke_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { showRevokeDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showRequestsDialog) {
        JoinRequestsDialog(
            requests = requests,
            isLoading = requestsLoading,
            onApprove = onApproveRequest,
            onDecline = onDeclineRequest,
            onDismiss = { showRequestsDialog = false }
        )
    }
    memberToRemove?.let { user ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text(stringResource(R.string.group_remove_member_title)) },
            text = { Text(stringResource(R.string.group_remove_member_body, userDisplayName(user))) },
            confirmButton = {
                TextButton(onClick = { onRemoveMember(user.uid); memberToRemove = null }) {
                    Text(stringResource(R.string.remove_member), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space12),
        verticalArrangement = Arrangement.spacedBy(Spacing.space12)
    ) {
        if (error != null) item { GroupErrorMessage(error, onClearError) }
        item {
            OutlinedCard(shape = MaterialTheme.shapes.largeIncreased, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(Spacing.space16), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.group_invite_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.space4))
                    Text(
                        text = inviteLink ?: stringResource(R.string.group_invite_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (inviteLink != null) {
                        Text(stringResource(if (requireJoinApproval) R.string.group_admin_approval_enabled else R.string.group_admin_approval_disabled), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(Spacing.space12))
                        val qrForeground = MaterialTheme.colorScheme.onSurface.toArgb()
                        val qrBackground = MaterialTheme.colorScheme.surface.toArgb()
                        val qr = remember(inviteLink, qrForeground, qrBackground) {
                            createQrBitmap(inviteLink, qrForeground, qrBackground)
                        }
                        if (qr != null) Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = stringResource(R.string.group_invite_qr_description),
                            modifier = Modifier.size(GroupCreationDimensions.qrCodeSize)
                        ) else Text(stringResource(R.string.group_qr_unavailable), color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(Spacing.space12))
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                            FilledTonalButton(
                                onClick = { clipboard.setText(AnnotatedString(inviteLink)); android.widget.Toast.makeText(context, R.string.group_link_copied, android.widget.Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.weight(1f).height(Sizes.ButtonHeightM3Medium)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(Spacing.space4))
                                Text(stringResource(R.string.group_copy_link))
                            }
                            FilledTonalButton(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, inviteLink)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.group_share_link)))
                                },
                                modifier = Modifier.weight(1f).height(Sizes.ButtonHeightM3Medium)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null)
                                Spacer(Modifier.width(Spacing.space4))
                                Text(stringResource(R.string.group_share_link))
                            }
                        }
                    }
                    TextButton(onClick = { showRevokeDialog = true }, enabled = !inviteUpdating, modifier = Modifier.padding(top = Spacing.space4)) {
                        if (inviteUpdating) CircularProgressIndicator(modifier = Modifier.size(Sizes.IconDefault), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(Spacing.space4))
                        Text(stringResource(R.string.group_revoke_regenerate))
                    }
                }
            }
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.group_pending_requests, requests.size)) },
                supportingContent = { Text(stringResource(R.string.group_pending_requests_support), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingContent = { Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onOpenRequests(); showRequestsDialog = true }
            )
        }
        item {
            Text(stringResource(R.string.group_participants_count, members.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.space8))
            Text(stringResource(R.string.group_participants_support), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (members.isEmpty()) {
            item {
                Card(shape = MaterialTheme.shapes.largeIncreased, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.group_members_unavailable), modifier = Modifier.padding(Spacing.space20), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(members, key = { it.first.uid }) { (user, isAdmin) ->
                val isMe = user.uid == currentUserId
                var menuExpanded by remember(user.uid) { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(if (isMe) stringResource(R.string.group_you) else userDisplayName(user)) },
                    supportingContent = {
                        Text(
                            text = if (isMe) stringResource(R.string.group_creator_admin) else if (isAdmin) stringResource(R.string.group_admin_role) else stringResource(R.string.group_member_role),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(if (isMe) Icons.Filled.Star else Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        if (!isMe && members.any { it.first.uid == currentUserId && it.second }) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.group_member_actions)) }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(if (isAdmin) R.string.group_demote_admin else R.string.group_promote_admin)) },
                                        onClick = { onChangeRole(user.uid, !isAdmin); menuExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.remove_member), color = MaterialTheme.colorScheme.error) },
                                        onClick = { memberToRemove = user; menuExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
        item { Spacer(Modifier.height(Spacing.space64)) }
    }
}

@Composable
private fun JoinRequestsDialog(
    requests: List<GroupJoinRequest>,
    isLoading: Boolean,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_pending_dialog_title)) },
        text = {
            when {
                isLoading -> Box(Modifier.fillMaxWidth().padding(Spacing.space16), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                requests.isEmpty() -> Text(stringResource(R.string.group_no_pending_requests))
                else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                    requests.forEach { request ->
                        Column {
                            Text(userDisplayName(request.user), style = MaterialTheme.typography.titleSmall)
                            Text(request.user.username?.let { "@$it" } ?: stringResource(R.string.group_member_request), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                                TextButton(onClick = { onApprove(request.id) }) { Text(stringResource(R.string.group_approve)) }
                                TextButton(onClick = { onDecline(request.id) }) { Text(stringResource(R.string.group_decline), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } }
    )
}

private fun createQrBitmap(value: String, foreground: Int, background: Int): Bitmap? = try {
    val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 420, 420)
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


private const val INVITE_BASE_URL = "https://synapse.chat/g/"
