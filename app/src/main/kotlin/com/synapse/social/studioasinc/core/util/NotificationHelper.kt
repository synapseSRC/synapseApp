package com.synapse.social.studioasinc.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.synapse.social.studioasinc.R

/**
 * Remote notifications are dispatched by trusted Supabase database triggers after
 * the corresponding message or notification row is committed. This class only
 * retains local notification utilities and a compatibility shim for old call sites.
 */
object NotificationHelper {
    private const val TAG = "NotificationHelper"

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun sendNotification(
        recipientUid: String,
        senderUid: String,
        message: String,
        notificationType: String,
        data: Map<String, String>? = null
    ) {
        if (recipientUid.isBlank() || senderUid.isBlank() || recipientUid == senderUid) return

        Log.d(
            TAG,
            "Client push request suppressed; Supabase database triggers dispatch remote notifications (type=$notificationType)."
        )
    }

    /**
     * Compatibility shim. The message INSERT trigger now initiates remote push delivery.
     */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun sendMessageAndNotifyIfNeeded(chatId: String, senderId: String, recipientId: String, message: String) {
        Log.d(TAG, "Message push is dispatched by the Supabase messages INSERT trigger.")
    }

    @JvmStatic
    fun dismissNotification(context: Context, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss notification", e)
        }
    }

    @JvmStatic
    fun showProgressNotification(context: Context, notificationId: Int, progress: Int, title: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "upload_channel",
                "Media Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for media uploads"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, "upload_channel")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setOngoing(progress < 100)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress < 100) {
            builder.setProgress(100, progress, false)
                .setContentText(context.getString(R.string.notification_upload_progress, progress))
        } else {
            builder.setProgress(0, 0, false)
                .setContentText(context.getString(R.string.upload_complete))
                .setAutoCancel(true)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show progress notification", e)
        }
    }
}
