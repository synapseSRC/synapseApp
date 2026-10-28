package com.synapse.social.studioasinc.data.call

import android.content.Context
import com.synapse.social.studioasinc.core.util.CallNotificationContent
import com.synapse.social.studioasinc.service.call.CallForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The licence to keep using the microphone and camera while the call's screen is not on top.
 *
 * A call that is genuinely running should survive the user pressing home; a call that is not should not
 * be able to keep a microphone open at all. Putting that between the repository and the platform as one
 * named thing — *acquire it when capture is open, release it when capture is gone* — is what keeps the
 * notification honest: nothing here can be acquired before hardware is running, and nothing can outlive
 * the teardown that released it.
 *
 * A seam rather than a direct service call so the repository depends on a capability instead of on an
 * Android component class, and so the debug build's simulated call — which captures nothing — is never
 * the thing that puts an "ongoing call" notification on a device that is only pretending.
 */
interface CallMediaHold {
    /** Claims the hold for a call that is capturing [content]'s media right now. */
    fun acquire(content: CallNotificationContent)

    /** Gives the hold back. Safe to call when nothing was acquired. */
    fun release()
}

@Singleton
class CallForegroundServiceHold @Inject constructor(
    @ApplicationContext private val context: Context
) : CallMediaHold {

    override fun acquire(content: CallNotificationContent) = CallForegroundService.start(context, content)

    override fun release() = CallForegroundService.stop(context)
}
