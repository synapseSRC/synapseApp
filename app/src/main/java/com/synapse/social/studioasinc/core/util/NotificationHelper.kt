package com.synapse.social.studioasinc.core.util

import android.util.Log
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalTime

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    private val dbService = SupabaseDatabaseService()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @JvmStatic
    fun sendNotification(
        recipientUid: String,
        senderUid: String,
        message: String,
        notificationType: String,
        data: Map<String, String>? = null
    ) {
        if (recipientUid.isNullOrEmpty() || senderUid.isNullOrEmpty()) {
            Log.w(TAG, "Recipient or sender UID is null or empty.")
            return
        }
        if (recipientUid == senderUid) {
            return
        }

        scope.launch {
            if (notificationType != "chat_message") {
                persistNotification(recipientUid, senderUid, message, notificationType, data)
            }

            try {
                val userResult = dbService.getSingle("users", "uid", recipientUid)
                val userData = userResult.getOrNull()

                if (userData == null) {
                    Log.e(TAG, "User not found for notification: $recipientUid")
                    return@launch
                }

                if (shouldSuppressPush(recipientUid)) {
                    Log.i(TAG, "Notification suppressed: Quiet Hours or DND active for user $recipientUid")
                    return@launch
                }

                val status = userData["status"] as? String
                val lastSeenStr = userData["last_seen"] as? String

                if (shouldSuppressNotification(status, lastSeenStr, senderUid)) {
                    Log.i(TAG, "Notification suppressed: User $recipientUid is active/chatting.")
                    return@launch
                }

                Log.i(TAG, "Sending notification to user $recipientUid via ${NotificationConfig.getNotificationSystemDescription()}")

                if (NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                    sendPushViaEdgeFunction(recipientUid, message, senderUid, notificationType, data)
                } else {
                    Log.i(TAG, "Server-side notifications configured (logic not in helper).")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in sendNotification flow", e)
            }
        }
    }

    private suspend fun shouldSuppressPush(recipientUid: String): Boolean {
        try {
            val result = dbService.getSingle("notification_preferences", "user_id", recipientUid)
            val prefs = result.getOrNull() ?: return false

            val dnd = prefs["do_not_disturb"] as? Boolean ?: false
            if (dnd) return true

            val quietHours = prefs["quiet_hours"]
            if (quietHours is Map<*, *>) {
                val enabled = quietHours["enabled"] as? Boolean ?: false
                if (enabled) {
                    val startStr = quietHours["start"] as? String
                    val endStr = quietHours["end"] as? String
                    if (startStr != null && endStr != null) {
                        return isCurrentTimeInWindow(startStr, endStr)
                    }
                }
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check notification preferences", e)
            return false
        }
    }

    private fun isCurrentTimeInWindow(start: String, end: String): Boolean {
        try {
            val now = LocalTime.now()
            val startTime = LocalTime.parse(start)
            val endTime = LocalTime.parse(end)

            return if (startTime.isBefore(endTime)) {
                now.isAfter(startTime) && now.isBefore(endTime)
            } else {
                now.isAfter(startTime) || now.isBefore(endTime)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time window", e)
            return false
        }
    }

    @JvmStatic
    private fun shouldSuppressNotification(status: String?, lastSeenStr: String?, chattingWith: String?): Boolean {
        if (status == "online") return true
        return false
    }

    @JvmStatic
    fun sendPushViaEdgeFunction(
        recipientUid: String,
        message: String,
        senderUid: String?,
        notificationType: String,
        data: Map<String, String>? = null
    ) {
        try {
            val request = mapOf(
                "recipient_id" to recipientUid,
                "message" to message,
                "type" to notificationType,
                "headings" to mapOf("en" to NotificationConfig.getTitleForNotificationType(notificationType)),
                "sender_id" to senderUid,
                "data" to data
            )

            scope.launch {
                try {
                    val response = SupabaseClient.client.functions.invoke(
                        function = NotificationConfig.EDGE_FUNCTION_SEND_PUSH,
                        body = request
                    )
                    Log.i(TAG, "Push notification sent via Edge Function.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to invoke Edge Function", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send push notification via Edge Function", e)
        }
    }

    private suspend fun persistNotification(
        recipientUid: String,
        senderUid: String,
        message: String,
        notificationType: String,
        data: Map<String, String>?
    ) {
        try {
            val targetId = data?.get("postId")
                ?: data?.get("commentId")
                ?: data?.get("followerId")
                ?: data?.get("chat_id")

            val notificationData = mutableMapOf<String, Any?>(
                "recipient_id" to recipientUid,
                "sender_id" to senderUid,
                "type" to notificationType,
                "data" to (data?.toMutableMap() ?: mutableMapOf()).apply {
                    put("message", message)
                    if (targetId != null) put("target_id", targetId)
                },
                "is_read" to false,
                "created_at" to java.time.Instant.now().toString()
            )

            notificationData["body"] = mapOf("en" to message)

            val result = dbService.insert("notifications", notificationData)

            if (result.isFailure) {
                Log.w(TAG, "Failed to persist notification: ${result.exceptionOrNull()?.message}")
            } else {
                Log.d(TAG, "Notification persisted to Supabase DB")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting notification", e)
        }
    }

    @JvmStatic
    fun sendMessageAndNotifyIfNeeded(chatId: String, senderId: String, recipientId: String, message: String) {
        sendNotification(recipientId, senderId, message, "chat_message", mapOf("chat_id" to chatId))
    }
}
