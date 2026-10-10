package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.Story
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import com.synapse.social.studioasinc.domain.model.StoryView
import com.synapse.social.studioasinc.domain.model.StoryViewWithUser
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoryUserDto(
    val id: String? = null,
    val uid: String? = null,
    val username: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val avatar: String? = null,
    val verify: Boolean = false
)

fun StoryUserDto.toDomain(fallbackUid: String): User {
    val avatarUrl = avatar?.let { path ->
        if (path.startsWith("http")) path else SupabaseClient.constructAvatarUrl(path)
    }
    return User(
        id = id,
        uid = uid ?: fallbackUid,
        username = username,
        displayName = displayName,
        avatar = avatarUrl,
        verify = verify
    )
}

@Serializable
data class StoryWithUserDto(
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
    val privacySetting: String? = null,
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
    val expiresAt: String? = null,
    @SerialName("users")
    val user: StoryUserDto? = null
)

fun StoryWithUserDto.toDomain(): Story {
    return Story(
        id = id,
        userId = userId,
        mediaUrl = mediaUrl,
        mediaType = mediaType?.let {
            try {
                StoryMediaType.valueOf(it.uppercase())
            } catch (e: Exception) {
                null
            }
        },
        content = content,
        duration = duration,
        durationHours = durationHours,
        privacy = when (privacySetting) {
            "followers" -> StoryPrivacy.FOLLOWERS
            "public" -> StoryPrivacy.PUBLIC
            else -> null
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

@Serializable
data class StoryViewWithUserDto(
    val id: String? = null,
    @SerialName("story_id")
    val storyId: String,
    @SerialName("viewer_id")
    val viewerId: String,
    @SerialName("viewed_at")
    val viewedAt: String? = null,
    @SerialName("users")
    val user: StoryUserDto? = null
)

fun StoryViewWithUserDto.toDomain(): StoryViewWithUser {
    val storyView = StoryView(
        id = id,
        storyId = storyId,
        viewerId = viewerId,
        viewedAt = viewedAt
    )
    return StoryViewWithUser(
        storyView = storyView,
        viewer = user?.toDomain(viewerId)
    )
}
