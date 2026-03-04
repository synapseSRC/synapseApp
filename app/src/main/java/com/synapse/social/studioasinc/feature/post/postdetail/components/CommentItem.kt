package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R

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
    isFirstInThread: Boolean = true,
    isLastReply: Boolean = false,
    showThreadLine: Boolean = false
) {
    val isLoading = loadingIds.contains(comment.id)
    val directReplies = replies.ifEmpty { repliesState[comment.id] ?: emptyList() }
    val isReply = depth > 0 || comment.parentCommentId != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Column: Avatar and Thread Line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp)
            ) {
                com.synapse.social.studioasinc.ui.components.CircularAvatar(
                    imageUrl = comment.user?.avatar,
                    contentDescription = "Avatar",
                    size = if (depth == 0) 40.dp else 32.dp,
                    onClick = { comment.userId?.let { onUserClick(it) } }
                )

                if (showThreadLine || directReplies.isNotEmpty() || comment.repliesCount > 0) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Column: Content
            Column(modifier = Modifier.weight(1f)) {
                // Header: User Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = comment.user?.displayName ?: "User",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { comment.userId?.let { onUserClick(it) } }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "@${comment.user?.username ?: "user"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = " · ${TimeUtils.getTimeAgo(comment.createdAt ?: "") ?: "now"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = { onShowOptions(comment) },
                        modifier = Modifier.size(20.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Replying to context
                if (isReply) {
                    Text(
                        text = "Replying to ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Note: Ideally we'd have the parent username here. 
                    // For now, using a placeholder or just showing the context.
                }

                // Content
                val context = LocalContext.current
                val colorOnSurface = MaterialTheme.colorScheme.onSurface
                AndroidView(
                    modifier = Modifier.padding(vertical = 4.dp),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(colorOnSurface.toArgb())
                            textSize = 15f
                        }
                    },
                    update = { textView ->
                        MarkdownRenderer.get(context).render(textView, comment.content)
                        textView.setTextColor(colorOnSurface.toArgb())
                    }
                )

                // Actions Bar (X-style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reply Action
                    CommentActionItem(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        count = comment.repliesCount,
                        onClick = { onReplyClick(comment) },
                        contentDescription = "Reply"
                    )

                    // Like Action
                    val isLiked = comment.userReaction != null
                    CommentActionItem(
                        icon = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        count = comment.likesCount,
                        color = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onLikeClick(comment.id) },
                        onLongClick = { onShowReactions(comment) },
                        contentDescription = "Like"
                    )

                    // Share
                    IconButton(onClick = { /* Share Logic */ }, modifier = Modifier.size(20.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Expanded Replies
        if (directReplies.isNotEmpty()) {
            directReplies.forEachIndexed { index, reply ->
                CommentItem(
                    comment = reply,
                    replies = emptyList(),
                    repliesState = emptyMap(),
                    depth = depth + 1,
                    isRepliesLoading = false,
                    loadingIds = loadingIds,
                    onReplyClick = onReplyClick,
                    onLikeClick = onLikeClick,
                    onShowReactions = onShowReactions,
                    onShowOptions = onShowOptions,
                    onUserClick = onUserClick,
                    onViewReplies = {},
                    showThreadLine = index < directReplies.lastIndex || comment.repliesCount > directReplies.size
                )
            }
        }

        if (comment.repliesCount > directReplies.size && !isRepliesLoading) {
            Text(
                text = "Show more replies",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 68.dp, top = 4.dp, bottom = 12.dp)
                    .clickable(onClick = onViewReplies)
            )
        }

        if (isRepliesLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(start = 68.dp, top = 4.dp, bottom = 12.dp)
                    .size(20.dp),
                strokeWidth = 2.dp
            )
        }

        if (depth == 0) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun CommentActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
