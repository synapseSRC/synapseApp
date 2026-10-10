package com.synapse.social.studioasinc.shared.data.local.entity

import com.synapse.social.studioasinc.shared.domain.model.LinkPreview

data class CommentEntity(
    val id: String,
    val postId: String,
    val authorUid: String,
    val text: String,
    val timestamp: Long,
    val parentCommentId: String?,
    val username: String?,
    val avatarUrl: String?,
    val linkPreview: LinkPreview? = null,
    val viewsCount: Int = 0
)
