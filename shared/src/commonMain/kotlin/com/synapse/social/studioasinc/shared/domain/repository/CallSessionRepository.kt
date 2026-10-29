package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignal
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignalRecord
import kotlinx.coroutines.flow.Flow

/**
 * The backend's view of a call: the state machine, and the signaling mailboxes that
 * hang off it.
 *
 * Split from `CallRepository` on purpose. `CallRepository` is what the screen uses —
 * "start a call", "mute", "flip the camera" — and it owns the local media. This
 * interface is what the *server* uses: it records facts the server can verify and
 * returns the authoritative row for every transition. The orchestrating repository
 * sits between the two and is the only place where local media state and backend
 * state are allowed to meet.
 *
 * Nothing here is Supabase-specific. Realtime, PostgREST and the RPC names belong to
 * the data layer; the domain sees transitions and envelopes.
 */
interface CallSessionRepository {

    /** The signed-in user id, or null when there is no session to act on behalf of. */
    val currentUserId: String?

    // ---- Lifecycle transitions -------------------------------------------------
    // Every one of these returns the row the backend settled on, not what the client
    // asked for. The caller is expected to treat the returned status as the truth, so
    // a successful response can never be read as "we are connected".

    suspend fun create(chatId: String, calleeId: String, mediaType: CallMediaType): Result<CallSession>

    suspend fun accept(callId: String): Result<CallSession>

    suspend fun decline(callId: String): Result<CallSession>

    /** Caller hangs up before anyone answered. */
    suspend fun cancel(callId: String): Result<CallSession>

    /** Only legal once the callee has accepted — enforced server-side. */
    suspend fun markActive(callId: String): Result<CallSession>

    suspend fun markReconnecting(callId: String): Result<CallSession>

    suspend fun switchMediaType(callId: String, mediaType: CallMediaType): Result<CallSession>

    /**
     * Ends a call. [reason] is the client's *claim*; the backend overrides it when its
     * own view of the phase disagrees (a ring nobody answered is never "completed").
     */
    suspend fun end(callId: String, reason: CallEndReason? = null): Result<CallSession>

    // ---- Reads -----------------------------------------------------------------

    suspend fun fetch(callId: String): Result<CallSession?>

    /**
     * Live row for [callId], including remote changes.
     *
     * Emits on change only: the row a transition returned is already in the caller's hands,
     * and re-reading it here would create a window where two sources disagree about the
     * current phase. The stream stays open until the collector cancels — the terminal `ended`
     * row is delivered like any other, because the caller has to see the call end before it
     * can tear the call down. If a consumer needs the current row on subscribe, it calls
     * [fetch] first.
     */
    fun observe(callId: String): Flow<CallSession>

    /**
     * Sessions addressed to the signed-in user as callee.
     *
     * Starts with the rings that are still open server-side — a call placed while this
     * process was not running must still surface — then every later change to those rows,
     * including the change that ends them. A ring the caller cancelled arrives as an ended
     * session so the UI can stop ringing rather than outlive the call.
     */
    fun observeIncomingInvitations(): Flow<CallSession>

    /**
     * Every session this user is still a party to, as a snapshot list.
     *
     * Re-read from the backend whenever any of this user's session rows changes, so it is the
     * stream to heal from after a process death: the truth may have moved several transitions
     * while the client was gone, and the snapshot says where it landed.
     */
    fun observeOpenSessions(): Flow<List<CallSession>>

    /**
     * Ended 1:1 sessions this user took part in, newest first.
     *
     * Not scoped to a chat: the Calls tab is a history of calls, and a session only has
     * a chat because the backend needs a conversation to attribute it to. Participant
     * filtering is the real boundary and is enforced by row-level security, not by this
     * argument list.
     */
    suspend fun getCallHistory(limit: Int = 30): Result<List<CallSession>>

    /** Resolves rings whose server deadline has passed. Safe to call on launch. */
    suspend fun resolveStaleSessions(): Result<Int>

    /**
     * Writes back a presence beat for a live call.
     *
     * The server treats silence as evidence the client died: a connected row whose beat
     * stops is closed after a grace period, which is what turns a crashed call into an
     * endable, visible session instead of a row that blocks the pair forever.
     */
    suspend fun heartbeat(callId: String): Result<Unit>

    // ---- Signaling -------------------------------------------------------------

    suspend fun publish(callId: String, signal: CallSignal): Result<Unit>

    /**
     * Envelope stream for one session, ordered by [CallSignalRecord.id], **peers only**.
     *
     * The transport echoes your own writes back, and an implementation that forwarded them
     * would have to be told not to answer its own offer; this interface simply does not
     * deliver them. Delivery is at-least-once. [replayAfterId] is the consumer's cursor: pass
     * the highest id already handled when re-subscribing after a dropped connection so a
     * candidate that arrived while offline is replayed instead of lost. The stream
     * re-establishes itself when the transport drops and keeps advancing its own cursor, so a
     * single subscription is safe to hold for the life of a call.
     */
    fun observeSignals(callId: String, replayAfterId: Long = 0): Flow<CallSignalRecord>
}
