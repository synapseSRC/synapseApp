package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.CommentAction
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.feature.shared.components.post.PostOption
import com.synapse.social.studioasinc.feature.shared.components.post.QuickAction
import com.synapse.social.studioasinc.feature.shared.components.post.OptionItem

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
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAction(
                    icon = R.drawable.ic_reply,
                    label = "Reply",
                    onClick = {
                        onAction(CommentAction.Reply(comment.id, comment.userId))
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = R.drawable.ic_content_copy_48px,
                    label = "Copy",
                    onClick = {
                        onAction(CommentAction.Copy(comment.content))
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = R.drawable.ic_send,
                    label = "Share",
                    onClick = {
                        onAction(CommentAction.Share(comment.id, comment.content, comment.postId))
                        onDismiss()
                    }
                )
                if (isPostAuthor && !comment.isDeleted) {
                    QuickAction(
                        icon = R.drawable.ic_bookmark,
                        label = if (comment.isPinned) "Unpin" else "Pin",
                        onClick = {
                            onAction(CommentAction.Pin(comment.id, comment.postId))
                            onDismiss()
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val options = mutableListOf<PostOption>()
            if (isOwnComment && !comment.isDeleted) {
                options.add(PostOption("Edit", R.drawable.ic_edit_note_48px) {
                    onAction(CommentAction.Edit(comment.id, comment.content))
                    onDismiss()
                })
                options.add(PostOption("Delete", R.drawable.ic_delete_48px, isDangerous = true) {
                    onAction(CommentAction.Delete(comment.id))
                    onDismiss()
                })
            }
            if (!isOwnComment) {
                options.add(PostOption("Report", R.drawable.ic_report_48px, isDangerous = true) {
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
