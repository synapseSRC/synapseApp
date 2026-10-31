package com.synapse.social.studioasinc.service.echo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.synapse.social.studioasinc.core.util.EchoNotificationHelper
import com.synapse.social.studioasinc.shared.domain.usecase.echo.LeaveEchoRoomUseCase
import dagger.hilt.android.AndroidEntryPoint
import io.github.aakira.napier.Napier
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The notification's Leave button.
 *
 * It runs the same use case the room screen does — disconnect first, then the membership row — so
 * there is one definition of leaving. The service is stopped with `stopService`, which is allowed
 * from the background, rather than by sending it another start command; `onDestroy` then removes
 * the notification even if the network write below fails.
 *
 * `goAsync` is not decoration: without it the process may be torn down as soon as `onReceive`
 * returns, and the leave would be a broadcast the backend never hears.
 */
@AndroidEntryPoint
class EchoRoomActionReceiver : BroadcastReceiver() {

    @Inject lateinit var leaveEchoRoom: LeaveEchoRoomUseCase

    @Inject lateinit var notifications: EchoNotificationHelper

    @Inject
    @field:Named("ApplicationScope")
    lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LEAVE) return
        val roomId = intent.getStringExtra(EXTRA_ROOM_ID)
        if (roomId.isNullOrBlank()) return

        notifications.dismissActiveRoom()
        runCatching { context.stopService(Intent(context, EchoRoomForegroundService::class.java)) }

        val pending = goAsync()
        appScope.launch {
            try {
                leaveEchoRoom(roomId).onFailure { error ->
                    // The row refused — already ended, or not this member's to change. Audio is
                    // disconnected either way and the backend remains the source of truth.
                    Napier.w("Leave from the notification shade refused: ${error.message}")
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_LEAVE = "com.synapse.social.studioasinc.echo.action.LEAVE"
        const val EXTRA_ROOM_ID = "com.synapse.social.studioasinc.echo.extra.ROOM_ID"
    }
}
