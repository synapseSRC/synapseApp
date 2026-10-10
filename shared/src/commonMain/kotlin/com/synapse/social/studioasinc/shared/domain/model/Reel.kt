package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reel(
    val id: String,
    @SerialName("creator_id") val creatorId: String,
    @SerialName("video_url") val videoUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val caption: String? = null,
    @SerialName("music_track") val musicTrack: String? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("share_count") val shareCount: Int = 0,
    @SerialName("oppose_count") val opposeCount: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val creatorUsername: String? = null,
    val creatorAvatarUrl: String? = null,
    val isLikedByCurrentUser: Boolean = false,
    val isOpposedByCurrentUser: Boolean = false,
    val isSavedByCurrentUser: Boolean = false,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val metadata: String? = null
)
