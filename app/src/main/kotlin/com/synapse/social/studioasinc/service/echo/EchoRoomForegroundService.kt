package com.synapse.social.studioasinc.service.echo

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.synapse.social.studioasinc.core.util.EchoNotificationContent
import com.synapse.social.studioasinc.core.util.EchoNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import io.github.aakira.napier.Napier
import javax.inject.Inject

/**
 * Keeps an Echo Room audible while its screen is not on top.
 *
 * The service owns no room state: `IsSpeaker` only chooses the foreground type the media justifies,
 * and nothing here connects, mutes or leaves. `EchoRoomViewModel` asks for the hold after the role
 * is known and releases it on teardown, so the notification can never describe a room this process
 * is not in.
 *
 * A listener holds `mediaPlayback` — no runtime permission is involved. A host or speaker re-enters
 * with `microphone` added, which is the upgrade that lets the OS keep capture alive after the user
 * leaves the app.
 */
@AndroidEntryPoint
class EchoRoomForegroundService : Service() {

    @Inject lateinit var notifications: EchoNotificationHelper

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val content = intent?.roomContent()
        if (content == null) {
            Napier.w("Echo media hold started without a room; not promoted")
            stopSelf()
            return START_NOT_STICKY
        }
        if (!promote(content)) {
            // The audio is the repository's business and continues while the screen is on; what is
            // lost is the licence to keep it up in the background, which is worth a log, not a crash.
            Napier.w("Echo media hold could not go foreground")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        notifications.dismissActiveRoom()
        super.onDestroy()
    }

    private fun promote(content: EchoNotificationContent): Boolean {
        val playback = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        // Android 14 refuses the microphone type while the app is in the background (e.g. a
        // promotion arriving with the screen off). Falling back to playback keeps the room audible
        // instead of dropping the whole hold.
        val types = if (content.isPublishing) {
            listOf(playback or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE, playback)
        } else {
            listOf(playback)
        }
        val notification = notifications.buildActiveRoomNotification(content)
        for (type in types) {
            try {
                ServiceCompat.startForeground(this, notifications.activeNotificationId(), notification, type)
                return true
            } catch (e: Exception) {
                Napier.w("Echo startForeground($type) refused: ${e.message}")
            }
        }
        return false
    }

    companion object {
        private const val ACTION_START =
            "com.synapse.social.studioasinc.echo.action.START_MEDIA_HOLD"
        private const val ACTION_STOP =
            "com.synapse.social.studioasinc.echo.action.STOP_MEDIA_HOLD"
        private const val EXTRA_ROOM_ID = "com.synapse.social.studioasinc.echo.extra.ROOM_ID"
        private const val EXTRA_ROOM_TITLE = "com.synapse.social.studioasinc.echo.extra.ROOM_TITLE"
        private const val EXTRA_PUBLISHING = "com.synapse.social.studioasinc.echo.extra.PUBLISHING"

        fun start(context: Context, content: EchoNotificationContent) {
            val intent = Intent(context, EchoRoomForegroundService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ROOM_ID, content.roomId)
                .putExtra(EXTRA_ROOM_TITLE, content.title)
                .putExtra(EXTRA_PUBLISHING, content.isPublishing)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Napier.w("Echo media hold could not be requested: ${it.message}") }
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, EchoRoomForegroundService::class.java).setAction(ACTION_STOP)
                )
            }.onFailure { Napier.w("Echo media hold could not be released: ${it.message}") }
        }

        private fun Intent.roomContent(): EchoNotificationContent? {
            val roomId = getStringExtra(EXTRA_ROOM_ID)
            if (roomId.isNullOrBlank()) return null
            return EchoNotificationContent(
                roomId = roomId,
                title = getStringExtra(EXTRA_ROOM_TITLE).orEmpty(),
                isPublishing = getBooleanExtra(EXTRA_PUBLISHING, false)
            )
        }
    }
}
