package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("author_id")
    val authorId: String,
    val text: String,
    val timestamp: Long,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("replies_count")
    val repliesCount: Int = 0,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,
    val username: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)
