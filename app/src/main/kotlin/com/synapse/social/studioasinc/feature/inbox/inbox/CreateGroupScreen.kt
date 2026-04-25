package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.foundation.clickable
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val selectedUsers by viewModel.selectedUsers.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val successChatId by viewModel.successChatId.collectAsStateWithLifecycle()

    LaunchedEffect(successChatId) {
        successChatId?.let { onGroupCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_chat_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.createGroup() },
                        enabled = groupName.isNotBlank() && selectedUsers.isNotEmpty() && !isLoading
                    ) {
                        Text(stringResource(R.string.action_create))
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


            AnimatedVisibility(
                visible = selectedUsers.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
                ) {
                    items(selectedUsers, key = { it.uid }) { user ->
                        SelectedUserItem(
                            user = user,
                            onRemove = { viewModel.removeMember(user) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = groupName,
                onValueChange = { viewModel.updateGroupName(it) },
                label = { Text(stringResource(R.string.group_name_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Medium),
                singleLine = true
            )

            Text(
                text = stringResource(id = R.string.select_members, selectedUsers.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            )

            if (isLoading && users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(users, key = { it.uid }) { user ->
                        val isSelected = selectedUsers.any { it.uid == user.uid }
                        UserSelectionItem(
                            user = user,
                            isSelected = isSelected,
                            onClick = { viewModel.toggleUserSelection(user) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSelectionItem(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Text(
            text = user.displayName ?: user.username ?: "Unknown",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
private fun SelectedUserItem(
    user: User,
    onRemove: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(Sizes.AvatarLarge)
    ) {
        Box {
            AsyncImage(
                model = user.avatar ?: "",
                contentDescription = null,
                modifier = Modifier
                    .size(Sizes.AvatarDefault)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(Sizes.IconDefault)
                    .align(Alignment.TopEnd)
                    .offset(x = Spacing.Small, y = (-Spacing.ExtraSmall))
                    .clickable(onClick = onRemove)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.padding(Spacing.Tiny),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
        Text(
            text = user.displayName?.split(" ")?.firstOrNull() ?: user.username ?: "Unknown",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
