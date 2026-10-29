package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import kotlinx.coroutines.flow.Flow

/**
 * The device-side answer to "is someone ringing me right now".
 *
 * [CallSessionRepository] is the backend's ledger: every row, every transition, every replay. That is
 * the wrong shape for a screen, which needs at most one thing to react to — an invitation the user has
 * not answered yet — and needs it only once, no matter how many realtime messages describe it. This
 * interface is that narrowed signal, and the implementation behind it is the only place allowed to know
 * about notification ids, deduplication windows and which rings this device has already surfaced.
 *
 * Keeping it separate from [CallRepository] is what lets the app react to a call it did not place —
 * including one that arrived while the process was not running — without pretending a screen owns it.
 */
interface IncomingCallWatcher {

    /**
     * The invitation waiting for an answer, or null when nothing is ringing.
     *
     * A session appears here only while the backend still calls it a ring, and disappears as soon as it
     * stops being one — answered, declined, cancelled or expired. Emitting null is not optional: a
     * consumer that never sees it keeps ringing.
     */
    val incomingCall: Flow<CallSession?>

    /** Starts watching. Calling it again while already watching does nothing. */
    fun start()

    /** Stops watching and clears whatever was ringing. Called on sign-out, so a ring never outlives it. */
    fun stop()
}
