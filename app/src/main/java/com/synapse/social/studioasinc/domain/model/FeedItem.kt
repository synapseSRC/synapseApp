package com.synapse.social.studioasinc.domain.model

sealed class FeedItem {
    abstract val id: String
    abstract val timestamp: Long
    abstract val itemType: String
    abstract val userId: String
    abstract val createdAt: String?
    abstract val likeCount: Int
    abstract val commentCount: Int
    abstract val isLiked: Boolean
    
    val uniqueKey: String
        get() = "$itemType:$id"

    data class PostItem(
        val post: Post
    ) : FeedItem() {
        override val id = post.id
        override val timestamp = post.timestamp
        override val itemType = "post"
        override val userId = post.authorUid
        override val createdAt = post.createdAt
        override val likeCount = post.getTotalReactionsCount()
        override val commentCount = post.replyCount
        override val isLiked = post.hasUserReacted() || post.userReaction == ReactionType.LIKE
    }

    data class CommentItem(
        override val id: String,
        override val timestamp: Long,
        override val userId: String,
        val content: String,
        val parentPostId: String?,
        val parentCommentId: String?,
        val parentAuthorUsername: String?,
        override val createdAt: String?,
        override val likeCount: Int,
        override val commentCount: Int,
        override val itemType: String = "comment",
        val userFullName: String = "",
        val username: String = "",
        val avatarUrl: String? = null,
        val mediaUrl: String? = null,
        val isVerified: Boolean = false,
        override val isLiked: Boolean = false // Needs fetching later
    ) : FeedItem()
}
