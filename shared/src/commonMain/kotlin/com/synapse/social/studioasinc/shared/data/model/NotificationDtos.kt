package com.synapse.social.studioasinc.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class NotificationDto(
    @SerialName("id") val id: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("type") val type: String,
    @SerialName("title") val title: JsonObject,
    @SerialName("body") val body: JsonObject,
    @SerialName("data") val data: JsonObject? = null,
    @SerialName("priority") val priority: Int = 2,
    @SerialName("delivery_status") val deliveryStatus: String? = "pending",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("interacted_at") val interactedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("actor") val actor: NotificationActorDto? = null
)

@Serializable
data class NotificationActorDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar") val avatar: String? = null
)

@Serializable
data class NotificationPreferencesDto(
    @SerialName("user_id") val userId: String,
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("settings") val settings: JsonObject,
    @SerialName("quiet_hours") val quietHours: JsonObject,
    @SerialName("do_not_disturb") val doNotDisturb: Boolean = false,
    @SerialName("dnd_until") val dndUntil: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class NotificationAnalyticsDto(
    @SerialName("id") val id: String? = null,
    @SerialName("notification_id") val notificationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("interaction_type") val interactionType: String,
    @SerialName("platform") val platform: String,
    @SerialName("app_version") val appVersion: String? = null
)
