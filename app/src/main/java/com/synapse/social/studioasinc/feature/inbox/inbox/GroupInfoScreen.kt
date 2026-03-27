package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.foundation.clickable
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    chatId: String,
    groupName: String,
    onNavigateBack: () -> Unit,
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val onlyAdminsCanMessage by viewModel.onlyAdminsCanMessage.collectAsState()
    val currentUserId = viewModel.currentUserId

    var showAddDialog by remember { mutableStateOf(false) }
    var userIdToAdd by remember { mutableStateOf("") }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        viewModel.loadMembers(chatId)
    }

    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text(stringResource(R.string.leave_group)) },
            text = { Text(stringResource(R.string.leave_group_confirm)) },
            confirmButton = {
                TextButton(
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
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_member_title)) },
            text = {
                OutlinedTextField(
                    value = userIdToAdd,
                    onValueChange = { userIdToAdd = it },
                    label = { Text(stringResource(R.string.user_id_label)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addMember(userIdToAdd)
                        showAddDialog = false
                    },
                    enabled = userIdToAdd.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val currentIsAdmin = members.find { it.first.uid == currentUserId }?.second == true
                    if (currentIsAdmin) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(Spacing.Medium)
                )
            }

            val currentIsAdmin = members.find { it.first.uid == currentUserId }?.second == true
            if (currentIsAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.only_admins_can_message), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = onlyAdminsCanMessage,
                        onCheckedChange = { viewModel.toggleOnlyAdminsCanMessage(it) }
                    )
                }
                HorizontalDivider()
            }

            Text(
                text = "Members (${members.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            )

            if (isLoading && members.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(members, key = { it.first.uid }) { (user, isAdmin) ->
                        val isMe = user.uid == currentUserId
                        val currentIsAdmin = members.find { it.first.uid == currentUserId }?.second == true
                        GroupMemberItem(
                            user = user,
                            isAdmin = isAdmin,
                            isMe = isMe,
                            canRemove = currentIsAdmin && !isMe,
                            currentIsAdmin = currentIsAdmin,
                            onRemove = { viewModel.removeMember(user.uid) },
                            onPromote = { viewModel.promoteToAdmin(user.uid) },
                            onDemote = { viewModel.demoteAdmin(user.uid) }
                        )
                    }
                    item {
                        Button(
                            onClick = { showLeaveConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.Medium)
                        ) {
                            Text(stringResource(R.string.leave_group))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupMemberItem(
    user: User,
    isAdmin: Boolean,
    isMe: Boolean,
    canRemove: Boolean,
    currentIsAdmin: Boolean,
    onRemove: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatar ?: "",
            contentDescription = null,
            modifier = Modifier
                .size(Sizes.IconMassive)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = (user.displayName ?: user.username ?: "Unknown") + if (isMe) " (You)" else "",
                style = MaterialTheme.typography.bodyLarge
            )
            if (isAdmin) {
                Text(stringResource(R.string.role_admin), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (currentIsAdmin && !isMe) {
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!isAdmin) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.promote_to_admin)) },
                            onClick = {
                                onPromote()
                                showMenu = false
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.demote_admin)) },
                            onClick = {
                                onDemote()
                                showMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onRemove()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
