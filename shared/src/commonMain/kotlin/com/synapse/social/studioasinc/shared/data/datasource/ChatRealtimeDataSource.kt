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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
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

    /**
     * Retrieves the current authenticated user's unique identifier.
     */
    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    /**
     * Sends an ephemeral typing status update to other participants in a specific chat.
     * Uses Supabase Broadcast to ensure low-latency delivery without persisting to the database.
     */
    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean) =
        withContext(AppDispatchers.IO) {
            try {
                val currentUserId = getCurrentUserId() ?: return@withContext
                val channelId = "broadcast-typing-$chatId"
                val channel = client.realtime.channel(channelId)

                if (channel.status.value != RealtimeChannel.Status.SUBSCRIBED) {
                    try {
                        client.realtime.connect()
                        channel.subscribe(blockUntilSubscribed = true)
                    } catch (e: Exception) {
                        Napier.e("Error subscribing to typing broadcast channel", e)
                    }
                }
                channel.track(buildJsonObject {
                    put("user_id", currentUserId)
                    put("is_typing", isTyping)
                })
            } catch (e: Exception) {
                Napier.e("Error broadcasting typing status", e)
            }
        }

    /**
     * Subscribes to new messages in a specific chat using Postgres CDC.
     * @param chatId The unique identifier for the conversation.
     * @return A [Flow] emitting [MessageDto] objects as they are created in the database.
     */
    fun subscribeToMessages(chatId: String): Flow<MessageDto> = callbackFlow {
        val channelId = "msgs_flow_${chatId}_${UUIDUtils.randomUUID()}"
        Napier.d("Creating realtime channel for messages: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {
            Napier.e("Failed to connect to Realtime WebSocket", e, tag = "Realtime")
        }

        val channel = client.realtime.channel(channelId)

        // Register the postgres change listener BEFORE subscribing.
        // postgresChangeFlow only registers the filter — it does not start the WebSocket
        // handshake. The actual subscription happens in channel.subscribe() below.
        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        // Subscribe first so the channel is fully joined on the server before we start
        // collecting. Any INSERT that arrives after subscribe() completes will be delivered.
        try {
            channel.subscribe(blockUntilSubscribed = true)
            Napier.d("Successfully subscribed to messages channel: $channelId", tag = "Realtime")
        } catch (e: CancellationException) {
            client.realtime.removeChannel(channel)
            throw e
        } catch (e: Exception) {
            Napier.e("Failed to subscribe to messages channel: $channelId", e, tag = "Realtime")
            client.realtime.removeChannel(channel)
            close(e)
            return@callbackFlow
        }

        val collector = launch(AppDispatchers.IO) {
            changeFlow.collect { action ->
                try {
                    val record = action.decodeRecord<MessageDto>()
                    Napier.d("Realtime message received in channel $channelId: ${record.id} for chat $chatId", tag = "Realtime")
                    send(record)
                } catch (e: Exception) {
                    Napier.e("Error decoding realtime message", e, tag = "Realtime")
                }
            }
        }

        awaitClose {
            Napier.d("Closing realtime channel for messages: $channelId", tag = "Realtime")
            collector.cancel()
            launch {
                try {
                    yield()
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Napier.w("Error during channel cleanup: ${e.message}", tag = "Realtime")
                }
            }
        }
    }

    /**
     * Monitors for any new messages across the entire 'messages' table to update the user's inbox.
     * Note: Server-side RLS (Row Level Security) ensures the user only receives messages they are authorized to see.
     */
    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<MessageDto> = callbackFlow {
        val channelId = "inbox_flow_${UUIDUtils.randomUUID()}_${Clock.System.now().toEpochMilliseconds()}"
        Napier.d("Creating realtime channel for inbox: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {}

        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }

        val subscriptionReady = CompletableDeferred<Unit>()
        val collector = launch(AppDispatchers.IO) {
            flow
                .onStart { subscriptionReady.complete(Unit) }
                .collect { action ->
                    try {
                        val message = action.decodeRecord<MessageDto>()
                        Napier.d("Realtime inbox update received: ${message.id}", tag = "Realtime")
                        send(message)
                    } catch (e: Exception) {
                        Napier.e("Error decoding real-time message in inbox", e, tag = "Realtime")
                    }
                }
        }

        launch(AppDispatchers.IO) {
            try {
                subscriptionReady.await()
                channel.subscribe(blockUntilSubscribed = true)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to inbox channel", e, tag = "Realtime")
                    close(e)
                }
            }
        }

        awaitClose {
            collector.cancel()
            launch {
                try {
                    yield()
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {}
            }
        }
    }

    /**
     * Subscribes to typing indicators from all participants in a chat using Presence.
     * Presence provides a shared state that tracks who is currently 'online' or 'typing' in a channel.
     */
    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> = callbackFlow {
        val channelId = "typing_flow_${chatId}_${UUIDUtils.randomUUID()}_${Clock.System.now().toEpochMilliseconds()}"
        Napier.d("Creating realtime channel for typing status: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {}

        val channel = client.realtime.channel(channelId)
        val presenceFlow = channel.presenceChangeFlow()

        val subscriptionReady = CompletableDeferred<Unit>()
        val collector = launch(AppDispatchers.IO) {
            presenceFlow
                .onStart { subscriptionReady.complete(Unit) }
                .collect { presenceChange ->
                    presenceChange.joins.values.forEach { presence ->
                        try {
                            val state = presence.state
                            val userId = state["user_id"]?.jsonPrimitive?.contentOrNull
                            val isTyping = state["is_typing"]?.jsonPrimitive?.booleanOrNull

                            if (userId != null && isTyping != null) {
                                send(mapOf("user_id" to userId, "is_typing" to isTyping))
                            }
                        } catch (e: Exception) {
                            Napier.e("Error decoding presence state", e, tag = "Realtime")
                        }
                    }

                    presenceChange.leaves.values.forEach { presence ->
                        try {
                            val state = presence.state
                            val userId = state["user_id"]?.jsonPrimitive?.contentOrNull

                            if (userId != null) {
                                send(mapOf("user_id" to userId, "is_typing" to false))
                            }
                        } catch (e: Exception) {
                            Napier.e("Error decoding presence leave state", e, tag = "Realtime")
                        }
                    }
                }
        }

        launch(AppDispatchers.IO) {
            try {
                subscriptionReady.await()
                channel.subscribe(blockUntilSubscribed = true)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to chat presence", e, tag = "Realtime")
                    close(e)
                }
            }
        }

        awaitClose {
            collector.cancel()
            launch {
                try {
                    yield()
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {}
            }
        }
    }

    /**
     * Listens for updates to existing messages (e.g., status changes to 'read' or 'delivered').
     */
    fun subscribeToReadReceipts(chatId: String): Flow<MessageDto> = callbackFlow {
        val channelId = "read_flow_${chatId}_${UUIDUtils.randomUUID()}_${Clock.System.now().toEpochMilliseconds()}"
        Napier.d("Creating realtime channel for read receipts: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {}

        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
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
            launch {
                try {
                    yield()
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
        val channelId = "react_flow_${chatId}_${UUIDUtils.randomUUID()}_${Clock.System.now().toEpochMilliseconds()}"
        Napier.d("Creating realtime channel for reactions: $channelId", tag = "Realtime")

        try {
            client.realtime.connect()
        } catch (e: Exception) {}

        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "message_reactions"
            filter("chat_id", FilterOperator.EQ, chatId)
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
            launch {
                try {
                    yield()
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {}
            }
        }
    }
}
