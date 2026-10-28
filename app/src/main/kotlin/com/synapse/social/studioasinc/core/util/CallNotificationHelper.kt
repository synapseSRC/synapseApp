package com.synapse.social.studioasinc.core.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.call.CallActivity
import com.synapse.social.studioasinc.service.call.CallActionReceiver
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aakira.napier.Napier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything a notification has to say about a call.
 *
 * A narrowed copy of [CallSession], because a notification is built by a process that may know nothing
 * else about the call: a service restarted by the system, or a broadcast from the shade. It carries the
 * identifiers needed to reopen the right call and nothing more — no media state, no phase, nothing that
 * could be read as "this call is connected" by something that cannot prove it.
 */
data class CallNotificationContent(
    val callId: String,
    val participantId: String,
    val participantName: String,
    val avatarUrl: String?,
    val chatId: String?,
    val mediaType: CallMediaType
)

/** The notification view of a session the backend just described. */
fun CallSession.toNotificationContent(): CallNotificationContent = CallNotificationContent(
    callId = id,
    participantId = participant.userId,
    participantName = participant.displayName,
    avatarUrl = participant.avatarUrl,
    chatId = chatId,
    mediaType = mediaType
)

/**
 * The ringing and ongoing notifications a call needs.
 *
 * Two channels, because they answer two different questions. `incoming_call_channel` is the one that has
 * to interrupt: high importance, a sound, a vibration, a category the system treats as a call, and — where
 * the user has allowed it — a full-screen intent that puts [CallActivity] over the lock screen.
 * `call_active_channel` is the quiet one that keeps a live call visible and lets the foreground service
 * hold the microphone.
 *
 * Everything a user taps here is an *intent*, not work done in this class: accept opens the call screen
 * with a flag that answers it once the microphone question is settled, and decline goes through
 * [CallActionReceiver], which writes the transition to the backend. A notification can therefore never
 * mark a call connected — that still needs a row and a real ICE link.
 */
@Singleton
class CallNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val manager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    // ---- Incoming ring ---------------------------------------------------------

    /**
     * Shows [content] as a call waiting for an answer.
     *
     * Re-posting the same call replaces the notification rather than stacking a second one, because a
     * realtime row arrives several times for one ring and each copy would sound again.
     */
    fun showIncomingCall(content: CallNotificationContent) {
        try {
            manager.notify(incomingNotificationId(content.callId), buildIncomingNotification(content))
        } catch (e: SecurityException) {
            // Notifications are a delivery path, never the point of a call. Without the permission the
            // realtime row still rings the screen while the app is open, and that is what we can promise.
            Napier.w("Incoming call notification refused (POST_NOTIFICATIONS): ${e.message}")
        }
    }

    fun dismissIncomingCall(callId: String) {
        manager.cancel(incomingNotificationId(callId))
    }

    /**
     * Full-screen-intent access, which Android 14+ no longer grants to an app that is not the user's
     * chosen calling account.
     *
     * False is not a failure: the notification still arrives as a heads-up, and the user can still open
     * the call. Saying so is the point — a ring the user cannot see because the screen stayed locked is
     * the bug this check exists to keep honest rather than pretend away.
     */
    fun canUseFullScreenIntent(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager.canUseFullScreenIntent()
        } else {
            context.packageManager.checkPermission(
                Manifest.permission.USE_FULL_SCREEN_INTENT,
                context.packageName
            ) == PackageManager.PERMISSION_GRANTED
        }

    /** Opens the system page where the user can allow calls to wake the screen; false if it cannot. */
    fun openFullScreenIntentSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    // ---- Ongoing call ----------------------------------------------------------

    /**
     * The notification a live call is anchored to: persistent, silent, and the reason the foreground
     * service is allowed to keep the microphone open.
     *
     * Public because [com.synapse.social.studioasinc.service.call.CallForegroundService] has to build it
     * before `startForeground` — the platform wants that notification to exist by the moment the service
     * is promoted, not one posted a moment later.
     */
    fun buildActiveCallNotification(content: CallNotificationContent): Notification {
        val body = activeBody(content)
        return NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(context.getString(R.string.call_notification_active_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent(content))
            .build()
    }

    fun activeNotificationId(): Int = ACTIVE_NOTIFICATION_ID

    fun dismissActiveCall() {
        manager.cancel(ACTIVE_NOTIFICATION_ID)
    }

    // ---- Construction ----------------------------------------------------------

    private fun buildIncomingNotification(content: CallNotificationContent): Notification {
        val title = if (content.mediaType == CallMediaType.VIDEO) {
            context.getString(R.string.call_notification_incoming_video, callerName(content))
        } else {
            context.getString(R.string.call_notification_incoming_voice, callerName(content))
        }
        val hint = context.getString(R.string.call_notification_incoming_hint)
        return NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(title)
            .setContentText(hint)
            .setStyle(NotificationCompat.BigTextStyle().bigText(hint))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openIntent(content))
            .setDeleteIntent(actionIntent(content.callId, CallActionReceiver.ACTION_DISMISS))
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_call_notification,
                    context.getString(R.string.call_notification_action_accept),
                    acceptIntent(content)
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_call_decline,
                    context.getString(R.string.call_notification_action_decline),
                    actionIntent(content.callId, CallActionReceiver.ACTION_DECLINE)
                ).build()
            )
            .apply {
                // A locked screen is where an incoming call matters most, so the intent that can beat it
                // goes in whenever the platform allows one; otherwise the heads-up view is the fallback.
                if (canUseFullScreenIntent()) setFullScreenIntent(openIntent(content), true)
            }
            .build()
    }

    /**
     * Opening a ring and answering it are separate, so they are separate intents.
     *
     * Only the accept action sets [CallActivity.EXTRA_AUTO_ACCEPT]: a tap on the body should show who is
     * calling and let the user decide, while the button means the decision was already made.
     */
    private fun openIntent(content: CallNotificationContent): PendingIntent =
        callActivityIntent(content, autoAccept = false)

    private fun acceptIntent(content: CallNotificationContent): PendingIntent =
        callActivityIntent(content, autoAccept = true)

    private fun callActivityIntent(content: CallNotificationContent, autoAccept: Boolean): PendingIntent {
        val intent = Intent(context, CallActivity::class.java)
            .putExtra(CallActivity.EXTRA_CALL_ID, content.callId)
            .putExtra(CallActivity.EXTRA_PEER_USER_ID, content.participantId)
            .putExtra(CallActivity.EXTRA_PEER_NAME, content.participantName)
            .putExtra(CallActivity.EXTRA_PEER_AVATAR, content.avatarUrl)
            .putExtra(CallActivity.EXTRA_CHAT_ID, content.chatId)
            .putExtra(CallActivity.EXTRA_IS_VIDEO, content.mediaType == CallMediaType.VIDEO)
            .putExtra(CallActivity.EXTRA_AUTO_ACCEPT, autoAccept)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            requestCode(content.callId, if (autoAccept) REQ_ACCEPT else REQ_OPEN),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(callId: String, action: String): PendingIntent {
        val intent = Intent(context, CallActionReceiver::class.java)
            .setAction(action)
            .putExtra(CallActivity.EXTRA_CALL_ID, callId)
        val kind = if (action == CallActionReceiver.ACTION_DECLINE) REQ_DECLINE else REQ_DISMISS
        return PendingIntent.getBroadcast(
            context,
            requestCode(callId, kind),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun callerName(content: CallNotificationContent): String =
        content.participantName.ifBlank { context.getString(R.string.call_notification_unknown_caller) }

    private fun activeBody(content: CallNotificationContent): String =
        if (content.mediaType == CallMediaType.VIDEO) {
            context.getString(R.string.call_notification_active_video, callerName(content))
        } else {
            context.getString(R.string.call_notification_active_voice, callerName(content))
        }

    /**
     * A stable id per call, so a second realtime update about the same ring replaces its own notification
     * instead of stacking one that sounds again.
     *
     * The low bit keeps the ringing view apart from the ongoing one: the same call occupies both at
     * different times, and they must not cancel each other.
     */
    private fun incomingNotificationId(callId: String): Int =
        (callId.hashCode() and NOTIFICATION_ID_MASK) * 2

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attributes = AudioAttributes.Builder()
            // A ring is not an alert to read later: this is the usage the system ducks other audio for
            // and routes to the right output. API 28 introduced it; before that the notification usage
            // with a speech content type is the closest the platform had.
            .setUsage(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST
                } else {
                    AudioAttributes.USAGE_NOTIFICATION
                }
            )
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val incoming = NotificationChannel(
            CHANNEL_INCOMING,
            context.getString(R.string.call_notification_channel_incoming_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.call_notification_channel_incoming_description)
            setSound(ringUri, attributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 500, 500)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }

        val active = NotificationChannel(
            CHANNEL_ACTIVE,
            context.getString(R.string.call_notification_channel_active_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.call_notification_channel_active_description)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(listOf(incoming, active))
    }

    private fun requestCode(callId: String, kind: Int): Int = callId.hashCode() xor kind

    private companion object {
        const val CHANNEL_INCOMING = "incoming_call_channel"
        const val CHANNEL_ACTIVE = "call_active_channel"
        const val ACTIVE_NOTIFICATION_ID = 0x4341_4C4C
        const val NOTIFICATION_ID_MASK = 0x3FF_FFFF
        const val REQ_OPEN = 1
        const val REQ_ACCEPT = 2
        const val REQ_DECLINE = 3
        const val REQ_DISMISS = 4
    }
}
