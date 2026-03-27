package com.synapse.social.studioasinc.core.util

import android.util.Log
import com.synapse.social.studioasinc.core.SynapseApplication
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

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
            persistNotification(recipientUid, senderUid, message, notificationType, data)

            try {
                val userResult = dbService.getSingle("users", "uid", recipientUid)
                val userData = userResult.getOrNull()

                if (userData == null) {
                    Log.e(TAG, "User not found for notification: $recipientUid")
                    return@launch
                }

                if (shouldSuppressPush(recipientUid, notificationType)) {
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
                    sendPushViaOneSignal(recipientUid, message, senderUid, notificationType, data)
                } else {
                    sendPushViaEdgeFunction(recipientUid, message, senderUid, notificationType, data)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in sendNotification flow", e)
            }
        }
    }

    private suspend fun shouldSuppressPush(recipientUid: String, notificationType: String? = null): Boolean {
        try {
            val result = dbService.getSingle("notification_preferences", "user_id", recipientUid)
            val prefs = result.getOrNull() ?: return false

            val dnd = prefs["do_not_disturb"] as? Boolean ?: false
            if (dnd) return true

            if (notificationType != null) {
                val settings = prefs["settings"]
                if (settings is Map<*, *>) {
                    val categoryEnabled = when (notificationType) {
                        NotificationConfig.NOTIFICATION_TYPE_NEW_MESSAGE -> settings["messages_enabled"] as? Boolean
                        NotificationConfig.NOTIFICATION_TYPE_NEW_COMMENT -> settings["comments_enabled"] as? Boolean
                        NotificationConfig.NOTIFICATION_TYPE_NEW_REPLY -> settings["replies_enabled"] as? Boolean
                        NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_POST,
                        NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_COMMENT -> settings["likes_enabled"] as? Boolean
                        NotificationConfig.NOTIFICATION_TYPE_MENTION -> settings["mentions_enabled"] as? Boolean
                        NotificationConfig.NOTIFICATION_TYPE_NEW_FOLLOWER -> settings["follows_enabled"] as? Boolean
                        NotificationConfig.NOTIFICATION_TYPE_NEW_POST -> settings["new_posts_enabled"] as? Boolean
                        else -> null
                    }
                    if (categoryEnabled == false) return true
                }
            }

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
    private fun sendPushViaOneSignal(
        recipientUid: String,
        message: String,
        senderUid: String?,
        notificationType: String,
        data: Map<String, String>? = null
    ) {
        // OneSignal client SDK cannot send to other users
        // Must use REST API via Edge Function
        sendPushViaEdgeFunction(recipientUid, message, senderUid, notificationType, data)
    }

    @JvmStatic
    private fun sendPushViaEdgeFunction(
        recipientUid: String,
        message: String,
        senderUid: String?,
        notificationType: String,
        data: Map<String, String>? = null
    ) {
        try {
            val request = buildJsonObject {
                put("recipient_id", recipientUid)
                put("message", message)
                put("type", notificationType)
                putJsonObject("headings") {
                    put("en", NotificationConfig.getTitleForNotificationType(SynapseApplication.instance, notificationType))
                }
                if (senderUid != null) {
                    put("sender_id", senderUid)
                }
                if (data != null) {
                    putJsonObject("data") {
                        data.forEach { (key, value) ->
                            put(key, value)
                        }
                    }
                }
            }

            Log.d(TAG, "Sending notification: recipient=$recipientUid, type=$notificationType")

            scope.launch {
                try {
                    val response = SupabaseClient.client.functions.invoke(
                        function = NotificationConfig.EDGE_FUNCTION_SEND_PUSH,
                        body = request
                    )
                    Log.i(TAG, "Push notification sent successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to invoke Edge Function: ${e.message}", e)
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
        sendNotification(
            recipientId, 
            senderId, 
            message, 
            NotificationConfig.NOTIFICATION_TYPE_NEW_MESSAGE, 
            mapOf("chat_id" to chatId)
        )
    }

    @JvmStatic
    fun dismissNotification(context: android.content.Context, notificationId: Int) {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        try {
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to dismiss notification", e)
        }
    }

    @JvmStatic
    fun showProgressNotification(context: android.content.Context, notificationId: Int, progress: Int, title: String) {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Ensure channel exists
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "upload_channel",
                "Media Uploads",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for media uploads"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(context, "upload_channel")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setOngoing(progress < 100)
            .setOnlyAlertOnce(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)

        if (progress < 100) {
            builder.setProgress(100, progress, false)
                .setContentText(context.getString(R.string.notification_upload_progress, progress))
        } else {
            builder.setProgress(0, 0, false)
                .setContentText(context.getString(R.string.upload_complete))
                // Auto cancel when complete
                .setAutoCancel(true)
        }

        // Note: For Android 13+ (API 33), POST_NOTIFICATIONS permission is required.
        // We assume it is already handled by the app for other notifications.
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to show progress notification", e)
        }
    }
}
