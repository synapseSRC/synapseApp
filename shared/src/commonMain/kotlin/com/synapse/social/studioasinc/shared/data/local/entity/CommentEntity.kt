package com.synapse.social.studioasinc.shared.data.local.entity

data class CommentEntity(
    val id: String,
    val postId: String,
    val authorUid: String,
    val text: String,
    val timestamp: Long,
    val parentCommentId: String?,
    val username: String?,
    val avatarUrl: String?
)
