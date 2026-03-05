package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.ReactionType
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.synapse.social.studioasinc.ui.settings.PostViewStyle



@Stable
data class PostCardState(
    val post: Post,
    val user: User,
    val isLiked: Boolean,
    val likeCount: Int,
    val commentCount: Int,
    val repostCount: Int = 0,
    val viewsCount: Int = 0,
    val isBookmarked: Boolean,
    val hideLikeCount: Boolean = false,
    val mediaUrls: List<String> = emptyList(),
    val isVideo: Boolean = false,
    val pollQuestion: String? = null,
    val pollOptions: List<PollOption>? = null,
    val userPollVote: Int? = null,
    val formattedTimestamp: String = "",
    val isExpanded: Boolean = false,
    val repostedBy: String? = null,
    // Comment-specific fields
    val isComment: Boolean = false,
    val parentCommentId: String? = null,
    val parentAuthorUsername: String? = null,
    val repliesCount: Int = 0,
    val depth: Int = 0,
    val showThreadLine: Boolean = false,
    val isLastReply: Boolean = false
)

@Composable
fun PostCard(
    state: PostCardState,
    postViewStyle: PostViewStyle = PostViewStyle.SWIPE,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onRepostClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onUserClick: () -> Unit,
    onPostClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onOptionsClick: () -> Unit,
    onPollVote: (String) -> Unit,
    onReactionSelected: ((ReactionType) -> Unit)? = null,
    onParentAuthorClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReactionPicker by remember { mutableStateOf(false) }

    // Calculate avatar size based on comment depth (memoized to avoid recomputation)
    val avatarSize = remember(state.isComment, state.depth) {
        when {
            state.isComment && state.depth > 0 -> 32.dp
            state.isComment -> 40.dp
            else -> 48.dp
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onPostClick)
    ) {
        if (state.repostedBy != null) {
            Row(
                modifier = Modifier
                    .padding(start = 48.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${state.repostedBy} reposted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
 
        // Use Row to position thread line alongside content for comments
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thread line column (only for comments with thread line)
            // Memoize the thread line visibility check
            val showThreadLineColumn = remember(state.isComment, state.showThreadLine, state.isLastReply) {
                state.isComment && state.showThreadLine && !state.isLastReply
            }
            
            if (showThreadLineColumn) {
                Column(
                    modifier = Modifier.padding(start = 12.dp + (avatarSize / 2) - (com.synapse.social.studioasinc.feature.shared.theme.Spacing.Tiny / 2)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Spacer to position thread line below avatar
                    // Header has top padding of 12.dp, avatar size, and bottom padding of 8.dp
                    Spacer(modifier = Modifier.size(12.dp + avatarSize + 8.dp))
                    
                    // Vertical thread line
                    Box(
                        modifier = Modifier
                            .width(com.synapse.social.studioasinc.feature.shared.theme.Spacing.Tiny)
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                    )
                }
            }
            
            // Main content column
            // Memoize the content column modifier
            val contentModifier = remember(showThreadLineColumn) {
                if (showThreadLineColumn) {
                    Modifier.weight(1f)
                } else {
                    Modifier.fillMaxWidth()
                }
            }
            
            Column(modifier = contentModifier) {
                PostHeader(
                    user = state.user,
                    timestamp = state.formattedTimestamp,
                    onUserClick = onUserClick,
                    onOptionsClick = onOptionsClick,
                    taggedPeople = state.post.metadata?.taggedPeople ?: emptyList(),
                    feeling = state.post.metadata?.feeling,
                    locationName = state.post.locationName,
                    avatarSize = avatarSize
                )

                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    // Reply context display for comments
                    if (state.isComment && state.parentAuthorUsername != null) {
                        ReplyContext(
                            parentAuthorUsername = state.parentAuthorUsername,
                            onParentAuthorClick = onParentAuthorClick
                        )
                    }
                    
                    // Memoize conditional parameters to avoid recomputation
                    val contentMediaUrls = state.mediaUrls
                    val contentPollQuestion = remember(state.isComment, state.pollQuestion) {
                        if (state.isComment) null else state.pollQuestion
                    }
                    val contentPollOptions = remember(state.isComment, state.pollOptions) {
                        if (state.isComment) null else state.pollOptions
                    }
                    val contentQuotedPost = remember(state.isComment, state.post.quotedPost) {
                        if (state.isComment) null else state.post.quotedPost
                    }
                    
                    PostContent(
                        text = state.post.postText,
                        mediaUrls = contentMediaUrls,
                        postViewStyle = postViewStyle,
                        isVideo = state.isVideo,
                        pollQuestion = contentPollQuestion,
                        pollOptions = contentPollOptions,
                        userPollVote = state.userPollVote,
                        onMediaClick = onMediaClick,
                        onPollVote = onPollVote,
                        quotedPost = contentQuotedPost,
                        isExpanded = state.isExpanded
                    )
                }

                PostInteractionBar(
                    isLiked = state.isLiked,
                    likeCount = state.likeCount,
                    commentCount = state.commentCount,
                    repostCount = state.repostCount,
                    viewsCount = state.viewsCount,
                    isBookmarked = state.isBookmarked,
                    hideLikeCount = state.hideLikeCount,
                    isComment = state.isComment,
                    onLikeClick = onLikeClick,
                    onCommentClick = onCommentClick,
                    onShareClick = onShareClick,
                    onRepostClick = onRepostClick,
                    onBookmarkClick = onBookmarkClick,
                    onReactionLongPress = if (onReactionSelected != null) {
                        { showReactionPicker = true }
                    } else null
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )
    }

    if (showReactionPicker && onReactionSelected != null) {
        ReactionPicker(
            onReactionSelected = { reaction ->
                onReactionSelected(reaction)
                showReactionPicker = false
            },
            onDismiss = { showReactionPicker = false }
        )
    }
}

@Composable
private fun ReplyContext(
    parentAuthorUsername: String,
    onParentAuthorClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(bottom = com.synapse.social.studioasinc.feature.shared.theme.Spacing.Tiny),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val replyingToText = androidx.compose.ui.res.stringResource(
            com.synapse.social.studioasinc.R.string.replying_to,
            ""
        ).replace("%s", "").trim()
        
        Text(
            text = "$replyingToText ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "@$parentAuthorUsername",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = if (onParentAuthorClick != null) {
                Modifier.clickable { onParentAuthorClick() }
            } else {
                Modifier
            }
        )
    }
}
