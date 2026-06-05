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
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.compose.foundation.background
import androidx.paging.LoadState
import androidx.paging.compose.itemKey
import androidx.paging.compose.LazyPagingItems
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.CommentAction
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.feature.shared.components.post.PostCard
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
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
    onCommentViewed: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {}
) {
    val viewedCommentIds = remember { mutableSetOf<String>() }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            headerContent()
        }

        if (comments.loadState.refresh is LoadState.Loading) {
            item {
                CommentShimmer()
            }
        }

        if (comments.loadState.refresh is LoadState.Error) {
             item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
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
                        .padding(Spacing.ExtraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.synapse.social.studioasinc.R.string.no_comments))
                }
            }
        }

        items(comments.itemCount) { index ->
            val comment = comments[index]
            if (comment != null) {
                LaunchedEffect(comment.id) {
                    if (viewedCommentIds.add(comment.id)) {
                        onCommentViewed?.invoke(comment.id)
                    }
                }
                val postCardState = PostUiMapper.toPostCardState(
                    comment = comment,
                    parentAuthorUsername = null,
                    depth = 0,
                    showThreadLine = comment.repliesCount > 0,
                    isLastReply = false
                )
                
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
            }
        }

        if (comments.loadState.append is LoadState.Loading) {
             item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            }
        }
    }
}
