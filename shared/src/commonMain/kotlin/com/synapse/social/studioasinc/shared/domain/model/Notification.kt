package com.synapse.social.studioasinc.shared.domain.model

sealed class NotificationError : Exception() {
    object NetworkError : NotificationError()
    object Unauthorized : NotificationError()
    object Unknown : NotificationError()
}

enum class NotificationMessageType {
    CUSTOM,
    FALLBACK
}

data class Notification(
    val id: String,
    val type: String,
    val actorName: String?,
    val actorAvatar: String?,
    val message: String?,
    val messageType: NotificationMessageType,
    val timestamp: String,
    val isRead: Boolean,
    val targetId: String?
)
