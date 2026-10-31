package com.synapse.social.studioasinc.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.main.MainActivity
import com.synapse.social.studioasinc.service.echo.EchoRoomActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The few facts a notification needs about the room this process is listening to. */
data class EchoNotificationContent(
    val roomId: String,
    val title: String,
    val isPublishing: Boolean
)

/**
 * The ongoing notification an Echo Room is anchored to.
 *
 * It exists because the foreground service needs one, and it says only what the process can prove:
 * the room's title and that audio is running. There is deliberately no speaker or listener count —
 * the notification is drawn by a process that may have lost its realtime subscription, and a stale
 * number would be worse than none.
 */
@Singleton
class EchoNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val manager = NotificationManagerCompat.from(context)

    init {
        createChannel()
    }

    /** Public because the foreground service has to build it before `startForeground`. */
    fun buildActiveRoomNotification(content: EchoNotificationContent): Notification {
        val roomTitle = content.title.ifBlank { context.getString(R.string.echo_notification_unknown_room) }
        return NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.ic_echo_notification)
            .setContentTitle(context.getString(R.string.echo_notification_title))
            .setContentText(roomTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(roomTitle))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent(content.roomId))
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_echo_notification,
                    context.getString(R.string.echo_notification_leave),
                    leaveIntent(content.roomId)
                ).build()
            )
            .build()
    }

    fun activeNotificationId(): Int = ACTIVE_NOTIFICATION_ID

    fun dismissActiveRoom() {
        manager.cancel(ACTIVE_NOTIFICATION_ID)
    }

    /**
     * Opens the room inside the app's own navigation graph.
     *
     * The deep link is set as data rather than as a route extra so it takes the exact same path a
     * link shared into the app would; `MainActivity` already forwards `ACTION_VIEW` intents to
     * the navigation controller.
     */
    private fun openIntent(roomId: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("synapse://echo/$roomId"))
            .setClass(context, MainActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        return PendingIntent.getActivity(
            context,
            roomId.hashCode() xor REQ_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun leaveIntent(roomId: String): PendingIntent {
        val intent = Intent(context, EchoRoomActionReceiver::class.java)
            .setAction(EchoRoomActionReceiver.ACTION_LEAVE)
            .putExtra(EchoRoomActionReceiver.EXTRA_ROOM_ID, roomId)
        return PendingIntent.getBroadcast(
            context,
            roomId.hashCode() xor REQ_LEAVE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ACTIVE,
            context.getString(R.string.echo_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.echo_notification_channel_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ACTIVE = "echo_room_channel"
        const val ACTIVE_NOTIFICATION_ID = 0x4543_484F
        const val REQ_OPEN = 1
        const val REQ_LEAVE = 2
    }
}
