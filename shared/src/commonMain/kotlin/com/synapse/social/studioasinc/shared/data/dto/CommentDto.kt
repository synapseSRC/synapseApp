package com.synapse.social.studioasinc.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentDto(
    @SerialName("id") val id: String,
    @SerialName("post_id") val postId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("content") val content: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("replies_count") val repliesCount: Int = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("author") val author: CommentAuthorDto? = null
)

@Serializable
data class CommentAuthorDto(
    @SerialName("username") val username: String? = null,
    @SerialName("avatar") val avatarUrl: String? = null
)
