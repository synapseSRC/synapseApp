package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient



@Serializable
data class CommentWithUser(
    val id: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,
    val content: String,
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("replies_count")
    val repliesCount: Int = 0,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("is_edited")
    val isEdited: Boolean = false,
    @SerialName("is_pinned")
    val isPinned: Boolean = false,

    @Transient
    val user: UserProfile? = null,

    @Transient
    val userReaction: ReactionType? = null,
    @Transient
    val reactionSummary: Map<ReactionType, Int> = emptyMap()
) {


    fun isReply(): Boolean = parentCommentId != null



    fun hasReplies(): Boolean = repliesCount > 0



    fun hasMedia(): Boolean = !mediaUrl.isNullOrEmpty()



    fun getTotalReactions(): Int = reactionSummary.values.sum()



    fun getDisplayName(): String = user?.displayName ?: "Unknown User"



    fun getUsername(): String = user?.username ?: "unknown"



    fun getAvatarUrl(): String? = user?.avatar
}
