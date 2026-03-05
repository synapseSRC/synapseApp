package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.runtime.Stable
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.data.model.UserProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow



object PostUiMapper {
    /**
     * Maximum depth for nested comments to prevent performance degradation.
     * Comments deeper than this will be clamped to this value.
     */
    private const val MAX_COMMENT_DEPTH = 10

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

    /**
     * Maps a FeedItem.CommentItem to PostCardState for rendering feed comments using PostCard.
     * 
     * @param feedComment The feed comment item to map
     * @return PostCardState configured for comment rendering
     */
    fun toPostCardState(feedComment: com.synapse.social.studioasinc.domain.model.FeedItem.CommentItem): PostCardState {
        // Create User object from FeedItem.CommentItem
        val user = User(
            uid = feedComment.userId,
            username = feedComment.username.ifBlank { "unknown" },
            displayName = feedComment.userFullName.ifBlank { feedComment.username.ifBlank { "Unknown User" } },
            avatar = feedComment.avatarUrl,
            verify = feedComment.isVerified
        )
        
        // Create minimal Post object with comment content and metrics
        val post = Post(
            id = feedComment.id,
            authorUid = feedComment.userId,
            postText = feedComment.content,
            timestamp = feedComment.timestamp,
            likesCount = feedComment.likeCount,
            commentsCount = feedComment.commentCount
        )
        
        return PostCardState(
            post = post,
            user = user,
            isLiked = feedComment.isLiked,
            likeCount = feedComment.likeCount,
            commentCount = feedComment.commentCount,
            repostCount = 0,
            viewsCount = 0,
            isBookmarked = false,
            hideLikeCount = false,
            mediaUrls = listOfNotNull(feedComment.mediaUrl),
            isVideo = false,
            pollQuestion = null,
            pollOptions = null,
            userPollVote = null,
            formattedTimestamp = com.synapse.social.studioasinc.core.util.TimeUtils.getTimeAgo(feedComment.createdAt ?: ""),
            isExpanded = false,
            repostedBy = null,
            // Comment-specific fields
            isComment = true,
            parentCommentId = feedComment.parentCommentId,
            parentAuthorUsername = feedComment.parentAuthorUsername,
            repliesCount = feedComment.commentCount,
            depth = 0, // Feed comments are always top-level
            showThreadLine = false, // No thread lines in feed
            isLastReply = false
        )
    }

    /**
     * Maps a CommentWithUser to PostCardState for rendering comments using PostCard.
     * 
     * @param comment The comment with user information to map
     * @param parentAuthorUsername Username of the parent comment author (for reply context)
     * @param depth Nesting depth of the comment (0 for top-level, clamped to MAX_COMMENT_DEPTH)
     * @param showThreadLine Whether to show the thread line indicator
     * @param isLastReply Whether this is the last reply in a thread
     * @return PostCardState configured for comment rendering
     */
    fun toPostCardState(
        comment: CommentWithUser,
        parentAuthorUsername: String? = null,
        depth: Int = 0,
        showThreadLine: Boolean = false,
        isLastReply: Boolean = false
    ): PostCardState {
        // Clamp depth to prevent performance degradation with deeply nested comments
        val clampedDepth = depth.coerceIn(0, MAX_COMMENT_DEPTH)
        
        // Create User object from CommentWithUser using helper methods
        val user = User(
            uid = comment.userId,
            username = comment.getUsername(),
            displayName = comment.getDisplayName(),
            avatar = comment.getAvatarUrl(),
            verify = comment.user?.isVerified ?: false
        )
        
        // Create minimal Post object with comment content and metrics
        val post = Post(
            id = comment.id,
            authorUid = comment.userId,
            postText = comment.content,
            timestamp = System.currentTimeMillis(), // Will be overridden by formattedTimestamp
            likesCount = comment.likesCount,
            commentsCount = comment.repliesCount
        )
        
        return PostCardState(
            post = post,
            user = user,
            isLiked = comment.userReaction != null,
            likeCount = comment.getTotalReactions(),
            commentCount = comment.repliesCount,
            repostCount = 0,
            viewsCount = 0,
            isBookmarked = false,
            hideLikeCount = false,
            mediaUrls = listOfNotNull(comment.mediaUrl),
            isVideo = false,
            pollQuestion = null,
            pollOptions = null,
            userPollVote = null,
            formattedTimestamp = com.synapse.social.studioasinc.core.util.TimeUtils.getTimeAgo(comment.createdAt),
            isExpanded = false,
            repostedBy = null,
            // Comment-specific fields
            isComment = true,
            parentCommentId = comment.parentCommentId,
            parentAuthorUsername = parentAuthorUsername,
            repliesCount = comment.repliesCount,
            depth = clampedDepth,
            showThreadLine = showThreadLine,
            isLastReply = isLastReply
        )
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
