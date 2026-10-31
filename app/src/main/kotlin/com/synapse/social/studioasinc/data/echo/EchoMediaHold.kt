package com.synapse.social.studioasinc.data.echo

import android.content.Context
import com.synapse.social.studioasinc.core.util.EchoNotificationContent
import com.synapse.social.studioasinc.service.echo.EchoRoomForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The licence to keep an Echo Room's audio running with the screen off.
 *
 * Mirrors the call stack's `CallMediaHold`: a room that is genuinely connected gets the hold, and a
 * room that is not cannot hold a notification that claims it is. The type follows the role —
 * a listener needs `mediaPlayback`, a host or speaker adds `microphone` — which is why acquiring
 * again after a promotion upgrades the same service instead of being ignored.
 */
interface EchoMediaHold {
    /** Claims the hold for the role described by [content]. Re-acquiring upgrades the type. */
    fun acquire(content: EchoNotificationContent)

    /** Gives the hold back. Safe to call when nothing was acquired. */
    fun release()
}

@Singleton
class EchoForegroundServiceHold @Inject constructor(
    @ApplicationContext private val context: Context
) : EchoMediaHold {

    override fun acquire(content: EchoNotificationContent) = EchoRoomForegroundService.start(context, content)

    override fun release() = EchoRoomForegroundService.stop(context)
}
