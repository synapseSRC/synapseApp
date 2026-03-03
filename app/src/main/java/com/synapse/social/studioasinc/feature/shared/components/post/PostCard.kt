package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val isExpanded: Boolean = false
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
    modifier: Modifier = Modifier
) {
    var showReactionPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onPostClick)
    ) {
        PostHeader(
            user = state.user,
            timestamp = state.formattedTimestamp,
            onUserClick = onUserClick,
            onOptionsClick = onOptionsClick,
            taggedPeople = state.post.metadata?.taggedPeople ?: emptyList(),
            feeling = state.post.metadata?.feeling,
            locationName = state.post.locationName
        )

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            PostContent(
                text = state.post.postText,
                mediaUrls = state.mediaUrls,
                postViewStyle = postViewStyle,
                isVideo = state.isVideo,
                pollQuestion = state.pollQuestion,
                pollOptions = state.pollOptions,
                onMediaClick = onMediaClick,
                onPollVote = onPollVote,
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
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            onShareClick = onShareClick,
            onRepostClick = onRepostClick,
            onBookmarkClick = onBookmarkClick,
            onReactionLongPress = if (onReactionSelected != null) {
                { showReactionPicker = true }
            } else null
        )

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
