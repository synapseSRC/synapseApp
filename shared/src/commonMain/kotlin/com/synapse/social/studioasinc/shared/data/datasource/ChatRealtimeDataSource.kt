package com.synapse.social.studioasinc.shared.data.datasource

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

internal class ChatRealtimeDataSource(private val client: SupabaseClientLib) {

    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean) =
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = getCurrentUserId() ?: return@withContext
                val channel = client.realtime.channel("chat-\$chatId")

                if (channel.status.value != io.github.jan.supabase.realtime.RealtimeChannel.Status.SUBSCRIBED) {
                    try {
                        Napier.d("Subscribing to typing channel: chat-\$chatId")
                        channel.subscribe(blockUntilSubscribed = true)
                    } catch (e: Exception) {
                        Napier.e("Error subscribing to typing channel", e)
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
        val channelId = "chat-messages-\$chatId-\${UUIDUtils.randomUUID()}"
        Napier.d("Creating channel: \$channelId")
        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        val collector = launch {
            kotlinx.coroutines.yield()
            flow.map { it.decodeRecord<MessageDto>() }.collect { message ->
                trySend(message)
            }
        }

        launch(Dispatchers.IO) {
            kotlinx.coroutines.yield()
            try {
                Napier.d("Subscribing to channel: \$channelId")
                channel.subscribe()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to chat", e)
                    close(e)
                }
            }
        }

        awaitClose {
            Napier.d("Closing channel: \$channelId")
            collector.cancel()
            launch {
            kotlinx.coroutines.yield()
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Napier.w("Failed to unsubscribe/remove channel: \$channelId", e)
                }
            }
        }
    }

    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<MessageDto> = callbackFlow {
        val channelId = "inbox-updates-\${UUIDUtils.randomUUID()}"
        Napier.d("Creating channel: \$channelId")
        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }

        val collector = launch {
            kotlinx.coroutines.yield()
            flow.collect { action ->
                try {
                    val message = action.decodeRecord<MessageDto>()
                    trySend(message)
                } catch (e: Exception) {
                    Napier.e("Error decoding real-time message in inbox", e)
                }
            }
        }

        launch {
            kotlinx.coroutines.yield()
            try {
                Napier.d("Subscribing to channel: \$channelId")
                channel.subscribe()
            } catch (e: Exception) {
                Napier.e("Failed to subscribe to inbox channel", e)
                close(e)
            }
        }

        awaitClose {
            Napier.d("Closing channel: \$channelId")
            collector.cancel()
            launch {
            kotlinx.coroutines.yield()
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Napier.w("Failed to unsubscribe/remove channel: \$channelId", e)
                }
            }
        }
    }

    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> = callbackFlow {
        val channelId = "chat-\$chatId-\${UUIDUtils.randomUUID()}"
        Napier.d("Creating channel: \$channelId")
        val channel = client.realtime.channel(channelId)

        val collector = launch {
            kotlinx.coroutines.yield()
            channel.presenceChangeFlow().collect { presenceChange ->
                presenceChange.joins.values.forEach { presence ->
                    try {
                        val state = presence.state
                        val userId = state["user_id"]?.jsonPrimitive?.contentOrNull
                        val isTyping = state["is_typing"]?.jsonPrimitive?.booleanOrNull

                        if (userId != null && isTyping != null) {
                            trySend(mapOf("user_id" to userId, "is_typing" to isTyping))
                        }
                    } catch (e: Exception) {
                        Napier.e("Error decoding presence state", e)
                    }
                }

                presenceChange.leaves.values.forEach { presence ->
                    try {
                        val state = presence.state
                        val userId = state["user_id"]?.jsonPrimitive?.contentOrNull

                        if (userId != null) {
                            trySend(mapOf("user_id" to userId, "is_typing" to false))
                        }
                    } catch (e: Exception) {
                        Napier.e("Error decoding presence leave state", e)
                    }
                }
            }
        }

        launch(Dispatchers.IO) {
            kotlinx.coroutines.yield()
            try {
                Napier.d("Subscribing to channel: \$channelId")
                channel.subscribe()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to chat presence", e)
                    close(e)
                }
            }
        }

        awaitClose {
            Napier.d("Closing channel: \$channelId")
            collector.cancel()
            launch {
            kotlinx.coroutines.yield()
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Napier.w("Failed to unsubscribe/remove channel: \$channelId", e)
                }
            }
        }
    }

    fun subscribeToReadReceipts(chatId: String): Flow<MessageDto> = callbackFlow {
        val channelId = "read-receipts-\$chatId-\${UUIDUtils.randomUUID()}"
        Napier.d("Creating channel: \$channelId")
        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        val collector = launch {
            kotlinx.coroutines.yield()
            flow.map { it.decodeRecord<MessageDto>() }.collect { message ->
                trySend(message)
            }
        }

        launch(Dispatchers.IO) {
            kotlinx.coroutines.yield()
            try {
                Napier.d("Subscribing to channel: \$channelId")
                channel.subscribe()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to read receipts", e)
                    close(e)
                }
            }
        }

        awaitClose {
            Napier.d("Closing channel: \$channelId")
            collector.cancel()
            launch {
            kotlinx.coroutines.yield()
                try {
                    channel.unsubscribe()
                    client.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Napier.w("Failed to unsubscribe/remove channel: \$channelId", e)
                }
            }
        }
    }

    fun subscribeToMessageReactions(): Flow<MessageReactionDto> = callbackFlow {
        val channelId = "message-reactions-\${UUIDUtils.randomUUID()}"
        val channel = client.realtime.channel(channelId)
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "message_reactions"
        }

        val collector = launch {
            kotlinx.coroutines.yield()
            flow.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> try { trySend(action.decodeRecord<MessageReactionDto>()) } catch(e: Exception) {}
                    is PostgresAction.Update -> try { trySend(action.decodeRecord<MessageReactionDto>()) } catch(e: Exception) {}
                    is PostgresAction.Delete -> try { trySend(action.decodeOldRecord<MessageReactionDto>()) } catch(e: Exception) {}
                    else -> {}
                }
            }
        }

        launch {
            kotlinx.coroutines.yield()
            try {
                channel.subscribe()
            } catch (e: Exception) {
                close(e)
            }
        }

        awaitClose {
            collector.cancel()
            launch {
            kotlinx.coroutines.yield()
                channel.unsubscribe()
                client.realtime.removeChannel(channel)
            }
        }
    }
}
