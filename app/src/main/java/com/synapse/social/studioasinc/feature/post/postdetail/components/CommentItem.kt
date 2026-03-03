package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import com.synapse.social.studioasinc.core.util.TimeUtils
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import com.synapse.social.studioasinc.styling.MarkdownRenderer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import android.text.TextUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentItem(
    comment: CommentWithUser,
    replies: List<CommentWithUser> = emptyList(),
    repliesState: Map<String, List<CommentWithUser>> = emptyMap(),
    depth: Int = 0,
    isRepliesLoading: Boolean = false,
    loadingIds: Set<String> = emptySet(),
    onReplyClick: (CommentWithUser) -> Unit,
    onLikeClick: (String) -> Unit,
    onShowReactions: (CommentWithUser) -> Unit,
    onShowOptions: (CommentWithUser) -> Unit,
    onUserClick: (String) -> Unit,
    onViewReplies: () -> Unit = {},
    modifier: Modifier = Modifier,
    isLastReply: Boolean = false
) {
    val isLoading = loadingIds.contains(comment.id)
    var showMentionDialogForUser by remember { mutableStateOf<String?>(null) }

    val directReplies = replies.ifEmpty { repliesState[comment.id] ?: emptyList() }
    val isReply = depth > 0

    if (showMentionDialogForUser != null) {
        AlertDialog(
            onDismissRequest = { showMentionDialogForUser = null },
            title = { Text("Open Profile") },
            text = { Text("Are you sure you want to open the account @${showMentionDialogForUser}?") },
            confirmButton = {
                TextButton(onClick = {
                    onUserClick(showMentionDialogForUser!!)
                    showMentionDialogForUser = null
                }) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMentionDialogForUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isReply) {
                        Modifier
                            .drawBehind {
                                val lineColor = Color.Gray.copy(alpha = 0.3f)
                                val lineWidth = 2.dp.toPx()
                                val startX = 0f
                                val avatarCenterY = 24.dp.toPx()


                                drawLine(
                                    color = lineColor,
                                    start = Offset(startX, 0f),
                                    end = Offset(startX, avatarCenterY),
                                    strokeWidth = lineWidth
                                )


                                drawLine(
                                    color = lineColor,
                                    start = Offset(startX, avatarCenterY),
                                    end = Offset(startX + 32.dp.toPx(), avatarCenterY),
                                    strokeWidth = lineWidth
                                )


                                if (!isLastReply) {
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(startX, avatarCenterY),
                                        end = Offset(startX, size.height),
                                        strokeWidth = lineWidth
                                    )
                                }
                            }
                            .padding(start = 32.dp)
                    } else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            CircularAvatar(
                imageUrl = comment.user?.avatar,
                contentDescription = "Avatar",
                size = 32.dp,
                onClick = { comment.userId?.let { onUserClick(it) } }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.user?.username ?: "User",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { comment.userId?.let { onUserClick(it) } }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TimeUtils.getTimeAgo(comment.createdAt ?: "") ?: "Just now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val context = LocalContext.current
                val colorOnSurface = MaterialTheme.colorScheme.onSurface
                AndroidView(
                    modifier = Modifier.padding(vertical = 4.dp),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(colorOnSurface.toArgb())
                            textSize = 14f
                        }
                    },
                    update = { textView ->
                        MarkdownRenderer.get(context).render(textView, comment.content)
                        textView.setTextColor(colorOnSurface.toArgb())
                        textView.maxLines = 10
                        textView.ellipsize = TextUtils.TruncateAt.END
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onReplyClick(comment) }
                            .padding(end = 16.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.combinedClickable(
                            onClick = { onLikeClick(comment.id) },
                            onLongClick = { onShowReactions(comment) }
                        )
                    ) {
                        val userReaction = comment.userReaction
                        if (userReaction != null) {
                            Image(
                                painter = painterResource(id = userReaction.iconRes),
                                contentDescription = userReaction.displayName,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (userReaction == ReactionType.LIKE) "Like" else userReaction.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                        } else {
                            Icon(
                                imageVector = Icons.Default.ThumbUpOffAlt,
                                contentDescription = "Like",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Like",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (comment.likesCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = comment.likesCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }


                if (comment.repliesCount > 0 && directReplies.isEmpty() && !isRepliesLoading) {
                    Text(
                        text = "View ${comment.repliesCount} replies",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable(onClick = onViewReplies)
                    )
                }

                 if (isRepliesLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            IconButton(
                onClick = { onShowOptions(comment) },
                modifier = Modifier.size(24.dp),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )


        if (directReplies.isNotEmpty() && depth == 0) {
            Column(modifier = Modifier.padding(start = 48.dp)) {
                directReplies.forEachIndexed { index, reply ->
                    CommentItem(
                        comment = reply,
                        replies = emptyList(),
                        repliesState = emptyMap(),
                        depth = 1,
                        isRepliesLoading = false,
                        loadingIds = loadingIds,
                        onReplyClick = onReplyClick,
                        onLikeClick = onLikeClick,
                        onShowReactions = onShowReactions,
                        onShowOptions = onShowOptions,
                        onUserClick = onUserClick,
                        onViewReplies = {},
                        isLastReply = index == directReplies.lastIndex
                    )
                }
            }
        }
    }
}
