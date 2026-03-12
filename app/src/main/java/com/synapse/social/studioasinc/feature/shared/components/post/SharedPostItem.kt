package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.domain.model.ReactionType

import com.synapse.social.studioasinc.ui.settings.PostViewStyle



@Composable
fun SharedPostItem(
    post: Post,
    currentProfile: UserProfile? = null,
    postViewStyle: PostViewStyle = PostViewStyle.SWIPE,
    actions: PostActions,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {

    val state = remember(post.id, post.updatedAt, currentProfile?.id, isExpanded) {
        PostUiMapper.mapToState(post, currentProfile, isExpanded)
    }



    val onLikeClick = remember(post.id, actions) { { actions.onLike(post) } }
    val onCommentClick = remember(post.id, actions) { { actions.onComment(post) } }
    val onShareClick = remember(post.id, actions) { { actions.onShare(post) } }
    val onRepostClick = remember(post.id, actions) { { actions.onRepost(post) } }
    val onQuoteClick = remember(post.id, actions) { { actions.onQuote(post) } }
    val onBookmarkClick = remember(post.id, actions) { { actions.onBookmark(post) } }
    val onUserClick = remember(post.authorUid, actions) { { actions.onUserClick(post.authorUid) } }
    val onPostClick = remember(post.id, actions) { { actions.onComment(post) } }
    val onMediaClick = remember(post.id, actions, postViewStyle) {
        { index: Int ->
            if (postViewStyle == PostViewStyle.GRID) {
                actions.onComment(post)
            } else {
                actions.onMediaClick(index)
            }
        }
    }
    val onOptionsClick = remember(post.id, actions) { { actions.onOptionClick(post) } }
    val onPollVote = remember(post.id, actions) {
        { optionId: String ->
            val index = optionId.toIntOrNull()
            if (index != null) {
                actions.onPollVote(post, index)
            }
        }
    }

    PostCard(
        state = state,
        postViewStyle = postViewStyle,
        onLikeClick = onLikeClick,
        onCommentClick = onCommentClick,
        onShareClick = onShareClick,
        onRepostClick = onRepostClick,
        onQuoteClick = onQuoteClick,
        onBookmarkClick = onBookmarkClick,
        onUserClick = onUserClick,
        onPostClick = onPostClick,
        onMediaClick = onMediaClick,
        onOptionsClick = onOptionsClick,
        onPollVote = onPollVote,

        onReactionSelected = { reaction -> actions.onReactionSelected(post, reaction) },
        modifier = modifier
    )
}
