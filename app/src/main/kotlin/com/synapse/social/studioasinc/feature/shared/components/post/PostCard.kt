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
import androidx.compose.runtime.Immutable
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.synapse.social.studioasinc.ui.settings.PostViewStyle



@Immutable
data class PostCardState(
    val post: Post,
    val user: User,
    val isLiked: Boolean,
    val likeCount: Int,
    val commentCount: Int,
    val repostCount: Int = 0,
    val viewsCount: Int = 0,
    val isBookmarked: Boolean,
    val isReshared: Boolean = false,
    val hideLikeCount: Boolean = false,
    val hideViewsCount: Boolean = false,
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
    val replyToUsernames: List<String> = emptyList(),
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
    onQuoteClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onUserClick: () -> Unit,
    onPostClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onOptionsClick: () -> Unit,
    onPollVote: (String) -> Unit,
    onReactionSelected: ((ReactionType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReactionPicker by remember { mutableStateOf(false) }

    val avatarSize = Sizes.AvatarDefault

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onPostClick)
            // No horizontal padding offset: avatars align fully to the left in X style
    ) {
        if (state.repostedBy != null) {
            Row(
                modifier = Modifier
                    .padding(start = Sizes.AvatarDefault, top = Spacing.Small, bottom = Spacing.ExtraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text(
                    text = "${state.repostedBy} reposted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
 
        // Outline colors for Canvas
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val lineColor = remember(outlineVariant) { outlineVariant.copy(alpha = 0.5f) }
        val isReply = state.depth > 0

        // Main layout Row: Avatar on left, content on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val paddingX = Spacing.SmallMedium.toPx()
                    val paddingY = Spacing.Small.toPx()
                    val avatarRadius = (avatarSize / 2).toPx()
                    val centerX = paddingX + avatarRadius
                    val avatarCenterY = paddingY + avatarRadius

                    // Line from top of card down to avatar center (connects to parent's line)
                    if (isReply) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, avatarCenterY),
                            strokeWidth = strokeWidth
                        )
                    }

                    // Line below avatar down to bottom of card (continues thread to child)
                    if (state.showThreadLine) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, avatarCenterY + avatarRadius),
                            end = Offset(centerX, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                }
                .padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small)
        ) {
            // Left Column: Avatar
            Box(
                modifier = Modifier
                    .width(avatarSize),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularAvatar(
                    imageUrl = state.user.avatar,
                    contentDescription = "Avatar of ${state.user.username}",
                    onClick = onUserClick,
                    size = avatarSize,
                    displayName = state.user.displayName ?: state.user.username
                )
            }

            Spacer(modifier = Modifier.width(Spacing.SmallMedium))

            // Right Column: Header, Content
            Column(modifier = Modifier.weight(1f)) {
                PostHeader(
                    user = state.user,
                    timestamp = if (state.isExpanded) "" else state.formattedTimestamp,
                    onUserClick = onUserClick,
                    onOptionsClick = onOptionsClick,
                    feeling = state.post.metadata?.feeling,
                    locationName = state.post.locationName,
                    taggedPeople = state.post.metadata?.taggedPeople ?: emptyList(),
                    replyToUsernames = state.replyToUsernames
                )

                PostContent(
                    text = state.post.postText,
                    mediaUrls = state.mediaUrls,
                    postViewStyle = postViewStyle,
                    isVideo = state.isVideo,
                    pollQuestion = state.pollQuestion,
                    pollOptions = state.pollOptions,
                    userPollVote = state.userPollVote,
                    onMediaClick = onMediaClick,
                    onPollVote = onPollVote,
                    quotedPost = state.post.quotedPost,
                    linkPreviews = state.post.linkPreviews,
                    isExpanded = state.isExpanded,
                    modifier = Modifier
                )

                if (!state.isExpanded) {
                    PostInteractionBar(
                        isLiked = state.isLiked,
                        likeCount = state.likeCount,
                        commentCount = state.commentCount,
                        repostCount = state.repostCount,
                        viewsCount = state.viewsCount,
                        isBookmarked = state.isBookmarked,
                        isReshared = state.isReshared,
                        hideLikeCount = state.hideLikeCount,
                        hideViewsCount = state.hideViewsCount,
                        onLikeClick = onLikeClick,
                        onCommentClick = onCommentClick,
                        onShareClick = onShareClick,
                        onRepostClick = onRepostClick,
                        onQuoteClick = onQuoteClick,
                        onBookmarkClick = onBookmarkClick,
                        onReactionLongPress = if (onReactionSelected != null) {
                            { showReactionPicker = true }
                        } else null
                    )
                }
            }
        }

        // Expanded (focal post) layout: timestamp + View post activity + full-width action bar
        if (state.isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.SmallMedium)
            ) {
                Spacer(modifier = Modifier.height(Spacing.Small))
                // Timestamp on its own line
                if (state.formattedTimestamp.isNotBlank()) {
                    Text(
                        text = state.formattedTimestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = avatarSize + Spacing.SmallMedium,
                            top = Spacing.ExtraSmall,
                            bottom = Spacing.ExtraSmall
                        )
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = Sizes.BorderHairline
                )

                // "View post activity" row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        text = stringResource(R.string.view_post_activity),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = Sizes.BorderHairline
                )

                // Full-width action bar (counts hidden for focal post, icons only)
                PostInteractionBar(
                    isLiked = state.isLiked,
                    likeCount = state.likeCount,
                    commentCount = state.commentCount,
                    repostCount = state.repostCount,
                    viewsCount = 0,
                    isBookmarked = state.isBookmarked,
                    isReshared = state.isReshared,
                    hideLikeCount = true,
                    hideViewsCount = true,
                    onLikeClick = onLikeClick,
                    onCommentClick = onCommentClick,
                    onShareClick = onShareClick,
                    onRepostClick = onRepostClick,
                    onQuoteClick = onQuoteClick,
                    onBookmarkClick = onBookmarkClick,
                    onReactionLongPress = if (onReactionSelected != null) {
                        { showReactionPicker = true }
                    } else null
                )
            }
        }

        if (!state.showThreadLine && !isReply) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Spacing.SmallMedium),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = Sizes.BorderHairline
            )
        }
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

