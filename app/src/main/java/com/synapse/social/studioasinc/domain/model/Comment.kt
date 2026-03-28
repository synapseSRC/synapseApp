package com.synapse.social.studioasinc.domain.model

@Deprecated("Use Post model instead")
data class Comment(
    val uid: String = "",
    val comment: String = "",
    val pushTime: String = "",
    val key: String = "",
    val like: Long? = null,
    val postKey: String = "",
    val replyCommentKey: String? = null,
    val isPinned: Boolean = false,
    val pinnedAt: String? = null,
    val pinnedBy: String? = null,
    val editedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val reportCount: Int = 0,
    val viewsCount: Int = 0,
    val photoUrl: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val mediaType: String? = null
)
