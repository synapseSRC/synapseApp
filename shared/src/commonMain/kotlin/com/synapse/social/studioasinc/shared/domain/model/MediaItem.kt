package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class MediaItem(
    val id: String,
    val url: String,
    val type: MediaType,
    val thumbnailUrl: String? = null,
    val duration: Long? = null,
    val size: Long? = null,
    val mimeType: String? = null,
    @Transient
    val likesCount: Int = 0,
    @Transient
    val commentsCount: Int = 0,
    @Transient
    val userHasLiked: Boolean = false
)
