package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.runtime.Stable
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.data.model.UserProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow



object PostUiMapper {
    fun toPostCardState(post: Post, currentProfile: UserProfile? = null, isExpanded: Boolean = false): PostCardState {

        val resolvedDisplayName = when {
            !post.displayName.isNullOrBlank() -> post.displayName!!
            !post.username.isNullOrBlank() -> post.username!!
            currentProfile?.id == post.authorUid -> {
                currentProfile.name?.takeIf { it.isNotBlank() } ?: currentProfile.username
            }
            else -> "Unknown"
        }

        val resolvedAvatar = when {
            !post.avatarUrl.isNullOrBlank() -> post.avatarUrl
            currentProfile?.id == post.authorUid -> currentProfile.avatar
            else -> null
        }

        val resolvedVerified = when {
            post.isVerified -> true
            currentProfile?.id == post.authorUid -> currentProfile.isVerified
            else -> false
        }

        val user = User(
            uid = post.authorUid,
            username = resolvedDisplayName,
            avatar = resolvedAvatar,
            verify = resolvedVerified
        )

        val mediaUrls = post.mediaItems?.mapNotNull { it.url } ?: listOfNotNull(post.postImage)


        val mappedPollOptions = post.pollOptions?.mapIndexed { index, option ->
            PollOption(
                id = index.toString(),
                text = option.text,
                voteCount = option.votes,
                isSelected = post.userPollVote == index
            )
        }

        return PostCardState(
            post = post,
            user = user,
            isLiked = post.userReaction == ReactionType.LIKE || post.hasUserReacted(),
            likeCount = post.getTotalReactionsCount(),
            commentCount = post.commentsCount,
            repostCount = post.resharesCount,
            viewsCount = post.viewsCount,
            isBookmarked = post.isBookmarked,
            hideLikeCount = post.postHideLikeCount == "true",
            mediaUrls = mediaUrls,
            isVideo = post.postType == "VIDEO",
            pollQuestion = post.pollQuestion,
            pollOptions = mappedPollOptions,
            userPollVote = post.userPollVote,
            formattedTimestamp = com.synapse.social.studioasinc.core.util.TimeUtils.getTimeAgo(post.publishDate ?: ""),
            isExpanded = isExpanded
        )
    }

    fun mapToState(post: Post, currentProfile: UserProfile? = null, isExpanded: Boolean = false): PostCardState {
        return toPostCardState(post, currentProfile, isExpanded)
    }
}



sealed class PostEvent {
    data class Liked(val postId: String, val isLiked: Boolean, val newLikeCount: Int) : PostEvent()
    data class PollVoted(val postId: String, val optionIndex: Int, val pollOptions: List<com.synapse.social.studioasinc.domain.model.PollOption>, val userVote: Int?) : PostEvent()
    data class Deleted(val postId: String) : PostEvent()
    data class Updated(val post: Post) : PostEvent()
    data class Created(val post: Post) : PostEvent()
    data class Error(val message: String) : PostEvent()
}



object PostEventBus {
    private val _events = MutableSharedFlow<PostEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    fun emit(event: PostEvent) {
        _events.tryEmit(event)
    }
}



@Stable
data class PostActions(
    val onLike: (Post) -> Unit,
    val onComment: (Post) -> Unit,
    val onShare: (Post) -> Unit,
    val onRepost: (Post) -> Unit,
    val onBookmark: (Post) -> Unit,
    val onOptionClick: (Post) -> Unit,
    val onPollVote: (Post, Int) -> Unit,
    val onUserClick: (String) -> Unit,
    val onMediaClick: (Int) -> Unit,
    val onReactionSelected: (Post, ReactionType) -> Unit
)
