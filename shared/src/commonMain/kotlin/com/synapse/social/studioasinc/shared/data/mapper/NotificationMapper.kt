package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.model.NotificationDto
import com.synapse.social.studioasinc.shared.data.model.NotificationPreferencesDto
import com.synapse.social.studioasinc.shared.data.model.NotificationAnalyticsDto
import com.synapse.social.studioasinc.shared.domain.model.Notification
import com.synapse.social.studioasinc.shared.domain.model.NotificationMessageType
import com.synapse.social.studioasinc.shared.domain.model.NotificationPreferences
import com.synapse.social.studioasinc.shared.domain.model.NotificationAnalytics
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

fun NotificationDto.toDomain(): Notification {
    val messageBody = body?.get("en")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
    val messageType = if (messageBody != null) NotificationMessageType.CUSTOM else NotificationMessageType.FALLBACK

    return Notification(
        id = id,
        type = type,
        actorId = senderId,
        actorName = actor?.displayName,
        actorAvatar = actor?.avatar,
        message = messageBody,
        messageType = messageType,
        timestamp = createdAt,
        isRead = isRead,
        targetId = data?.get("target_id")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
    )
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
