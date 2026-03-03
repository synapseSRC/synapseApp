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

    val state = remember(post, currentProfile, isExpanded) {
        PostUiMapper.mapToState(post, currentProfile, isExpanded)
    }



    val onLikeClick = remember(post, actions) { { actions.onLike(post) } }
    val onCommentClick = remember(post, actions) { { actions.onComment(post) } }
    val onShareClick = remember(post, actions) { { actions.onShare(post) } }
    val onRepostClick = remember(post, actions) { { actions.onRepost(post) } }
    val onBookmarkClick = remember(post, actions) { { actions.onBookmark(post) } }
    val onUserClick = remember(post, actions) { { actions.onUserClick(post.authorUid) } }
    val onPostClick = remember(post, actions) { { actions.onComment(post) } }
    val onMediaClick = remember(post, actions, postViewStyle) {
        { index: Int ->
            if (postViewStyle == PostViewStyle.GRID) {
                actions.onComment(post)
            } else {
                actions.onMediaClick(index)
            }
        }
    }
    val onOptionsClick = remember(post, actions) { { actions.onOptionClick(post) } }
    val onPollVote = remember(post, actions) {
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
