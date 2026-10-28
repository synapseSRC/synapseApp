package com.synapse.social.studioasinc.data.call

import com.synapse.social.studioasinc.data.call.dto.CallSessionDto
import com.synapse.social.studioasinc.data.call.dto.CallSignalingDto
import com.synapse.social.studioasinc.data.call.mapper.toWireValue
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignalKind
import com.synapse.social.studioasinc.shared.util.UUIDUtils
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime.Status
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Realtime for calls: the two change streams and the one write that signaling needs.
 *
 * The subscription lifecycle is the one the chat realtime source already proves works on
 * this backend — register the change listener, wait for its collector to be ready, then
 * subscribe, and remove the channel on a non-cancellable context — with two additions that
 * only matter for calls:
 *
 *  - the flow closes the moment the socket drops or the channel unsubscribes, so the
 *    orchestrator can re-subscribe with its own cursor instead of sitting on a silently
 *    dead subscription mid-negotiation;
 *  - signaling replays what is already stored before draining live events, buffering the
 *    events that arrive during the replay, because SDP and ICE order is not a preference.
 *
 * Envelope writes carry a per-sender sequence number that this class owns: monotonicity and
 * idempotence of a retry are transport properties, and pushing them up into the call
 * orchestrator would mean every caller had to get them right.
 */
class CallRealtimeDataSource(private val client: SupabaseClientLib) {

    private val seqMutex = Mutex()
    private val nextSeq = mutableMapOf<String, Int>()

    // ---- Signaling ---------------------------------------------------------------

    /**
     * Inserts one envelope.
     *
     * The body is built explicitly rather than by serializing a DTO: `id` is a
     * `GENERATED ALWAYS AS IDENTITY` column that rejects any supplied value, and
     * `sender_uid` is stamped by a database trigger from `auth.uid()`, so the exact key set
     * has to be controlled instead of left to a serializer's defaults.
     */
    suspend fun send(callId: String, kind: CallSignalKind, payload: JsonObject): Result<Unit> =
        withContext(AppDispatchers.IO) {
            try {
                val seq = claimSeq(callId)
                try {
                    insert(callId, seq, kind, payload)
                } catch (e: PostgrestRestException) {
                    if (!isDuplicateSeq(e)) throw e
                    // Our counter fell behind a row that is already stored — the response to
                    // an earlier attempt we believed was lost. Re-sync from the database and
                    // try once, so the retry cannot silently drop a negotiation step.
                    Napier.w("Signaling seq collision on $callId (seq=$seq), re-syncing", tag = "Realtime")
                    insert(callId, syncSeq(callId), kind, payload)
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("Failed to send $kind envelope for call $callId", e, tag = "Realtime")
                Result.failure(e)
            }
        }

    /**
     * Envelope stream for one session, ascending by row id, at-least-once.
     *
     * Includes envelopes this device sent: at this layer nobody knows whether the local user
     * is the caller or the callee, so [SupabaseCallSessionRepository] is what filters them out
     * before the domain sees a stream. [replayAfterId] is the caller's cursor: pass the highest
     * id already handled when re-subscribing after a dropped connection.
     */
    fun observeSignaling(callId: String, replayAfterId: Long = 0L): Flow<CallSignalingDto> = channelFlow {
        val normalizedCallId = callId.trim().lowercase()
        // Live events park here while the replay SELECT runs, then merge into the stream.
        val parked = Channel<CallSignalingDto>(Channel.UNLIMITED)

        val liveJob = launch(AppDispatchers.IO) {
            try {
                liveChanges(
                    topicPrefix = "sig_flow_$normalizedCallId",
                    table = SIGNALING_TABLE,
                    filterColumn = "session_id",
                    filterValue = normalizedCallId
                ) { action -> action.decodeOrNull<CallSignalingDto>() }.collect { parked.send(it) }
            } finally {
                parked.close()
            }
        }

        try {
            val seen = HashSet<Long>()
            replay(normalizedCallId, replayAfterId).forEach { row ->
                seen.add(row.id)
                send(row)
            }
            // Drained by hand rather than with `for (row in parked)`: a channel closed with a
            // cause must rethrow it, which is how a dropped socket reaches the orchestrator as
            // a failure instead of as a quiet end of stream.
            while (true) {
                val received = parked.receiveCatching()
                if (received.isClosed) {
                    received.exceptionOrNull()?.let { throw it }
                    break
                }
                val row = received.getOrNull() ?: continue
                if (seen.add(row.id)) send(row)
            }
        } finally {
            liveJob.cancel()
        }
    }.buffer(Channel.BUFFERED)

    /** Rows this session stored after [afterId], oldest first. */
    suspend fun replay(callId: String, afterId: Long): List<CallSignalingDto> = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from(SIGNALING_TABLE)
                .select(columns = Columns.list("id", "session_id", "sender_uid", "seq", "kind", "payload", "created_at")) {
                    filter {
                        eq("session_id", callId)
                        if (afterId > 0L) gt("id", afterId)
                    }
                    order("id", Order.ASCENDING)
                    limit(REPLAY_PAGE_SIZE)
                }.decodeList<CallSignalingDto>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failed replay is not fatal: live delivery continues, and the next reconnect
            // gets another chance. Failing the stream here would end a working call.
            Napier.e("Signaling replay failed for $callId", e, tag = "Realtime")
            emptyList()
        }
    }

    /** Forgets the per-session counter once a call is over; a new session starts at 0. */
    suspend fun resetSequence(callId: String) = seqMutex.withLock {
        nextSeq.remove(callId.trim().lowercase())
        Unit
    }

    // ---- Sessions ----------------------------------------------------------------

    /**
     * Row changes for one session, INSERT and UPDATE.
     *
     * The initial [CallSessionDataSource.fetch] is not part of this flow: the orchestrator
     * already holds the authoritative row returned by the RPC that created or accepted the
     * session, and re-reading it here would add a window where two sources disagree about
     * the current phase.
     */
    fun observeSession(callId: String): Flow<CallSessionDto> =
        liveChanges(
            topicPrefix = "session_flow_${callId.trim().lowercase()}",
            table = SESSION_TABLE,
            filterColumn = "id",
            filterValue = callId.trim().lowercase()
        ) { action -> action.decodeOrNull<CallSessionDto>() }

    /**
     * Every session row that names [userId] as callee.
     *
     * Filtered on the callee column rather than "all messages": the table is global, and a
     * subscription that received every ring in the project would be both a battery problem
     * and an information leak that RLS alone would have to hold back. Row visibility is
     * enforced by RLS; this filter only narrows what we ask to be sent.
     */
    fun observeInvitations(userId: String): Flow<CallSessionDto> =
        liveChanges(
            topicPrefix = "invitation_flow",
            table = SESSION_TABLE,
            filterColumn = "callee_id",
            filterValue = userId
        ) { action -> action.decodeOrNull<CallSessionDto>() }

    /**
     * Every session row this user may see, unfiltered.
     *
     * No column filter is not a wide open firehose: RLS restricts `call_sessions` to rows
     * where `auth.uid()` is a party, so what arrives here is already only this account's
     * sessions. Used to invalidate the open-session snapshot, where knowing *that*
     * something changed matters more than the changed row.
     */
    fun observeAnySession(): Flow<CallSessionDto> =
        liveChanges(
            topicPrefix = "sessions_flow",
            table = SESSION_TABLE,
            filterColumn = null,
            filterValue = null
        ) { action -> action.decodeOrNull<CallSessionDto>() }

    // ---- Internals ----------------------------------------------------------------

    /**
     * One Postgres-CDC subscription with the full lifecycle.
     *
     * [topicPrefix] gets a random suffix because `realtime.channel(id)` hands back the
     * still-registered channel for a reused topic: without a unique name, a re-subscription
     * after a drop would share the old channel and its asynchronous `removeChannel` would
     * silently unsubscribe the new one. Postgres routing ignores the topic, so the suffix is
     * free.
     */
    private fun <T : Any> liveChanges(
        topicPrefix: String,
        table: String,
        filterColumn: String?,
        filterValue: String?,
        decode: (PostgresAction) -> T?
    ): Flow<T> = callbackFlow {
        val topic = topicPrefix + "_" + UUIDUtils.randomUUID().take(8)
        Napier.d("Realtime channel $topic opened for $table", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {
            Napier.e("Realtime socket unavailable for $topic", e, tag = "Realtime")
        }

        val channel = client.realtime.channel(topic)
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = table
            if (filterColumn != null && filterValue != null) {
                filter(filterColumn, FilterOperator.EQ, filterValue)
            }
        }

        val subscriptionReady = CompletableDeferred<Unit>()
        val collector = launch(AppDispatchers.IO) {
            try {
                changeFlow
                    .onStart { subscriptionReady.complete(Unit) }
                    .collect { action ->
                        val decoded = decode(action)
                        if (decoded != null) send(decoded)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("Collector terminated for $topic", e, tag = "Realtime")
                close(e)
            }
        }

        val statusMonitor = launch(AppDispatchers.IO) {
            // Read by both watchers: the socket-reconnect branch only matters once this channel
            // has actually subscribed at least once.
            var wasSubscribed = false
            launch {
                channel.status.collect { status ->
                    if (status == RealtimeChannel.Status.SUBSCRIBED) {
                        wasSubscribed = true
                    } else if (wasSubscribed && status != RealtimeChannel.Status.SUBSCRIBED) {
                        Napier.w("$topic lost its subscription (status=$status)", tag = "Realtime")
                        close(IllegalStateException("Realtime channel unsubscribed"))
                    }
                }
            }
            launch {
                var wasConnected = false
                client.realtime.status.collect { socketStatus ->
                    val connected = socketStatus == Status.CONNECTED
                    if (connected && wasConnected && wasSubscribed) {
                        // Reconnects do not always re-deliver missed rows: end the flow so the
                        // caller can rebuild state from a replay instead of guessing.
                        Napier.w("$topic: socket reconnected, refreshing subscription", tag = "Realtime")
                        close(IllegalStateException("Realtime socket reconnected"))
                    } else if (!connected && wasConnected) {
                        close(IllegalStateException("Realtime socket disconnected"))
                    }
                    wasConnected = connected
                }
            }
        }

        launch(AppDispatchers.IO) {
            try {
                subscriptionReady.await()
                withTimeout(SUBSCRIBE_TIMEOUT_MS) {
                    channel.subscribe(blockUntilSubscribed = true)
                }
            } catch (e: TimeoutCancellationException) {
                close(IllegalStateException("Realtime subscription timed out", e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("Failed to subscribe $topic", e, tag = "Realtime")
                close(e)
            }
        }

        awaitClose {
            Napier.d("Closing realtime channel $topic", tag = "Realtime")
            collector.cancel()
            statusMonitor.cancel()
            CoroutineScope(AppDispatchers.IO).launch(NonCancellable) {
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Napier.w("Realtime cleanup failed for $topic: ${e.message}", tag = "Realtime")
                }
            }
        }
    }.buffer(Channel.BUFFERED)

    private suspend fun insert(callId: String, seq: Int, kind: CallSignalKind, payload: JsonObject) {
        client.postgrest.from(SIGNALING_TABLE).insert(buildJsonObject {
            put("session_id", callId)
            put("seq", seq)
            put("kind", kind.toWireValue())
            put("payload", payload)
        })
    }

    private suspend fun claimSeq(callId: String): Int = seqMutex.withLock {
        val key = callId.trim().lowercase()
        val value = nextSeq[key] ?: firstSeq(key)
        nextSeq[key] = value + 1
        value
    }

    /**
     * Reads the highest seq this user already published for the session, so a process
     * restart mid-call continues the sequence instead of colliding with stored rows.
     */
    private suspend fun firstSeq(callId: String): Int = try {
        val me = client.auth.currentUserOrNull()?.id ?: return DEFAULT_SEQ
        val rows = client.postgrest.from(SIGNALING_TABLE)
            .select(columns = Columns.list("seq")) {
                filter {
                    eq("session_id", callId)
                    eq("sender_uid", me)
                }
                order("seq", Order.DESCENDING)
                limit(1)
            }.decodeList<CallSignalingDto>()
        (rows.firstOrNull()?.seq ?: -1) + 1
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Could not read the signaling sequence, starting at 0: ${e.message}", tag = "Realtime")
        DEFAULT_SEQ
    }

    private suspend fun syncSeq(callId: String): Int = firstSeq(callId).also { fresh ->
        nextSeq[callId.trim().lowercase()] = fresh + 1
    }

    /**
     * Recognises the unique violation that `call_signaling_sender_seq_unique` raises for a
     * replayed sequence number. Matched on the constraint name rather than the SQLSTATE
     * alone, because a 23505 from another constraint means something else entirely.
     */
    private fun isDuplicateSeq(e: PostgrestRestException): Boolean {
        val code = e.code?.uppercase()
        if (code != "23505" && code != "23P01") return false
        val haystack = e.message + e.details.toString() + e.hint.orEmpty()
        return SEQ_CONSTRAINT in haystack
    }

    private inline fun <reified T : Any> PostgresAction.decodeOrNull(): T? = when (this) {
        is PostgresAction.Insert -> decodeSafely { decodeRecord() }
        is PostgresAction.Update -> decodeSafely { decodeRecord() }
        else -> null
    }

    private inline fun <R> decodeSafely(block: () -> R): R? =
        try {
            block()
        } catch (e: Exception) {
            // A record we cannot read must never take a live call down with it.
            Napier.e("Undecodable realtime row: ${e.message}", tag = "Realtime")
            null
        }

    private companion object {
        const val SESSION_TABLE = "call_sessions"
        const val SIGNALING_TABLE = "call_signaling"
        const val SEQ_CONSTRAINT = "call_signaling_sender_seq_unique"
        const val DEFAULT_SEQ = 0
        const val SUBSCRIBE_TIMEOUT_MS = 10_000L
        const val REPLAY_PAGE_SIZE = 200L
    }
}
