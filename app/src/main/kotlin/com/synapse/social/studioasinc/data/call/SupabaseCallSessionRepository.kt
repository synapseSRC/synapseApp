package com.synapse.social.studioasinc.data.call

import com.synapse.social.studioasinc.data.call.dto.CallSessionDto
import com.synapse.social.studioasinc.data.call.dto.CallSignalPayloadDto
import com.synapse.social.studioasinc.data.call.mapper.classifyCallFailure
import com.synapse.social.studioasinc.data.call.mapper.toDomain
import com.synapse.social.studioasinc.data.call.mapper.toParticipant
import com.synapse.social.studioasinc.data.call.mapper.toPayloadDto
import com.synapse.social.studioasinc.data.call.mapper.toRecord
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendException
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendStatus
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallErrorKind
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallParticipant
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignal
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignalRecord
import com.synapse.social.studioasinc.shared.domain.repository.CallSessionRepository
import io.github.aakira.napier.Napier
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * The Supabase-backed view of a call: `call_sessions` for the state machine,
 * `call_signaling` for the negotiation envelopes.
 *
 * Three responsibilities that the data sources deliberately refuse to hold:
 *
 *  1. **Interpretation.** A PostgREST rejection is a string; a call needs to know whether
 *     it was "you are busy", "that ring expired", or "the tables are not there yet".
 *     [classifyCallFailure] turns the backend's `SYNAPSE_CALL_*` protocol into a
 *     [CallErrorKind] exactly once, here.
 *  2. **Naming.** `call_sessions` stores user ids, so a session cannot be labelled without
 *     a profile read. [peerCache] keeps those reads to one query per peer for the life of
 *     the process instead of re-fetching on every realtime tick.
 *  3. **Continuity.** Realtime streams end when the socket drops. Each stream here restarts
 *     itself, and the signaling stream restarts *from its own cursor*, so an ICE candidate
 *     that landed while we were offline is replayed rather than missed — a missed candidate
 *     is a call that silently fails to connect on one side only.
 *
 * The backend is the authority for every fact this class returns. Nothing here infers a
 * status from a timer or from local media: a call is `active` because the row says so.
 */
class SupabaseCallSessionRepository(
    private val sessions: CallSessionDataSource,
    private val realtime: CallRealtimeDataSource
) : CallSessionRepository {

    private val json = Json {
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val peerMutex = Mutex()
    private val peerCache = LinkedHashMap<String, CallParticipant>()

    override val currentUserId: String? get() = sessions.currentUserId

    // ---- Lifecycle transitions ---------------------------------------------------

    override suspend fun create(
        chatId: String,
        calleeId: String,
        mediaType: CallMediaType
    ): Result<CallSession> {
        if (chatId.isBlank() || calleeId.isBlank()) {
            // PostgREST would answer this with a 22P02 "invalid input syntax for type uuid",
            // which reads like a backend fault. It is a caller fault: a call is only
            // authorisable inside a chat both users already share.
            return Result.failure(
                CallBackendException(
                    CallErrorKind.STATE_INVALID,
                    "create_call_session needs a chat id and a callee id " +
                        "(chat blank=${chatId.isBlank()}, callee blank=${calleeId.isBlank()})"
                )
            )
        }
        return attempt("create_call_session") {
            sessions.create(chatId, calleeId, mediaType, RINGING_SECONDS).asSession("create_call_session")
        }
    }

    override suspend fun accept(callId: String): Result<CallSession> =
        attempt("accept_call") { sessions.accept(callId).asSession("accept_call") }

    override suspend fun decline(callId: String): Result<CallSession> =
        attempt("decline_call") {
            sessions.decline(callId).asSession("decline_call").also { realtime.resetSequence(callId) }
        }

    override suspend fun cancel(callId: String): Result<CallSession> =
        attempt("cancel_call") {
            sessions.cancel(callId).asSession("cancel_call").also { realtime.resetSequence(callId) }
        }

    override suspend fun markActive(callId: String): Result<CallSession> =
        attempt("mark_active") { sessions.markActive(callId).asSession("mark_active") }

    override suspend fun markReconnecting(callId: String): Result<CallSession> =
        attempt("mark_reconnecting") { sessions.markReconnecting(callId).asSession("mark_reconnecting") }

    override suspend fun switchMediaType(
        callId: String,
        mediaType: CallMediaType
    ): Result<CallSession> =
        attempt("switch_call_media") {
            sessions.switchMediaType(callId, mediaType).asSession("switch_call_media")
        }

    override suspend fun end(callId: String, reason: CallEndReason?): Result<CallSession> =
        attempt("end_call") {
            sessions.end(callId, reason).asSession("end_call").also { realtime.resetSequence(callId) }
        }

    // ---- Reads -------------------------------------------------------------------

    override suspend fun fetch(callId: String): Result<CallSession?> =
        attempt("fetch_call_session") { sessions.fetch(callId)?.asSession("fetch_call_session") }

    /**
     * Live row for one session. No initial snapshot is emitted (the row that a transition
     * returned is already in the caller's hands) and the stream does not complete on `ended`
     * — the terminal row is delivered and the collector decides when it is done.
     */
    override fun observe(callId: String): Flow<CallSession> = flow {
        val backoff = StreamBackoff()
        while (true) {
            coroutineContext.ensureActive()
            try {
                realtime.observeSession(callId).collect { row ->
                    val session = row.toSessionOrNull()
                    // Null means this client cannot read the row as a party to it, which RLS
                    // is supposed to make impossible; skipping is the only honest option.
                    if (session != null) {
                        backoff.delivered()
                        emit(session)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("Session stream for $callId dropped: ${e.message}", tag = TAG)
            }
            delay(backoff.nextDelayMillis())
        }
    }

    /**
     * Rings addressed to this user: whatever is still open when we start listening, then
     * every later change — including the change that ends a ring, which is how a cancelled
     * call stops ringing on a device that never showed an answer UI.
     */
    override fun observeIncomingInvitations(): Flow<CallSession> = flow {
        val backoff = StreamBackoff()
        var pendingRead = false
        while (true) {
            coroutineContext.ensureActive()
            val me = currentUserId
            if (me == null) {
                // Signed out, or mid-token-refresh. Keep polling for an id: the rings that
                // arrive after it returns are delivered by the stream below, and a ring that
                // opened in the meantime is caught by the pending read on the next pass.
                pendingRead = false
                delay(backoff.nextDelayMillis())
                continue
            }
            if (!pendingRead) {
                pendingRead = true
                val pending = attempt("get_pending_invitations") { sessions.getPendingInvitations(me) }
                    .getOrNull().orEmpty()
                for (row in pending) {
                    val session = row.toSessionOrNull() ?: continue
                    if (session.backendStatus != CallBackendStatus.ENDED) emit(session)
                }
            }
            try {
                realtime.observeInvitations(me).collect { row ->
                    val session = row.toSessionOrNull() ?: return@collect
                    backoff.delivered()
                    emit(session)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("Invitation stream dropped: ${e.message}", tag = TAG)
            }
            delay(backoff.nextDelayMillis())
        }
    }

    /**
     * Snapshot of the sessions this user is still in, re-read whenever any of their session
     * rows changes.
     *
     * The invalidation signal is conflated: however many rows change at once, at most one
     * re-read is in flight, and the read is the source of truth rather than the deltas that
     * triggered it. That is the shape a process-death recovery needs — the truth may have
     * moved several steps while this client was not running.
     *
     * A failed read does *not* emit an empty list. This stream is what keeps a live call's UI
     * alive across a navigation, and answering a network blip with "no sessions" would drop it
     * out of a call that is still running for everyone else. The last known snapshot stays.
     */
    override fun observeOpenSessions(): Flow<List<CallSession>> = channelFlow {
        val invalidate = Channel<Unit>(Channel.CONFLATED)
        val backoff = StreamBackoff()
        launch(AppDispatchers.IO) {
            while (true) {
                coroutineContext.ensureActive()
                try {
                    realtime.observeAnySession().collect {
                        backoff.delivered()
                        invalidate.trySend(Unit)
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Napier.w("Open-session stream dropped: ${e.message}", tag = TAG)
                }
                delay(backoff.nextDelayMillis())
            }
        }
        // The first read is not triggered by a change: a session opened before this
        // collector started is still open, and healing after a process death depends on it
        // arriving without anyone having to dial again.
        invalidate.trySend(Unit)
        while (true) {
            val received = invalidate.receiveCatching()
            if (received.isClosed) {
                received.exceptionOrNull()?.let { throw it }
                break
            }
            val me = currentUserId
            val snapshot = if (me == null) emptyList() else openSessionsSnapshot(me)
            if (snapshot != null) send(snapshot) else delay(backoff.nextDelayMillis())
        }
    }

    override suspend fun getCallHistory(limit: Int): Result<List<CallSession>> {
        // The signed-in id is read once and handed to the mapper, so a token refresh that
        // lands between the query and the mapping cannot flip every direction to "unknown".
        val me = currentUserId ?: return Result.failure(
            CallBackendException(CallErrorKind.NOT_AUTHENTICATED, "No signed-in user for call history")
        )
        return attempt("get_call_history") {
            sessions.getHistory(me, limit.coerceIn(1, MAX_HISTORY)).toSessions(me)
        }
    }

    override suspend fun resolveStaleSessions(): Result<Int> =
        attempt("expire_stale_call_sessions") { sessions.resolveStaleSessions() }

    override suspend fun heartbeat(callId: String): Result<Unit> =
        attempt("call_heartbeat") { sessions.heartbeat(callId) }.map { }

    // ---- Signaling ---------------------------------------------------------------

    override suspend fun publish(callId: String, signal: CallSignal): Result<Unit> {
        val sent = realtime.send(callId, signal.kind, encode(signal))
        val failure = sent.exceptionOrNull() ?: return Result.success(Unit)
        if (failure is CancellationException) throw failure
        val interpreted = failure.toBackendException("publish_${signal.kind.name.lowercase()}")
        Napier.w("Signaling publish failed ($callId, ${signal.kind}): ${interpreted.message}", tag = TAG)
        return Result.failure(interpreted)
    }

    /**
     * Peer envelopes only, ordered, and self-healing.
     *
     * Two things the orchestrator would otherwise have to get right by itself: it must not
     * answer its own offer (the transport echoes every write back), and after a dropped
     * socket it must not resume from "now" — a candidate that arrived during the gap is
     * already stored and only a replay will deliver it.
     */
    override fun observeSignals(callId: String, replayAfterId: Long): Flow<CallSignalRecord> = flow {
        val backoff = StreamBackoff()
        var cursor = replayAfterId
        while (true) {
            coroutineContext.ensureActive()
            val me = currentUserId
            try {
                realtime.observeSignaling(callId, cursor).collect { row ->
                    val record = row.toRecord()
                    if (record == null) {
                        Napier.w("Dropped an undecodable signaling row (id=${row.id}, kind=${row.kind})", tag = TAG)
                        return@collect
                    }
                    if (record.id > cursor) cursor = record.id
                    if (me != null && record.senderUserId == me) return@collect
                    backoff.delivered()
                    emit(record)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("Signaling stream for $callId dropped (${e.message}); replaying from id=$cursor", tag = TAG)
            }
            delay(backoff.nextDelayMillis())
        }
    }

    // ---- Internals: mapping ------------------------------------------------------

    /**
     * Row -> domain, with the peer's profile resolved.
     *
     * Throws rather than returning a half-read session when the row cannot be understood: a
     * transition the backend accepted but this client cannot map is a bug worth surfacing,
     * not a call worth continuing on a guessed label.
     */
    private suspend fun CallSessionDto.asSession(action: String): CallSession =
        toSessionOrNull() ?: throw CallBackendException(
            if (currentUserId == null) CallErrorKind.NOT_AUTHENTICATED else CallErrorKind.FORBIDDEN,
            "$action returned a row this client is not a party to (call=${id ?: "unknown"}, user=${currentUserId ?: "none"})"
        )

    private suspend fun CallSessionDto.toSessionOrNull(): CallSession? {
        val me = currentUserId ?: return null
        return toDomain(me, peersFor(listOf(callerId, calleeId)))
    }

    private suspend fun List<CallSessionDto>.toSessions(userId: String?): List<CallSession> {
        val me = userId ?: return emptyList()
        val peers = peersFor(flatMap { listOf(it.callerId, it.calleeId) })
        return mapNotNull { it.toDomain(me, peers) }
    }

    /** Null when the read failed, which the caller must not turn into an empty list. */
    private suspend fun openSessionsSnapshot(userId: String): List<CallSession>? =
        attempt("get_open_sessions") { sessions.getOpenSessions(userId).toSessions(userId) }.getOrNull()

    /**
     * Profiles for [ids], cached for the life of the process.
     *
     * A failed profile read is *not* a failed call: the ids stay uncached so the next read
     * tries again, and [toDomain] falls back to labelling the peer by id. Inventing "Unknown
     * user" here would be the data layer writing copy, and would hide the fact that a rename
     * is still pending.
     */
    private suspend fun peersFor(ids: List<String>): Map<String, CallParticipant> {
        val wanted = ids.filter { it.isNotBlank() }
        val missing = peerMutex.withLock { wanted.filterNot { peerCache.containsKey(it) } }
        if (missing.isNotEmpty()) {
            try {
                val fetched = sessions.getPeers(missing).map { it.toParticipant() }
                peerMutex.withLock {
                    if (peerCache.size >= MAX_CACHED_PEERS) peerCache.clear()
                    fetched.forEach { peerCache[it.userId] = it }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("Peer profiles unavailable (${missing.size} id(s)): ${e.message}", tag = TAG)
            }
        }
        return peerMutex.withLock {
            wanted.mapNotNull { peerCache[it] }.associateBy { it.userId }
        }
    }

    private fun encode(signal: CallSignal): JsonObject =
        json.encodeToJsonElement(CallSignalPayloadDto.serializer(), signal.toPayloadDto()).jsonObject

    // ---- Internals: failure interpretation ----------------------------------------

    private suspend fun <T> attempt(action: String, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val interpreted = if (e is CallBackendException) e else e.toBackendException(action)
        Napier.w("Call action '$action' failed [${interpreted.kind}]: ${interpreted.message}", tag = TAG)
        Result.failure(interpreted)
    }

    /**
     * Maps a backend or transport failure onto a [CallBackendException].
     *
     * Ordering matters. A `SYNAPSE_CALL_*` token is our own protocol and always wins; the
     * "unapplied migration" shape is checked before the generic fall-through because it is
     * the single most likely failure of a freshly cloned build, and naming the SQL file to
     * apply is worth more than an accurate-but-useless `UNKNOWN`.
     */
    private fun Throwable.toBackendException(action: String): CallBackendException {
        if (this is CallBackendException) return this
        val sqlState = (this as? PostgrestRestException)?.code
        val status = (this as? RestException)?.statusCode?.takeIf { it > 0 }
        val raw = message

        if (isUnappliedSchema(sqlState, raw)) {
            return CallBackendException(
                CallErrorKind.BACKEND_UNREACHABLE,
                "The calling schema is not available ('$action'). Apply the " +
                    "calling backend schema to this Supabase project, then retry. " +
                    "Backend said: ${raw ?: sqlState ?: this}"
            )
        }
        if (this is HttpRequestException || this is IOException) {
            return CallBackendException(
                CallErrorKind.BACKEND_UNREACHABLE,
                "Could not reach the call service ('$action'): ${raw ?: this}"
            )
        }
        val kind = classifyCallFailure(raw, sqlState, status)
        return CallBackendException(
            kind,
            "$action rejected by the backend [${sqlState ?: status ?: kind.name}]: ${raw ?: this}"
        )
    }

    /**
     * Recognises "the tables or functions do not exist yet".
     *
     * PostgREST reports an unknown relation as PGRST205 and an unknown RPC as PGRST202; a raw
     * Postgres connection answers 42P01/42883 instead. All four mean the same thing here, and
     * none of them mean "your call was refused".
     */
    private fun isUnappliedSchema(sqlState: String?, raw: String?): Boolean {
        if (sqlState in UNAPPLIED_SCHEMA_CODES) return true
        val message = raw?.lowercase() ?: return false
        return ("does not exist" in message || "schema cache" in message) &&
            ("relation" in message || "function" in message || "table" in message)
    }

    private companion object {
        const val TAG = "Call"

        /** Kept in step with `ringing_expires_at`'s default in the sessions migration. */
        const val RINGING_SECONDS = 45

        const val MAX_HISTORY = 50
        const val MAX_CACHED_PEERS = 200

        val UNAPPLIED_SCHEMA_CODES = setOf("PGRST202", "PGRST205", "42P01", "42883")
    }
}

/**
 * Retry pacing for a realtime stream.
 *
 * A per-call stream wants to be back on the socket in about a second; an invitation listener
 * lives for the whole app session, and re-subscribing every 1.5 s while the device has no
 * signal is a battery problem, not a resilience feature. So the delay grows while a stream is
 * failing and resets the moment anything actually arrives.
 */
private class StreamBackoff {
    private var failures = 0

    fun delivered() {
        failures = 0
    }

    fun nextDelayMillis(): Long {
        val step = failures.coerceAtMost(MAX_STEP)
        failures++
        return minOf(BASE_DELAY_MS shl step, MAX_DELAY_MS)
    }

    private companion object {
        const val BASE_DELAY_MS = 1_500L
        const val MAX_STEP = 4
        const val MAX_DELAY_MS = 30_000L
    }
}
