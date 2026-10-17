package com.synapse.social.studioasinc.core.util

import com.synapse.social.studioasinc.R

object NotificationHelper {
    private const val TAG = "NotificationHelper"

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
