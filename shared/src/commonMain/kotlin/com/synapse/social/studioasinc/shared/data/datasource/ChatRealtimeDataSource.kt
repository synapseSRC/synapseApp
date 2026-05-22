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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
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

internal class ChatRealtimeDataSource(private val client: SupabaseClientLib) {

    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

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

    fun subscribeToMessages(chatId: String): Flow<MessageDto> = callbackFlow {
        val channelId = "msgs_flow_${chatId}_${UUIDUtils.randomUUID()}"
        Napier.d("Creating realtime channel for messages: $channelId", tag = "Realtime")

        val channel = client.realtime.channel(channelId)

        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        val collector = launch(AppDispatchers.IO) {
            changeFlow.collect { action ->
                try {
                    val record = action.decodeRecord<MessageDto>()
                    Napier.d("Realtime message received in channel $channelId: ${record.id}", tag = "Realtime")
                    send(record)
                } catch (e: Exception) {
                    Napier.e("Error decoding realtime message", e, tag = "Realtime")
                }
            }
        }

        launch(AppDispatchers.IO) {
            // Give a small window for the postgresChangeFlow collector to register its filters
            // with the client before we send the subscription request to the server.
            delay(200)
            yield()
            try {
                client.realtime.connect()
                val status = channel.status.value
                if (status != RealtimeChannel.Status.SUBSCRIBED) {
                    Napier.d("Subscribing to messages channel: $channelId", tag = "Realtime")
                    channel.subscribe()
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

    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<MessageDto> = callbackFlow {
        val channelId = "inbox_flow_${UUIDUtils.randomUUID()}_${Clock.System.now().toEpochMilliseconds()}"
        Napier.d("Creating realtime channel for inbox: $channelId", tag = "Realtime")

        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }

        val collector = launch(AppDispatchers.IO) {
            flow.collect { action ->
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
            delay(200)
            yield()
            try {
                client.realtime.connect()
                val status = channel.status.value
                if (status != RealtimeChannel.Status.SUBSCRIBED) {
                    channel.subscribe()
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
            launch {
                try {
                    yield()
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {}
            }
        }
    }

    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> = callbackFlow {
        val channelId = "typing_flow_${chatId}_${UUIDUtils.randomUUID()}_${Clock.System.now().toEpochMilliseconds()}"
        Napier.d("Creating realtime channel for typing status: $channelId", tag = "Realtime")

        val channel = client.realtime.channel(channelId)
        val presenceFlow = channel.presenceChangeFlow()

        val collector = launch(AppDispatchers.IO) {
            presenceFlow.collect { presenceChange ->
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
            delay(200)
            yield()
            try {
                client.realtime.connect()
                val status = channel.status.value
                if (status != RealtimeChannel.Status.SUBSCRIBED) {
                    channel.subscribe()
                }
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

    fun subscribeToReadReceipts(chatId: String): Flow<MessageDto> = callbackFlow {
        val channelId = "read_flow_${chatId}_${UUIDUtils.randomUUID()}_${Clock.System.now().toEpochMilliseconds()}"
        Napier.d("Creating realtime channel for read receipts: $channelId", tag = "Realtime")

        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        val collector = launch(AppDispatchers.IO) {
            flow.collect { action ->
                try {
                    val message = action.decodeRecord<MessageDto>()
                    send(message)
                } catch (e: Exception) {}
            }
        }

        launch(AppDispatchers.IO) {
            delay(200)
            yield()
            try {
                client.realtime.connect()
                val status = channel.status.value
                if (status != RealtimeChannel.Status.SUBSCRIBED) {
                    channel.subscribe()
                }
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

    fun subscribeToMessageReactions(chatId: String): Flow<MessageReactionDto> = callbackFlow {
        val channelId = "react_flow_${chatId}_${UUIDUtils.randomUUID()}_${Clock.System.now().toEpochMilliseconds()}"
        Napier.d("Creating realtime channel for reactions: $channelId", tag = "Realtime")

        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "message_reactions"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        val collector = launch(AppDispatchers.IO) {
            flow.collect { action ->
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
            delay(200)
            yield()
            try {
                client.realtime.connect()
                val status = channel.status.value
                if (status != RealtimeChannel.Status.SUBSCRIBED) {
                    channel.subscribe()
                }
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
