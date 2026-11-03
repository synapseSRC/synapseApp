package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.util.UUIDUtils
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Change signals for one Echo Room.
 *
 * This is deliberately not the source of truth: it emits once per change and the repository
 * re-reads the room through `get_echo_room`. Both tables are watched because the room row carries
 * the lifecycle (`status`) and the participant rows carry the roster; heartbeat updates arrive
 * here too and are folded by the repository's debounce.
 */
class EchoRoomRealtimeDataSource(private val client: SupabaseClientLib) {

    fun subscribeToRoomChanges(roomId: String): Flow<Unit> = callbackFlow {
        val normalized = roomId.trim().lowercase()
        // A unique topic keeps a restarted screen from sharing a channel that is still being
        // unsubscribed; Postgres change routing does not depend on it.
        val topic = "echo_room_${normalized}_${UUIDUtils.randomUUID().take(8)}"
        Napier.d("Subscribing to echo room changes: $topic", tag = TAG)

        try {
            client.realtime.connect()
        } catch (e: Exception) {
            Napier.w("Echo realtime connect failed: ${e.message}", tag = TAG)
        }

        val channel = client.realtime.channel(topic)

        // Filters must be registered before the channel is joined.
        val roomChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "echo_rooms"
            filter("id", FilterOperator.EQ, normalized)
        }
        val participantChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "echo_room_participants"
            filter("room_id", FilterOperator.EQ, normalized)
        }

        val collectors = listOf(roomChanges, participantChanges).map { changes ->
            launch(AppDispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                try {
                    changes.collect { trySend(Unit) }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Napier.e("Echo room change stream failed", e, tag = TAG)
                    close(e)
                }
            }
        }

        val statusMonitor = launch(AppDispatchers.IO) {
            launch {
                var wasSubscribed = channel.status.value == RealtimeChannel.Status.SUBSCRIBED
                channel.status.collect { status ->
                    if (status == RealtimeChannel.Status.SUBSCRIBED) {
                        wasSubscribed = true
                    } else if (wasSubscribed) {
                        Napier.w("Echo room channel lost subscription (status=$status)", tag = TAG)
                        close(IllegalStateException("Echo room channel lost its subscription"))
                    }
                }
            }
            launch {
                // Both statuses are StateFlows: collect() replays the current value first. Seeding
                // "was connected" from .value and then acting on that replay would treat a healthy
                // socket as a reconnect and close the channel milliseconds after subscribing, so
                // only genuine transitions are handled.
                var previous = client.realtime.status.value
                client.realtime.status.collect { status ->
                    if (status == previous) return@collect
                    val wasConnected = previous == Realtime.Status.CONNECTED
                    previous = status
                    if (status != Realtime.Status.CONNECTED && wasConnected) {
                        Napier.w("Echo realtime socket disconnected", tag = TAG)
                        close(IllegalStateException("Echo realtime socket disconnected"))
                    }
                }
            }
        }

        val subscription = launch(AppDispatchers.IO) {
            try {
                withTimeout(10_000) { channel.subscribe(blockUntilSubscribed = true) }
                Napier.d("Subscribed to echo room changes: $topic", tag = TAG)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Napier.e("Echo room subscription failed: $topic", e, tag = TAG)
                close(IllegalStateException("Echo room subscription failed", e))
            }
        }

        awaitClose {
            Napier.d("Closing echo room channel: $topic", tag = TAG)
            collectors.forEach { it.cancel() }
            statusMonitor.cancel()
            subscription.cancel()
            CoroutineScope(AppDispatchers.IO).launch(NonCancellable) {
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Napier.w("Echo realtime cleanup failed: ${e.message}", tag = TAG)
                }
            }
        }
    }.buffer(Channel.CONFLATED)

    private companion object {
        const val TAG = "EchoRealtime"
    }
}
