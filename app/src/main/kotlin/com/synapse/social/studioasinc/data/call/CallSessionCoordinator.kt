package com.synapse.social.studioasinc.data.call

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.synapse.social.studioasinc.core.util.CallNotificationHelper
import com.synapse.social.studioasinc.core.util.toNotificationContent
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendStatus
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallSessionRepository
import com.synapse.social.studioasinc.shared.domain.repository.IncomingCallWatcher
import io.github.aakira.napier.Napier
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Decides, once for the whole process, whether this device is being rung.
 *
 * Three jobs, and only three. It narrows the backend's invitation stream to the one thing a screen can
 * act on; it keeps a single realtime subscription alive so a ring survives navigation, a recomposition
 * and the app going behind the home screen; and it posts or withdraws the ringing notification according
 * to whether the app can show the call itself. The last one is why this class watches the process
 * lifecycle: a notification on top of a ringing screen would sound twice for one call, and a ring the
 * user walked away from needs the notification it did not need a moment ago.
 *
 * What it deliberately does not do is answer a call. Every path out of here is a *signal* — a flow a
 * screen collects, a notification a user taps — and both lead to a ViewModel that still has to satisfy
 * the backend before anything is ringing or connected. A coordinator that could mark itself answered
 * would be the ghost call this design exists to prevent.
 */
@Singleton
class CallSessionCoordinator @Inject constructor(
    private val sessions: CallSessionRepository,
    private val notifications: CallNotificationHelper,
    @Named("ApplicationScope") private val appScope: CoroutineScope
) : IncomingCallWatcher {

    private val _incomingCall = MutableStateFlow<CallSession?>(null)
    override val incomingCall: Flow<CallSession?> = _incomingCall.asStateFlow()

    private var watchJob: Job? = null
    private var lifecycleObserver: DefaultLifecycleObserver? = null

    /** The call this coordinator has a visible notification for, so it can be withdrawn by id. */
    private var deliveredCallId: String? = null

    /**
     * Begins watching, and — if already watching — re-reads what is open.
     *
     * Both halves matter. The first makes this the cold-start path: the flow's opening snapshot is the
     * backend's own pending list, so a call placed before this process existed still surfaces. The second
     * is what a push wake needs: a hint from outside says only "look", and a watch that was already
     * running would otherwise do nothing at all.
     */
    override fun start() {
        val watching = synchronized(this) {
            if (watchJob?.isActive == true) {
                true
            } else {
                watchJob = appScope.launch { watch() }
                false
            }
        }
        if (watching) appScope.launch { reconcile() }
        appScope.launch(Dispatchers.Main) { observeForeground() }
    }

    /**
     * Stops watching and clears the ring.
     *
     * Called on sign-out. Leaving a ringing row in the flow would hand the next user's device a call that
     * belongs to the one that just left.
     */
    override fun stop() {
        val job = synchronized(this) { watchJob.also { watchJob = null } }
        job?.cancel()
        appScope.launch(Dispatchers.Main) {
            val observer = lifecycleObserver
            if (observer != null) {
                ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
                lifecycleObserver = null
            }
            publish(null)
        }
    }

    // ---- Watch -------------------------------------------------------------------

    private suspend fun watch() {
        // Expired rings become missed rows before anything reads them, so a call that died while this
        // device was asleep cannot arrive as something still worth answering.
        sessions.resolveStaleSessions()
            .onFailure { Napier.w("Stale call rows left unresolved: ${it.message}") }

        sessions.observeIncomingInvitations().collect { row -> publish(row) }
    }

    /**
     * Re-reads the open rows and settles the ring on what the backend currently says.
     *
     * [CallSessionRepository.observeOpenSessions] is the snapshot for this: it is re-read server-side, so
     * it can be advanced or rewound several transitions while this device was not looking. Used only as a
     * response to a wake hint, never as a substitute for the live stream.
     */
    private suspend fun reconcile() {
        val open = withTimeoutOrNull(RECONCILE_TIMEOUT_MS) {
            sessions.observeOpenSessions().firstOrNull()
        } ?: return
        publish(open.firstOrNull { it.isWaitingForMe() })
    }

    // ---- Delivery ----------------------------------------------------------------

    /**
     * Publishes [row] as the ring, and delivers it wherever the user can still see it.
     *
     * The flow is written on the collector's thread so it stays ordered against the stream; the
     * notification is decided on Main a moment later, and re-reads the flow rather than this row. That is
     * deliberate: two rows arriving quickly would otherwise post and dismiss in the wrong order, while a
     * delivery that always asks "what is ringing *now*" converges on the truth whichever copy runs last.
     */
    private fun publish(row: CallSession?) {
        val ring = row?.takeIf(CallSession::isWaitingForMe)
        _incomingCall.value = ring
        appScope.launch(Dispatchers.Main) { deliver(_incomingCall.value) }
    }

    private fun deliver(ring: CallSession?) {
        if (ring == null) {
            deliveredCallId?.let(notifications::dismissIncomingCall)
            deliveredCallId = null
            return
        }
        if (isAppVisible()) {
            // The screen is ringing, so the notification must not. Kept withdrawn rather than never
            // posted, because the user may be about to background the app while the call still waits.
            notifications.dismissIncomingCall(ring.id)
            deliveredCallId = null
        } else {
            notifications.showIncomingCall(ring.toNotificationContent())
            deliveredCallId = ring.id
        }
    }

    private fun observeForeground() {
        if (lifecycleObserver != null) return
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = deliver(_incomingCall.value)
            override fun onStop(owner: LifecycleOwner) = deliver(_incomingCall.value)
        }
        lifecycleObserver = observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }

    private fun isAppVisible(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private companion object {
        const val RECONCILE_TIMEOUT_MS = 5_000L
    }
}

/**
 * True while the backend still counts this row as a ring addressed to this device.
 *
 * Reads the server's status rather than [com.synapse.social.studioasinc.shared.domain.model.call.CallPhase]:
 * the phase is what a screen renders and can be ahead of, or behind, the row. Deciding that somebody is
 * waiting for an answer is a fact about the row.
 */
private fun CallSession.isWaitingForMe(): Boolean =
    direction == CallDirection.INCOMING && backendStatus == CallBackendStatus.RINGING
