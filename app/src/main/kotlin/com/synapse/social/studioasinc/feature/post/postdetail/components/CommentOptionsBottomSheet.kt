package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.CommentAction
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.feature.shared.components.post.PostOption
import com.synapse.social.studioasinc.feature.shared.components.post.QuickAction
import com.synapse.social.studioasinc.feature.shared.components.post.OptionItem
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentOptionsBottomSheet(
    comment: CommentWithUser,
    isOwnComment: Boolean,
    isPostAuthor: Boolean,
    onDismiss: () -> Unit,
    onAction: (CommentAction) -> Unit
) {
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
                    label = "Reply",
                    onClick = {
                        onAction(CommentAction.Reply(comment.id, comment.userId))
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = Icons.Filled.ContentCopy,
                    label = "Copy",
                    onClick = {
                        onAction(CommentAction.Copy(comment.content))
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Share",
                    onClick = {
                        onAction(CommentAction.Share(comment.id, comment.content, comment.postId))
                        onDismiss()
                    }
                )
                if (isPostAuthor && !comment.isDeleted) {
                    QuickAction(
                        icon = Icons.Filled.Bookmark,
                        label = if (comment.isPinned) "Unpin" else "Pin",
                        onClick = {
                            onAction(CommentAction.Pin(comment.id, comment.postId))
                            onDismiss()
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small))

            val options = mutableListOf<PostOption>()
            if (isOwnComment && !comment.isDeleted) {
                options.add(PostOption("Edit", Icons.Filled.Edit) {
                    onAction(CommentAction.Edit(comment.id, comment.content))
                    onDismiss()
                })
                options.add(PostOption("Delete", Icons.Filled.Delete, isDangerous = true) {
                    onAction(CommentAction.Delete(comment.id))
                    onDismiss()
                })
            }
            if (!isOwnComment) {
                options.add(PostOption("Report", Icons.Filled.Report, isDangerous = true) {
                    onAction(CommentAction.Report(comment.id, "spam", null))
                    onDismiss()
                })
            }

            LazyColumn {
                items(options) { option ->
                    OptionItem(option = option)
                }
            }
        }
    }
}
