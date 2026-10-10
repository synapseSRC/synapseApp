package com.synapse.social.studioasinc.shared.data.model

import com.synapse.social.studioasinc.shared.domain.model.Story
import com.synapse.social.studioasinc.shared.domain.model.StoryMediaType
import com.synapse.social.studioasinc.shared.domain.model.StoryPrivacy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoryDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    val content: String? = null,
    val duration: Int? = null,
    @SerialName("duration_hours")
    val durationHours: Int? = null,
    @SerialName("privacy_setting")
    val privacy: String? = null,
    @SerialName("views_count")
    val viewCount: Int? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("media_width")
    val mediaWidth: Int? = null,
    @SerialName("media_height")
    val mediaHeight: Int? = null,
    @SerialName("media_duration_seconds")
    val mediaDurationSeconds: Int? = null,
    @SerialName("file_size_bytes")
    val fileSizeBytes: Long? = null,
    @SerialName("reactions_count")
    val reactionsCount: Int? = null,
    @SerialName("replies_count")
    val repliesCount: Int? = null,
    @SerialName("is_reported")
    val isReported: Boolean? = null,
    @SerialName("moderation_status")
    val moderationStatus: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null
)

fun StoryDto.toDomain(): Story {
    return Story(
        id = id,
        userId = userId,
        mediaUrl = mediaUrl,
        mediaType = mediaType?.let { type ->
            StoryMediaType.entries.find { it.name.equals(type, ignoreCase = true) }
        },
        content = content,
        duration = duration,
        durationHours = durationHours,
        privacy = privacy?.let { p ->
            StoryPrivacy.entries.find { it.name.equals(p, ignoreCase = true) }
        },
        viewCount = viewCount,
        isActive = isActive,
        thumbnailUrl = thumbnailUrl,
        mediaWidth = mediaWidth,
        mediaHeight = mediaHeight,
        mediaDurationSeconds = mediaDurationSeconds,
        fileSizeBytes = fileSizeBytes,
        reactionsCount = reactionsCount,
        repliesCount = repliesCount,
        isReported = isReported,
        moderationStatus = moderationStatus,
        createdAt = createdAt,
        expiresAt = expiresAt
    )
}
