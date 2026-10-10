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

sealed interface NotificationTarget {
    data class Post(val postId: String) : NotificationTarget
    data class Comment(val postId: String, val commentId: String) : NotificationTarget
    data class Profile(val userId: String) : NotificationTarget
    data object Unknown : NotificationTarget
}

data class Notification(
    val id: String,
    val type: String,
    val actorId: String?,
    val actorName: String?,
    val actorAvatar: String?,
    val message: String?,
    val messageType: NotificationMessageType,
    val timestamp: String,
    val isRead: Boolean,
    val targetId: String?,
    val target: NotificationTarget = NotificationTarget.Unknown
)
