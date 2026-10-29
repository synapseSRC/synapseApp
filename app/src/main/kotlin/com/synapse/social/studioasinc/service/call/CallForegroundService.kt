package com.synapse.social.studioasinc.service.call

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.synapse.social.studioasinc.core.util.CallNotificationContent
import com.synapse.social.studioasinc.core.util.CallNotificationHelper
import com.synapse.social.studioasinc.feature.call.CallActivity
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import dagger.hilt.android.AndroidEntryPoint
import io.github.aakira.napier.Napier
import javax.inject.Inject

/**
 * The reason this process is allowed to keep the microphone open when its screen is not on top.
 *
 * A call is one of the few things Android still lets run with the app in the background, but only if the
 * system can see *which* call — hence the ongoing notification this service is pinned to. The service
 * owns no call state at all: it neither starts nor stops media, and it does not survive a call it was
 * not told about. `WebRtcCallRepository` opens capture and asks for this hold through [CallMediaHold],
 * and that ordering is what makes it impossible for the notification to describe a call that is not
 * actually using hardware.
 *
 * The type follows the media: `microphone` for a voice call, `microphone|camera` once video is up, which
 * is also why an upgrade re-enters here instead of being ignored.
 */
@AndroidEntryPoint
class CallForegroundService : Service() {

    @Inject lateinit var notifications: CallNotificationHelper

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val content = intent?.callContent()
        // Nothing to hold: a stale redelivery, or an intent that named no call. A restarted service must
        // not present itself as a live call, so it says nothing and goes away.
        if (content == null) {
            Napier.w("Call media hold started without a call; not promoted")
            stopSelf()
            return START_NOT_STICKY
        }
        if (!promote(content)) {
            // Either the platform refused the type or the notification could not be posted. Media is the
            // repository's business and continues; what is lost is the licence to keep it up with the
            // screen off, which the user finds out about the moment they lock the phone.
            Napier.w("Call media hold could not go foreground")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // The notification is the user's only clue that a call is still running, so leaving it behind
        // after the service dies would be worse than the service dying.
        notifications.dismissActiveCall()
        super.onDestroy()
    }

    /**
     * Promotes this service, notification first.
     *
     * `startForeground` throws if the declared type is one this app may not hold — the microphone type
     * needs the audio permission, the camera type needs the camera one — and Android 12+ throws when a
     * backgrounded app asks at all. Returning false rather than propagating keeps that platform decision
     * from taking a live call down with it.
     */
    private fun promote(content: CallNotificationContent): Boolean {
        val type = if (content.mediaType == CallMediaType.VIDEO) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        return try {
            ServiceCompat.startForeground(
                this,
                notifications.activeNotificationId(),
                notifications.buildActiveCallNotification(content),
                type
            )
            true
        } catch (e: Exception) {
            Napier.w("startForeground refused: ${e.message}")
            false
        }
    }

    companion object {
        private const val ACTION_START =
            "com.synapse.social.studioasinc.call.action.START_MEDIA_HOLD"
        private const val ACTION_STOP =
            "com.synapse.social.studioasinc.call.action.STOP_MEDIA_HOLD"

        /**
         * Asks for the hold on a call that is using hardware now.
         *
         * Safe to call again with a different media type: the service is already running, so the second
         * command re-promotes it with the new type and the new text, which is how a voice call that
         * becomes a video call keeps its licence to use the camera.
         */
        fun start(context: Context, content: CallNotificationContent) {
            val intent = Intent(context, CallForegroundService::class.java)
                .setAction(ACTION_START)
                .putExtra(CallActivity.EXTRA_CALL_ID, content.callId)
                .putExtra(CallActivity.EXTRA_PEER_USER_ID, content.participantId)
                .putExtra(CallActivity.EXTRA_PEER_NAME, content.participantName)
                .putExtra(CallActivity.EXTRA_PEER_AVATAR, content.avatarUrl)
                .putExtra(CallActivity.EXTRA_CHAT_ID, content.chatId)
                .putExtra(CallActivity.EXTRA_IS_VIDEO, content.mediaType == CallMediaType.VIDEO)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Napier.w("Call media hold could not be requested: ${it.message}") }
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, CallForegroundService::class.java).setAction(ACTION_STOP)
                )
            }.onFailure { Napier.w("Call media hold could not be released: ${it.message}") }
        }

        /** Reads the call back out of the intent, or null when the extras do not describe one. */
        private fun Intent.callContent(): CallNotificationContent? {
            val callId = getStringExtra(CallActivity.EXTRA_CALL_ID)
            val participantId = getStringExtra(CallActivity.EXTRA_PEER_USER_ID)
            if (callId.isNullOrBlank() || participantId.isNullOrBlank()) return null
            return CallNotificationContent(
                callId = callId,
                participantId = participantId,
                participantName = getStringExtra(CallActivity.EXTRA_PEER_NAME).orEmpty(),
                avatarUrl = getStringExtra(CallActivity.EXTRA_PEER_AVATAR),
                chatId = getStringExtra(CallActivity.EXTRA_CHAT_ID),
                mediaType = if (getBooleanExtra(CallActivity.EXTRA_IS_VIDEO, false)) {
                    CallMediaType.VIDEO
                } else {
                    CallMediaType.VOICE
                }
            )
        }
    }
}
