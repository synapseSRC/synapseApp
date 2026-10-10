package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.model.NotificationDto
import com.synapse.social.studioasinc.shared.data.model.NotificationPreferencesDto
import com.synapse.social.studioasinc.shared.data.model.NotificationAnalyticsDto
import com.synapse.social.studioasinc.shared.domain.model.Notification
import com.synapse.social.studioasinc.shared.domain.model.NotificationMessageType
import com.synapse.social.studioasinc.shared.domain.model.NotificationPreferences
import com.synapse.social.studioasinc.shared.domain.model.NotificationAnalytics
import com.synapse.social.studioasinc.shared.domain.model.NotificationTarget
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private fun JsonObject.getString(key: String): String? {
    val element = get(key) ?: return null
    if (element is JsonPrimitive) {
        return element.contentOrNull?.takeIf { it.isNotBlank() }
    }
    return null
}

fun NotificationDto.toDomain(): Notification {
    val messageBody = body?.get("en")?.let { if (it is JsonPrimitive) it else null }?.contentOrNull
        ?: title?.get("en")?.let { if (it is JsonPrimitive) it else null }?.contentOrNull
    val messageType = if (messageBody != null) NotificationMessageType.CUSTOM else NotificationMessageType.FALLBACK

    val rawTargetId = data?.getString("target_id")
        ?: data?.getString("targetId")
        ?: data?.getString("postId")
        ?: data?.getString("post_id")
        ?: data?.getString("commentId")
        ?: data?.getString("comment_id")
        ?: data?.getString("chat_id")
        ?: data?.getString("followerId")
        ?: data?.getString("follower_id")

    val rawDisplayName = actor?.displayName?.trim()
    val rawUsername = actor?.username?.trim()
    val actorName = when {
        !rawDisplayName.isNullOrEmpty() -> rawDisplayName
        !rawUsername.isNullOrEmpty() -> rawUsername
        else -> null
    }

    val targetType = data?.getString("targetType") ?: data?.getString("target_type")
    val postId = data?.getString("postId") ?: data?.getString("post_id") ?: rawTargetId
    val commentId = data?.getString("commentId") ?: data?.getString("comment_id")
    val followerId = data?.getString("followerId") ?: data?.getString("follower_id") ?: senderId

    val resolvedTarget: NotificationTarget = resolveNotificationTarget(
        type = type,
        targetType = targetType,
        postId = postId,
        commentId = commentId,
        followerId = followerId,
        actorId = senderId,
        rawTargetId = rawTargetId
    )

    return Notification(
        id = id,
        type = type,
        actorId = senderId,
        actorName = actorName,
        actorAvatar = actor?.avatar,
        message = messageBody,
        messageType = messageType,
        timestamp = createdAt,
        isRead = isRead,
        targetId = rawTargetId,
        target = resolvedTarget
    )
}

private fun resolveNotificationTarget(
    type: String,
    targetType: String?,
    postId: String?,
    commentId: String?,
    followerId: String?,
    actorId: String?,
    rawTargetId: String?
): NotificationTarget {
    val upperType = type.uppercase()
    val upperTargetType = targetType?.uppercase()

    if (upperType == "NEW_FOLLOWER" || upperType == "FOLLOW") {
        val targetUser = followerId ?: actorId ?: rawTargetId
        return if (!targetUser.isNullOrBlank()) NotificationTarget.Profile(targetUser) else NotificationTarget.Unknown
    }

    if (upperType == "NEW_POST" || upperType == "POST" || upperType == "MENTION") {
        val targetPost = postId ?: rawTargetId
        return if (!targetPost.isNullOrBlank()) NotificationTarget.Post(targetPost) else NotificationTarget.Unknown
    }

    if (upperType == "NEW_COMMENT" || upperType == "COMMENT" || upperType == "NEW_REPLY" || upperType == "REPLY" || upperType == "NEW_LIKE_COMMENT" || upperType == "LIKE_COMMENT") {
        val targetPost = postId ?: rawTargetId
        return when {
            !targetPost.isNullOrBlank() && !commentId.isNullOrBlank() -> NotificationTarget.Comment(targetPost, commentId)
            !targetPost.isNullOrBlank() -> NotificationTarget.Post(targetPost)
            else -> NotificationTarget.Unknown
        }
    }

    if (upperType == "NEW_LIKE_POST" || upperType == "LIKE_POST") {
        val targetPost = postId ?: rawTargetId
        return if (!targetPost.isNullOrBlank()) NotificationTarget.Post(targetPost) else NotificationTarget.Unknown
    }

    // Fallback using targetType or generic payload matching
    if (upperTargetType == "POST_COMMENT" && !postId.isNullOrBlank() && !commentId.isNullOrBlank()) {
        return NotificationTarget.Comment(postId, commentId)
    }

    if (upperTargetType == "POST" && !postId.isNullOrBlank()) {
        return NotificationTarget.Post(postId)
    }

    // General fallback based on available IDs
    return when {
        !postId.isNullOrBlank() && !commentId.isNullOrBlank() -> NotificationTarget.Comment(postId, commentId)
        !postId.isNullOrBlank() -> NotificationTarget.Post(postId)
        !rawTargetId.isNullOrBlank() -> NotificationTarget.Post(rawTargetId)
        else -> NotificationTarget.Unknown
    }
}

fun NotificationPreferencesDto.toDomain(): NotificationPreferences {
    return NotificationPreferences(
        userId = userId,
        enabled = enabled,
        settings = settings,
        quietHours = quietHours,
        doNotDisturb = doNotDisturb,
        dndUntil = dndUntil,
        updatedAt = updatedAt
    )
}

fun NotificationPreferences.toDto(): NotificationPreferencesDto {
    return NotificationPreferencesDto(
        userId = userId,
        enabled = enabled,
        settings = settings,
        quietHours = quietHours,
        doNotDisturb = doNotDisturb,
        dndUntil = dndUntil,
        updatedAt = updatedAt
    )
}

fun NotificationAnalytics.toDto(): NotificationAnalyticsDto {
    return NotificationAnalyticsDto(
        id = id,
        notificationId = notificationId,
        userId = userId,
        deliveredAt = deliveredAt,
        openedAt = openedAt,
        interactionType = interactionType,
        platform = platform,
        appVersion = appVersion
    )
}
