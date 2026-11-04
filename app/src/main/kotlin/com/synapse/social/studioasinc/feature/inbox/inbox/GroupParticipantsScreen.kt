@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.ChatInfoMetrics
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.User

/**
 * Full participant roster for a group, on its own screen so the list can grow arbitrarily long
 * without crowding [GroupInfoScreen], which only previews a few members inline.
 *
 * Search, role changes and removal all reuse [GroupInfoViewModel] — the same business logic the
 * info screen drives — with a lighter load that skips invite, mute and request data. Rows stack as
 * one contained group, and the positions are derived from the filtered list so the grouping holds
 * while the user is searching.
 */
@Composable
fun GroupParticipantsScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(chatId) { viewModel.loadParticipants(chatId) }

    val errorRes = uiState.errorRes
    LaunchedEffect(errorRes) {
        if (errorRes != null) {
            snackbarHostState.showSnackbar(context.getString(errorRes))
            viewModel.clearError()
        }
    }

    val members = uiState.members
    val filteredMembers = uiState.filteredMembers
    val isAdmin = uiState.currentUserIsAdmin
    val currentUserId = uiState.currentUserId
    val createdBy = uiState.groupInfo?.createdBy
    var memberToRemove by remember { mutableStateOf<User?>(null) }

    Scaffold(
        containerColor = AppColors.screenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_participants_count, members.size)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        if (uiState.isLoading && members.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space16),
                verticalArrangement = Arrangement.spacedBy(ChatInfoMetrics.listItemGap)
            ) {
                item(key = "member-search") {
                    OutlinedTextField(
                        value = uiState.memberSearchQuery,
                        onValueChange = { viewModel.onMemberSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.space12),
                        shape = MaterialTheme.shapes.extraLarge,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.group_search_members)) },
                        singleLine = true
                    )
                }

                if (filteredMembers.isEmpty()) {
                    item(key = "members-empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = AppColors.cardBackground
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(Spacing.space16),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.space8)
                            ) {
                                Text(
                                    text = stringResource(R.string.group_no_results_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.group_no_results_body, uiState.memberSearchQuery),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(items = filteredMembers, key = { _, pair -> pair.first.uid }) { index, pair ->
                        val user = pair.first
                        GroupMemberRow(
                            user = user,
                            isAdmin = pair.second,
                            isMe = user.uid == currentUserId,
                            isCreator = user.uid == createdBy,
                            canManage = isAdmin,
                            position = settingsPositionAt(index, filteredMembers.size),
                            onProfileClick = { onNavigateToProfile(user.uid) },
                            onPromote = { viewModel.promoteToAdmin(user.uid) },
                            onDemote = { viewModel.demoteAdmin(user.uid) },
                            onRemove = { memberToRemove = user }
                        )
                    }
                }
            }
        }
    }

    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text(stringResource(R.string.group_remove_member_title)) },
            text = { Text(stringResource(R.string.group_remove_member_body, userDisplayName(member))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(member.uid)
                        memberToRemove = null
                    }
                ) {
                    Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
