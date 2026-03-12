package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.paging.LoadState
import androidx.paging.compose.itemKey
import androidx.paging.compose.LazyPagingItems
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.CommentAction
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.feature.shared.components.post.PostCard
import com.synapse.social.studioasinc.feature.shared.components.post.PostUiMapper
import com.synapse.social.studioasinc.domain.model.ReactionType

@Composable
fun CommentsList(
    comments: LazyPagingItems<CommentWithUser>,
    repliesState: Map<String, List<CommentWithUser>> = emptyMap(),
    replyLoadingState: Set<String> = emptySet(),
    commentActionsLoading: Set<String> = emptySet(),
    onReplyClick: (CommentWithUser) -> Unit,
    onLikeClick: (String) -> Unit,
    onViewReplies: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onShowReactions: (CommentWithUser) -> Unit,
    onShowOptions: (CommentWithUser) -> Unit,
    onUserClick: (String) -> Unit,
    onShareClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {}
) {

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            headerContent()
        }

        if (comments.loadState.refresh is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                        contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            }
        }

        if (comments.loadState.refresh is LoadState.Error) {
             item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.synapse.social.studioasinc.R.string.loading_posts_error))
                }
            }
        }

        if (comments.itemCount == 0 && comments.loadState.refresh !is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.synapse.social.studioasinc.R.string.no_comments))
                }
            }
        }

        items(comments.itemCount) { index ->
            val comment = comments[index]
            if (comment != null) {
                val replies = repliesState[comment.id] ?: emptyList()
                val hasReplies = comment.repliesCount > 0 || replies.isNotEmpty()
                val isLastComment = index == comments.itemCount - 1
                
                // X (Twitter) style: Nesting with thread lines
                val postCardState = PostUiMapper.toPostCardState(
                    comment = comment,
                    parentAuthorUsername = null,
                    depth = 0,
                    showThreadLine = hasReplies,
                    isThreadChild = false,
                    isLastReply = false
                )
                
                Column {
                    PostCard(
                        state = postCardState,
                        onLikeClick = { onLikeClick(comment.id) },
                        onCommentClick = { onReplyClick(comment) },
                        onShareClick = { onShareClick?.invoke(comment.id) },
                        onRepostClick = { /* Not applicable for comments */ },
                        onBookmarkClick = { /* Not applicable for comments */ },
                        onUserClick = { comment.userId?.let { onUserClick(it) } },
                        onPostClick = { onCommentClick(comment.id) },
                        onMediaClick = { /* No media in comments */ },
                        onOptionsClick = { onShowOptions(comment) },
                        onPollVote = { /* No polls in comments */ },
                        onReactionSelected = { reaction -> onShowReactions(comment) },
                        onQuoteClick = { },
                        modifier = Modifier
                    )
                    
                    // Render nested replies
                    if (replies.isNotEmpty()) {
                        RenderReplies(
                            replies = replies,
                            parentComment = comment,
                            repliesState = repliesState,
                            replyLoadingState = replyLoadingState,
                            depth = 1,
                            onReplyClick = onReplyClick,
                            onLikeClick = onLikeClick,
                            onShowReactions = onShowReactions,
                            onShowOptions = onShowOptions,
                            onUserClick = onUserClick,
                            onShareClick = onShareClick,
                            onViewReplies = onViewReplies,
                            onCommentClick = onCommentClick
                        )
                    }
                    
                    // Show "Show more replies" button
                    if (comment.repliesCount > replies.size && !replyLoadingState.contains(comment.id)) {
                        val remainingReplies = comment.repliesCount - replies.size
                        Text(
                            text = "View $remainingReplies replies",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 68.dp, top = 4.dp, bottom = 12.dp)
                                .clickable { onViewReplies(comment.id) }
                        )
                    }
                    
                    // Show loading indicator for replies
                    if (replyLoadingState.contains(comment.id)) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = 68.dp, top = 4.dp, bottom = 12.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        if (comments.loadState.append is LoadState.Loading) {
             item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun RenderReplies(
    replies: List<CommentWithUser>,
    parentComment: CommentWithUser,
    repliesState: Map<String, List<CommentWithUser>>,
    replyLoadingState: Set<String>,
    depth: Int,
    onReplyClick: (CommentWithUser) -> Unit,
    onLikeClick: (String) -> Unit,
    onShowReactions: (CommentWithUser) -> Unit,
    onShowOptions: (CommentWithUser) -> Unit,
    onUserClick: (String) -> Unit,
    onShareClick: ((String) -> Unit)?,
    onViewReplies: (String) -> Unit,
    onCommentClick: (String) -> Unit
) {
    replies.forEachIndexed { replyIndex, reply ->
        val nestedReplies = repliesState[reply.id] ?: emptyList()
        val isLastInBranch = replyIndex == replies.size - 1
        
        val hasChildren = reply.repliesCount > 0 || nestedReplies.isNotEmpty()
        val isFirstReply = replyIndex == 0

        // X (Twitter) style: First reply connects to parent. Subsequent replies start new blocks.
        val replyState = PostUiMapper.toPostCardState(
            comment = reply,
            parentAuthorUsername = parentComment.getUsername(),
            depth = depth,
            showThreadLine = hasChildren,
            isThreadChild = isFirstReply,
            isLastReply = isLastInBranch && !hasChildren
        )
        
        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Vertical connector line for visual indentation
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 24.dp)
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                PostCard(
                    state = replyState,
                    onLikeClick = { onLikeClick(reply.id) },
                    onCommentClick = { onReplyClick(reply) },
                    onShareClick = { onShareClick?.invoke(reply.id) },
                    onRepostClick = { /* Not applicable for comments */ },
                    onBookmarkClick = { /* Not applicable for comments */ },
                    onUserClick = { reply.userId?.let { onUserClick(it) } },
                    onPostClick = { onCommentClick(reply.id) },
                    onMediaClick = { /* No media in comments */ },
                    onOptionsClick = { onShowOptions(reply) },
                    onPollVote = { /* No polls in comments */ },
                    onReactionSelected = { reaction -> onShowReactions(reply) },
                    onQuoteClick = { },
                    onParentAuthorClick = { parentComment.userId?.let { onUserClick(it) } },
                    modifier = Modifier
                )
            }
            
            // X (Twitter) style: Render nested replies with increased depth
            if (nestedReplies.isNotEmpty()) {
                RenderReplies(
                    replies = nestedReplies,
                    parentComment = reply,
                    repliesState = repliesState,
                    replyLoadingState = replyLoadingState,
                    depth = depth + 1,
                    onReplyClick = onReplyClick,
                    onLikeClick = onLikeClick,
                    onShowReactions = onShowReactions,
                    onShowOptions = onShowOptions,
                    onUserClick = onUserClick,
                    onShareClick = onShareClick,
                    onViewReplies = onViewReplies,
                    onCommentClick = onCommentClick
                )
            }
            
            // Show "Show more replies" button aligned with text content
            val basePadding = 68.dp
            
            if (reply.repliesCount > nestedReplies.size && !replyLoadingState.contains(reply.id)) {
                val remainingReplies = reply.repliesCount - nestedReplies.size
                Text(
                    text = "View $remainingReplies replies",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = basePadding, top = 4.dp, bottom = 12.dp)
                        .clickable { onViewReplies(reply.id) }
                )
            }
            
            // Show loading indicator aligned with text content
            if (replyLoadingState.contains(reply.id)) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = basePadding, top = 4.dp, bottom = 12.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
