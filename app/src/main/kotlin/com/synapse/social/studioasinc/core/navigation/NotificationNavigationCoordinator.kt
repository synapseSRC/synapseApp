package com.synapse.social.studioasinc.core.navigation

import android.os.SystemClock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.json.JSONObject
import java.util.LinkedHashMap
import java.util.Locale

data class NotificationNavigationRequest(
    val target: String,
    val postId: String? = null,
    val commentId: String? = null,
    val chatId: String? = null,
    val messageId: String? = null,
    val userId: String? = null,
    val notificationId: String? = null,
    /**
     * The `call_sessions` row a `CALL` target points at.
     *
     * Carried for completeness and used only as a deduplication key: opening a call never reads it. The
     * call screen resolves the ringing row from the caller's id, because a push is a hint and a row whose
     * id someone typed into a payload is not an invitation.
     */
    val callId: String? = null,
    /** The caller's display name, so a ring opened from a push can name somebody while it looks up the row. */
    val callerName: String? = null,
    /**
     * `voice` or `video` for a `CALL` target, straight from the payload.
     *
     * Needed rather than convenient: the media type decides which permission a call asks for before its
     * row is read, and opening a video ring as a voice one would have the screen reach for a camera it
     * never asked permission for.
     */
    val mediaType: String? = null
)

/** Buffers OneSignal taps until the app navigation graph is ready and deduplicates repeated SDK callbacks. */
object NotificationNavigationCoordinator {
    private const val DUPLICATE_WINDOW_MS = 5_000L
    private val requestChannel = Channel<NotificationNavigationRequest>(Channel.BUFFERED)
    private val recentRequests = LinkedHashMap<String, Long>()
    private val _pendingHomeNavigation = MutableStateFlow<NotificationNavigationRequest?>(null)

    val requests = requestChannel.receiveAsFlow()
    val pendingHomeNavigation = _pendingHomeNavigation.asStateFlow()

    fun onNotificationClick(additionalData: JSONObject?) {
        val request = parse(additionalData)
        val key = request.notificationId?.let { "notification:$it" }
            ?: listOf(request.target, request.postId, request.commentId, request.chatId, request.messageId, request.userId, request.callId)
                .joinToString(separator = ":") { it.orEmpty() }
        val now = SystemClock.elapsedRealtime()
        val accepted = synchronized(recentRequests) {
            recentRequests.entries.removeAll { now - it.value >= DUPLICATE_WINDOW_MS }
            if (recentRequests.containsKey(key)) {
                false
            } else {
                if (recentRequests.size >= 64) recentRequests.remove(recentRequests.keys.first())
                recentRequests[key] = now
                true
            }
        }
        if (!accepted) return
        if (requestChannel.trySend(request).isFailure) {
            synchronized(recentRequests) { recentRequests.remove(key) }
        }
    }
    fun requestHomeNavigation(request: NotificationNavigationRequest) {
        _pendingHomeNavigation.value = request
    }

    fun consumeHomeNavigation(request: NotificationNavigationRequest) {
        if (_pendingHomeNavigation.value == request) _pendingHomeNavigation.value = null
    }

    fun clearHomeNavigation() {
        _pendingHomeNavigation.value = null
    }

    private fun parse(data: JSONObject?): NotificationNavigationRequest {
        if (data == null) return NotificationNavigationRequest(target = "NOTIFICATIONS")

        val navigation = data.optJSONObject("navigation")
        val legacyTarget = firstString(data, "targetType", "target_type")?.uppercase(Locale.ROOT)
        val eventType = firstString(data, "type")?.uppercase(Locale.ROOT)
        val rawTarget = firstString(navigation, "target")?.uppercase(Locale.ROOT)
        val chatId = firstString(navigation, "chatId", "chat_id") ?: firstString(data, "chatId", "chat_id")
        val messageId = firstString(navigation, "messageId", "message_id") ?: firstString(data, "messageId", "message_id")
        val userId = firstString(navigation, "userId", "user_id")
            ?: firstString(data, "userId", "user_id", "followerId", "follower_id", "senderId", "sender_id")
        val targetId = firstString(data, "target_id", "targetId")
        val callId = firstString(navigation, "callId", "call_id") ?: firstString(data, "callId", "call_id")
        val callerId = firstString(data, "callerId", "caller_id")

        val target = when {
            rawTarget != null -> normalizeTarget(rawTarget)
            legacyTarget in setOf("FOLLOW_REQUESTS", "FOLLOW_REQUEST") || eventType == "FOLLOW_REQUEST" -> "FOLLOW_REQUESTS"
            eventType == "INCOMING_CALL" || (callId != null && callerId != null) -> "CALL"
            eventType == "NEW_MESSAGE" || chatId != null -> "CHAT"
            eventType in COMMENT_TYPES || legacyTarget == "POST_COMMENT" -> "POST_COMMENT"
            legacyTarget == "PROFILE" || legacyTarget == "USER" || eventType in PROFILE_TYPES -> "PROFILE"
            eventType in POST_TYPES || legacyTarget == "POST" -> "POST"
            firstString(data, "postId", "post_id") != null -> "POST"
            userId != null -> "PROFILE"
            else -> "NOTIFICATIONS"
        }

        val postId = firstString(navigation, "postId", "post_id")
            ?: firstString(data, "postId", "post_id")
            ?: targetId?.takeIf { target == "POST" || legacyTarget == "POST" }
        val commentId = firstString(navigation, "commentId", "comment_id")
            ?: firstString(data, "commentId", "comment_id")
            ?: targetId?.takeIf { target == "POST_COMMENT" && (legacyTarget == "POST_COMMENT" || eventType in COMMENT_TYPES) }

        return NotificationNavigationRequest(
            target = target,
            postId = postId,
            commentId = commentId,
            chatId = chatId,
            messageId = messageId,
            // A call is addressed from somebody, and that somebody is the peer the screen has to show.
            userId = if (target == "CALL") callerId ?: userId else userId,
            notificationId = firstString(data, "notificationId", "notification_id"),
            callId = callId,
            callerName = firstString(data, "callerName", "caller_name"),
            mediaType = firstString(navigation, "mediaType", "media_type")
                ?: firstString(data, "mediaType", "media_type")
        )
    }

    private fun normalizeTarget(target: String): String = when (target) {
        "POST", "POST_COMMENT", "CHAT", "PROFILE", "FOLLOW_REQUESTS", "NOTIFICATIONS", "CALL" -> target
        "USER" -> "PROFILE"
        "FOLLOW_REQUEST" -> "FOLLOW_REQUESTS"
        else -> "NOTIFICATIONS"
    }

    private fun firstString(source: JSONObject?, vararg keys: String): String? {
        for (key in keys) {
            val value = source?.opt(key)
            if (value is String) value.trim().takeIf { it.isNotEmpty() }?.let { return it }
        }
        return null
    }

    private val COMMENT_TYPES = setOf("NEW_COMMENT", "NEW_REPLY", "NEW_LIKE_COMMENT", "COMMENT", "REPLY", "LIKE_COMMENT")
    private val PROFILE_TYPES = setOf("NEW_FOLLOWER", "FOLLOW")
    private val POST_TYPES = setOf("NEW_POST", "NEW_LIKE_POST", "SHARE", "POST", "LIKE_POST")
}
