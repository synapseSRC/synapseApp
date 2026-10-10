package com.synapse.social.studioasinc.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ReelDto(
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
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("location_address") val locationAddress: String? = null,
    @SerialName("location_latitude") val locationLatitude: Double? = null,
    @SerialName("location_longitude") val locationLongitude: Double? = null,
    val metadata: JsonElement? = null,
    val users: UserDto? = null
)

@Serializable
data class UserDto(
    val username: String? = null,
    val avatar: String? = null
)

@Serializable
data class ReelCommentDto(
    val id: String,
    @SerialName("reel_id") val reelId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val users: UserDto? = null
)
