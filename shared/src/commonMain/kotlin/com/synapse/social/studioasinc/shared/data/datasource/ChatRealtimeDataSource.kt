package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.util.UUIDUtils
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageReactionDto
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

/**
 * Data source responsible for managing real-time communication with Supabase.
 * It utilizes WebSockets to listen for Postgres CDC (Change Data Capture) events,
 * Presence for user state tracking, and Broadcast for ephemeral messaging.
 */
internal class ChatRealtimeDataSource(private val client: SupabaseClientLib) {
    // One persistent typing channel per chat � broadcast sender and receiver
    // share the same channel object so events are never dropped.
    private val activeTypingChannels = java.util.concurrent.ConcurrentHashMap<String, RealtimeChannel>()

    private suspend fun getOrCreateTypingChannel(chatId: String): RealtimeChannel {
        val channelId = "broadcast-typing-${chatId.trim().lowercase()}"
        val channel = activeTypingChannels.getOrPut(channelId) { client.realtime.channel(channelId) }
        if (channel.status.value != RealtimeChannel.Status.SUBSCRIBED) {
            try { client.realtime.connect() } catch (e: Exception) { if (e is CancellationException) throw e }
            var subscribed = false; var attempts = 0
            while (!subscribed && attempts < 3) {
                try { channel.subscribe(blockUntilSubscribed = true); subscribed = true }
                catch (e: Exception) {
                    if (e is CancellationException) throw e
                    attempts++
                    if (attempts < 3) delay(300L * attempts)
                }
            }
        }
        return channel
    }

    suspend fun releaseTypingChannel(chatId: String) {
        val channelId = "broadcast-typing-${chatId.trim().lowercase()}"
        val channel = activeTypingChannels.remove(channelId) ?: return
        withContext(AppDispatchers.IO) {
            try { channel.unsubscribe(); client.realtime.removeChannel(channel) }
            catch (e: Exception) { Napier.w("Error releasing typing channel $channelId: ${e.message}", tag = "Realtime") }
        }
    }


    /**
     * Retrieves the current authenticated user's unique identifier.
     */
    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    /**
     * Sends an ephemeral typing status update to other participants in a specific chat.
     * Uses Supabase Broadcast to ensure low-latency delivery without persisting to the database.
     *
     * The broadcast channel is subscribed before sending. If subscription fails after retries,
     * the broadcast is aborted rather than silently failing on an unsubscribed channel.
     */
    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean) =
        withContext(AppDispatchers.IO) {
            try {
                val currentUserId = getCurrentUserId() ?: return@withContext
                val normalizedChatId = chatId.trim().lowercase()
                val channelId = "broadcast-typing-$normalizedChatId"
                val channel = client.realtime.channel(channelId)

                // Ensure the realtime socket is connected before subscribing.
                try {
                    client.realtime.connect()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Napier.w("Realtime connect failed before broadcast: ${e.message}", e, tag = "Realtime")
                    return@withContext
                }

                // Subscribe to the channel before broadcasting. If subscription fails,
                // abort rather than silently-failing broadcasts on an unsubscribed channel.
                if (channel.status.value != RealtimeChannel.Status.SUBSCRIBED) {
                    var subscribed = false
                    var attempts = 0
                    while (!subscribed && attempts < 3) {
                        try {
                            channel.subscribe(blockUntilSubscribed = true)
                            subscribed = true
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            attempts++
                            Napier.w("Attempt $attempts failed to subscribe to typing channel: ${e.message}", e, tag = "Realtime")
                            if (attempts < 3) delay(500L * attempts)
                        }
                    }
                    if (!subscribed) {
                        Napier.e("Aborting typing broadcast: could not subscribe to channel $channelId", tag = "Realtime")
                        return@withContext
                    }
                }

                val payload = buildJsonObject {
                    put("user_id", currentUserId)
                    put("is_typing", isTyping)
                }
                try {
                    channel.broadcast(event = "typing", message = payload)
                    Napier.d("Broadcast typing status for chat $normalizedChatId: isTyping=$isTyping", tag = "Realtime")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Napier.w("Broadcast typing failed: ${e.message}", e, tag = "Realtime")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Napier.e("Error broadcasting typing status", e)
            }
        }

    /**
     * Subscribes to new messages in a specific chat using Postgres CDC.
     * @param chatId The unique identifier for the conversation.
     * @return A [Flow] emitting [MessageDto] objects as they are created in the database.
     *
     * Implementation uses a synchronized subscription pattern: the collector is registered
     * and signals readiness via [CompletableDeferred] before the WebSocket subscription
     * is finalized. This prevents a race condition where messages sent immediately after
     * entering a chat are missed.
     */
    fun subscribeToMessages(chatId: String): Flow<MessageDto> = callbackFlow {
        val normalizedChatId = chatId.trim().lowercase()
        val channelId = "msgs_flow_${normalizedChatId}"
        Napier.d("Creating realtime channel for messages: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {
            Napier.e("Failed to connect to Realtime WebSocket", e, tag = "Realtime")
        }

        val channel = client.realtime.channel(channelId)

        // Register the postgres change listener BEFORE subscribing.
        // postgresChangeFlow only registers the filter — it does not start the WebSocket
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, normalizedChatId)
        }

        val subscriptionReady = CompletableDeferred<Unit>()
        val collector = launch(AppDispatchers.IO) {
            try {
                changeFlow
                    .onStart { subscriptionReady.complete(Unit) }
                    .collect { action ->
                        try {
                            when (action) {
                                is PostgresAction.Insert -> {
                                    val record = action.decodeRecord<MessageDto>()
                                    Napier.d("Realtime message INSERT received in channel $channelId: ${record.id} for chat $normalizedChatId", tag = "Realtime")
                                    send(record)
                                }
                                is PostgresAction.Update -> {
                                    val record = action.decodeRecord<MessageDto>()
                                    Napier.d("Realtime message UPDATE received in channel $channelId: ${record.id} for chat $normalizedChatId, status=${record.deliveryStatus}", tag = "Realtime")
                                    send(record)
                                }
                                else -> {}
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Napier.e("Error decoding realtime message action", e, tag = "Realtime")
                        }
                    }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Collector terminated unexpectedly for channel $channelId", e, tag = "Realtime")
                    close(e)
                }
            }
        }

        val statusMonitor = launch(AppDispatchers.IO) {
            try {
                var wasSubscribed = false
                launch {
                    channel.status.collect { status ->
                        if (status == RealtimeChannel.Status.SUBSCRIBED) {
                            wasSubscribed = true
                        } else if (wasSubscribed && status != RealtimeChannel.Status.SUBSCRIBED) {
                            Napier.w("Channel $channelId lost subscription (status=$status), closing flow to trigger re-subscription", tag = "Realtime")
                            close(IllegalStateException("Realtime channel unsubscribed"))
                        }
                    }
                }
                launch {
                    var wasConnected = false
                    client.realtime.status.collect { rStatus ->
                        if (rStatus == io.github.jan.supabase.realtime.Realtime.Status.CONNECTED) {
                            if (wasConnected && wasSubscribed) {
                                Napier.w("Realtime socket reconnected, refreshing subscription for channel $channelId", tag = "Realtime")
                                close(IllegalStateException("Realtime socket reconnected"))
                            }
                            wasConnected = true
                        } else if (rStatus == io.github.jan.supabase.realtime.Realtime.Status.DISCONNECTED && wasConnected) {
                            Napier.w("Realtime socket disconnected for channel $channelId", tag = "Realtime")
                            close(IllegalStateException("Realtime socket disconnected"))
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.w("Status monitor terminated for channel $channelId: ${e.message}", tag = "Realtime")
                }
            }
        }

        launch(AppDispatchers.IO) {
            try {
                subscriptionReady.await()
                yield()
                var subscribed = false
                var attempts = 0
                while (!subscribed && attempts < 3 && isActive) {
                    try {
                        channel.subscribe(blockUntilSubscribed = true)
                        subscribed = true
                        Napier.d("Successfully subscribed to messages channel: $channelId", tag = "Realtime")
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        attempts++
                        Napier.w("Attempt $attempts failed to subscribe to messages channel: $channelId: ${e.message}", tag = "Realtime")
                        if (attempts < 3) delay(1000L * attempts)
                    }
                }
                if (!subscribed && isActive) {
                    try {
                        channel.subscribe(blockUntilSubscribed = false)
                        Napier.d("Subscribed to messages channel non-blocking: $channelId", tag = "Realtime")
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            Napier.e("Failed to subscribe to messages channel after retries: $channelId", e, tag = "Realtime")
                            close(e)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to messages channel: $channelId", e, tag = "Realtime")
                    close(e)
                }
            }
        }
        awaitClose {
            Napier.d("Closing realtime channel for messages: $channelId", tag = "Realtime")
            collector.cancel()
            statusMonitor.cancel()
            CoroutineScope(AppDispatchers.IO).launch(NonCancellable) {
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Napier.w("Error during channel cleanup: ${e.message}", tag = "Realtime")
                }
            }
        }
    }.buffer(Channel.BUFFERED)

    /**
     * Monitors for any new messages across the entire 'messages' table to update the user's inbox.
     * Note: Server-side RLS (Row Level Security) ensures the user only receives messages they are authorized to see.
     */
    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<MessageDto> = callbackFlow {
        // Fixed topic: Supabase rejects channel topics over 100 bytes, and joining every chat id
        // would exceed that with 3+ conversations. Row visibility is enforced by RLS, not the topic.
        val channelId = "inbox_flow"
        Napier.d("Creating realtime channel for inbox: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {}

        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }

        val subscriptionReady = CompletableDeferred<Unit>()
        val collector = launch(AppDispatchers.IO) {
            try {
                flow
                    .onStart { subscriptionReady.complete(Unit) }
                    .collect { action ->
                        try {
                            when (action) {
                                is PostgresAction.Insert -> {
                                    val message = action.decodeRecord<MessageDto>()
                                    Napier.d("Realtime inbox INSERT received: ${message.id}", tag = "Realtime")
                                    trySend(message)
                                }
                                is PostgresAction.Update -> {
                                    val message = action.decodeRecord<MessageDto>()
                                    Napier.d("Realtime inbox UPDATE received: ${message.id}", tag = "Realtime")
                                    trySend(message)
                                }
                                else -> {}
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Napier.e("Error decoding real-time message in inbox", e, tag = "Realtime")
                        }
                    }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Inbox flow collector terminated unexpectedly", e, tag = "Realtime")
                    close(e)
                }
            }
        }

        launch(AppDispatchers.IO) {
            try {
                subscriptionReady.await()
                yield()
                var subscribed = false
                var attempts = 0
                while (!subscribed && attempts < 3 && isActive) {
                    try {
                        channel.subscribe(blockUntilSubscribed = true)
                        subscribed = true
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        attempts++
                        if (attempts < 3) delay(1000L * attempts)
                    }
                }
                if (!subscribed && isActive) {
                    try {
                        channel.subscribe(blockUntilSubscribed = false)
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            Napier.e("Failed to subscribe to inbox channel after retries", e, tag = "Realtime")
                            close(e)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to inbox channel", e, tag = "Realtime")
                    close(e)
                }
            }
        }

        awaitClose {
            collector.cancel()
            CoroutineScope(AppDispatchers.IO).launch(NonCancellable) {
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {}
            }
        }
    }

    /**
     * Subscribes to typing indicators from all participants in a chat using Supabase Broadcast.
     *
     * Broadcast-only implementation: presence was previously used alongside broadcast on the
     * same channel, which can conflict in supabase-kt realtime-kt. Broadcast messages are
     * delivered to every subscriber of the channel, so presence is unnecessary.
     *
     * The collector is registered and signals readiness via [CompletableDeferred] before the
     * WebSocket subscription is finalized, preventing a race where typing events sent
     * immediately after entering a chat are missed.
     */
    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> = callbackFlow {
        val normalizedChatId = chatId.trim().lowercase()
        val channelId = "broadcast-typing-$normalizedChatId"
        Napier.d("Subscribing to typing broadcast channel: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }

        val channel = client.realtime.channel(channelId)

        val subscriptionReady = CompletableDeferred<Unit>()

        val collector = launch(AppDispatchers.IO) {
            try {
                channel.broadcastFlow<kotlinx.serialization.json.JsonObject>("typing")
                    .onStart { subscriptionReady.complete(Unit) }
                    .collect { json ->
                        val userId = json["user_id"]?.jsonPrimitive?.contentOrNull
                        val isTyping = json["is_typing"]?.jsonPrimitive?.booleanOrNull

                        if (userId != null && isTyping != null) {
                            trySend(mapOf("user_id" to userId, "is_typing" to isTyping))
                        }
                    }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.w("Typing broadcast flow ended: ${e.message}", e, tag = "Realtime")
                }
            }
        }

        launch(AppDispatchers.IO) {
            try {
                subscriptionReady.await()
                yield()
                var subscribed = false
                var attempts = 0
                while (!subscribed && attempts < 3 && isActive) {
                    try {
                        channel.subscribe(blockUntilSubscribed = true)
                        subscribed = true
                        Napier.d("Successfully subscribed to typing channel: $channelId", tag = "Realtime")
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        attempts++
                        Napier.w("Attempt $attempts failed to subscribe to typing channel: ${e.message}", e, tag = "Realtime")
                        if (attempts < 3) delay(500L * attempts)
                    }
                }
                if (!subscribed && isActive) {
                    try {
                        channel.subscribe(blockUntilSubscribed = false)
                        Napier.d("Subscribed to typing channel non-blocking: $channelId", tag = "Realtime")
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            Napier.e("Failed to subscribe to typing channel after retries: $channelId", e, tag = "Realtime")
                            close(e)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to typing channel: $channelId", e, tag = "Realtime")
                    close(e)
                }
            }
        }

        awaitClose {
            Napier.d("Closing typing broadcast channel: $channelId", tag = "Realtime")
            collector.cancel()
            CoroutineScope(AppDispatchers.IO).launch(NonCancellable) {
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Napier.w("Error during typing channel cleanup: ${e.message}", tag = "Realtime")
                }
            }
        }
    }

    /**
     * Listens for updates to existing messages (e.g., status changes to 'read' or 'delivered').
     */
    fun subscribeToReadReceipts(chatId: String): Flow<MessageDto> = callbackFlow {
        val normalizedChatId = chatId.trim().lowercase()
        val channelId = "read_flow_${normalizedChatId}"
        Napier.d("Creating realtime channel for read receipts: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {}

        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, normalizedChatId)
        }

        val subscriptionReady = CompletableDeferred<Unit>()
        val collector = launch(AppDispatchers.IO) {
            flow
                .onStart { subscriptionReady.complete(Unit) }
                .collect { action ->
                    try {
                        val message = action.decodeRecord<MessageDto>()
                        send(message)
                    } catch (e: Exception) {}
                }
        }

        launch(AppDispatchers.IO) {
            try {
                subscriptionReady.await()
                yield()
                channel.subscribe(blockUntilSubscribed = true)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to read receipts", e, tag = "Realtime")
                    close(e)
                }
            }
        }

        awaitClose {
            collector.cancel()
            CoroutineScope(AppDispatchers.IO).launch(NonCancellable) {
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {}
            }
        }
    }

    /**
     * Tracks additions and removals of emoji reactions for a specific chat.
     */
    fun subscribeToMessageReactions(chatId: String): Flow<MessageReactionDto> = callbackFlow {
        val normalizedChatId = chatId.trim().lowercase()
        val channelId = "react_flow_${normalizedChatId}"
        Napier.d("Creating realtime channel for reactions: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {}

        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "message_reactions"
            filter("chat_id", FilterOperator.EQ, normalizedChatId)
        }

        val subscriptionReady = CompletableDeferred<Unit>()
        val collector = launch(AppDispatchers.IO) {
            flow
                .onStart { subscriptionReady.complete(Unit) }
                .collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> try { send(action.decodeRecord<MessageReactionDto>()) } catch(e: Exception) {}
                        is PostgresAction.Update -> try { send(action.decodeRecord<MessageReactionDto>()) } catch(e: Exception) {}
                        is PostgresAction.Delete -> try {
                            val oldRecord = action.decodeOldRecord<MessageReactionDto>()
                            send(oldRecord.copy(isDeleteEvent = true))
                        } catch(e: Exception) {}
                        else -> {}
                    }
                }
        }

        launch(AppDispatchers.IO) {
            try {
                subscriptionReady.await()
                yield()
                channel.subscribe(blockUntilSubscribed = true)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to reactions channel", e, tag = "Realtime")
                    close(e)
                }
            }
        }

        awaitClose {
            collector.cancel()
            CoroutineScope(AppDispatchers.IO).launch(NonCancellable) {
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {}
            }
        }
    }
}
