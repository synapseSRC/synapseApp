package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

data class PostOption(
    val label: String,
    val icon: ImageVector,
    val isDangerous: Boolean = false,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    post: Post,
    isOwner: Boolean,
    commentsDisabled: Boolean = false,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToThread: (() -> Unit)? = null,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onBookmark: () -> Unit,
    onReshare: () -> Unit = {},
    onToggleComments: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onRevokeVote: () -> Unit,
    onSummarize: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.Medium)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAction(
                    icon = Icons.AutoMirrored.Filled.Reply,
                    label = "Reshare",
                    onClick = {
                        onReshare()
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Share",
                    onClick = {
                        onShare()
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = Icons.Filled.Link,
                    label = "Copy Link",
                    onClick = {
                        onCopyLink()
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = Icons.Filled.Bookmark,
                    label = "Save",
                    onClick = {
                        onBookmark()
                        onDismiss()
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small))

            val options = buildOptions(
                isOwner = isOwner,
                commentsDisabled = commentsDisabled,
                post = post,
                onEdit = { onEdit(); onDismiss() },
                onDelete = { showDeleteDialog = true },
                onAddToThread = { onAddToThread?.invoke(); onDismiss() },
                onToggleComments = { onToggleComments(); onDismiss() },
                onReport = { onReport(); onDismiss() },
                onBlock = { onBlock(); onDismiss() },
                onShare = { onShare(); onDismiss() },
                onRevokeVote = { onRevokeVote(); onDismiss() },
                onSummarize = { onSummarize(); onDismiss() }
            )

            LazyColumn {
                items(options) { option ->
                    OptionItem(option = option)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_post_title)) },
            text = { Text(stringResource(R.string.delete_post_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
internal fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(Sizes.HeightDefault)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(Sizes.IconLarge)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
internal fun OptionItem(option: PostOption) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = option.action)
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.label,
            tint = if (option.isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(Sizes.IconLarge)
        )
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (option.isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun buildOptions(
    isOwner: Boolean,
    commentsDisabled: Boolean,
    post: Post,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToThread: () -> Unit,
    onToggleComments: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onShare: () -> Unit,
    onRevokeVote: () -> Unit,
    onSummarize: () -> Unit
): List<PostOption> {
    val options = mutableListOf<PostOption>()

    if (isOwner) {
        options.add(PostOption("Add to thread", Icons.AutoMirrored.Filled.Reply, action = onAddToThread))
        options.add(PostOption(stringResource(R.string.edit), Icons.Filled.EditNote, action = onEdit))
        options.add(PostOption(
            if (commentsDisabled) stringResource(R.string.turn_on_commenting) else stringResource(R.string.turn_off_commenting),
            Icons.Filled.CommentsDisabled,
            action = onToggleComments
        ))
        options.add(PostOption(stringResource(R.string.delete), Icons.Filled.Delete, isDangerous = true, action = onDelete))
    } else {
        options.add(PostOption(stringResource(R.string.report), Icons.Filled.Report, isDangerous = true, action = onReport))
        options.add(PostOption(stringResource(R.string.block_user), Icons.Filled.Block, isDangerous = true, action = onBlock))
    }

    if (post.userPollVote != null) {
        options.add(PostOption(stringResource(R.string.revoke_vote), Icons.Filled.Delete, action = onRevokeVote))
    }

    options.add(PostOption(stringResource(R.string.action_ai_summary), Icons.Filled.AutoAwesome, action = onSummarize))
    options.add(PostOption(stringResource(R.string.share_via), Icons.AutoMirrored.Filled.Send, action = onShare))

    return options
}
