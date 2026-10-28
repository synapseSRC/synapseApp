package com.synapse.social.studioasinc.service.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.synapse.social.studioasinc.core.util.CallNotificationHelper
import com.synapse.social.studioasinc.feature.call.CallActivity
import com.synapse.social.studioasinc.shared.domain.usecase.call.DeclineCallInvitationUseCase
import dagger.hilt.android.AndroidEntryPoint
import io.github.aakira.napier.Napier
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The buttons on the incoming-call notification that do not need a screen.
 *
 * Decline is the only one. It writes the transition straight to the backend, which is correct in both
 * directions: if this device has the call open, its realtime subscription sees the ended row and tears
 * the media down like any other remote change, and if it does not — the app was closed when the call
 * arrived — there is no local media to release and the caller still learns the answer.
 *
 * A swipe away is *not* a decline. `ACTION_DISMISS` stops the alert and leaves the row ringing, because
 * the person on the other end is still waiting, and the honest outcome for an unanswered ring comes from
 * the server's own deadline (`missed`), not from a notification the user cleared to get their screen
 * back.
 *
 * There is deliberately no accept here. Answering means capturing audio, and this process has no UI to
 * ask for the microphone with — the accept action opens [CallActivity] instead, where the question can
 * actually be asked.
 */
@AndroidEntryPoint
class CallActionReceiver : BroadcastReceiver() {

    @Inject lateinit var declineCallInvitation: DeclineCallInvitationUseCase

    @Inject lateinit var notifications: CallNotificationHelper

    @Inject
    @field:Named("ApplicationScope")
    lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(CallActivity.EXTRA_CALL_ID)
        if (callId.isNullOrBlank()) return
        when (intent.action) {
            ACTION_DECLINE -> {
                notifications.dismissIncomingCall(callId)
                appScope.launch {
                    declineCallInvitation(callId).onFailure { error ->
                        // The row said no for a reason — the call already ended, or this is not the
                        // callee's to decline. Either way the notification is gone and the truth is on
                        // the backend, so nothing here is left ringing.
                        Napier.w("Decline from the notification shade refused: ${error.message}")
                    }
                }
            }

            ACTION_DISMISS -> notifications.dismissIncomingCall(callId)
        }
    }

    companion object {
        const val ACTION_DECLINE = "com.synapse.social.studioasinc.call.action.DECLINE"
        const val ACTION_DISMISS = "com.synapse.social.studioasinc.call.action.DISMISS"
    }
}
