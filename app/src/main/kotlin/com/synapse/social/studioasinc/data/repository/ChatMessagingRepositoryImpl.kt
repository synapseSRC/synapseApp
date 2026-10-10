package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatMessage
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatParticipantDto
import com.synapse.social.studioasinc.feature.inbox.inbox.models.Conversation
import com.synapse.social.studioasinc.feature.inbox.inbox.models.NewMessageDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.realtime
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class ChatMessagingRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) {
    companion object {
        private const val TAG = "ChatMessagingRepositoryImpl"
    }

    /**
     * Get the current authenticated user's ID.
     */
    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    /**
     * Fetch all conversations for the current user by querying chat_participants.
     */
    suspend fun getConversations(): Result<List<Conversation>> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            // 1. Get all chat IDs the user participates in
            val myParticipations = client.from("chat_participants")
                .select(columns = Columns.list("chat_id", "user_id", "is_archived")) {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<ChatParticipantDto>()

            val chatIds = myParticipations
                .filter { !it.isArchived }
                .map { it.chatId }

            if (chatIds.isEmpty()) return@withContext Result.success(emptyList())

            // 2. Batch fetch all other participants for these chats
            val otherParticipantsList = client.from("chat_participants")
                .select(columns = Columns.list("chat_id", "user_id")) {
                    filter {
                        isIn("chat_id", chatIds)
                        neq("user_id", currentUserId)
                    }
                }.decodeList<ChatParticipantDto>()

            val otherParticipantsByChat = otherParticipantsList.groupBy { it.chatId }
            val otherUserIds = otherParticipantsList.map { it.userId }.distinct()

            // 3. Batch fetch all other user profiles
            val otherUsers = if (otherUserIds.isNotEmpty()) {
                client.from("users")
                    .select {
                        filter { isIn("uid", otherUserIds) }
                    }.decodeList<User>().associateBy { it.uid }
            } else {
                emptyMap()
            }

            // 4. Concurrently fetch the last message for each chat
            // In postgrest without custom views, we fetch recent messages to find the last one per chat.
            // Using async allows us to make these roundtrips concurrently rather than sequentially.
            val lastMessagesDeferred = coroutineScope {
                chatIds.associateWith { chatId ->
                    async {
                        try {
                            client.from("messages")
                                .select {
                                    filter {
                                        eq("chat_id", chatId)
                                        eq("is_deleted", false)
                                    }
                                    order("created_at", Order.DESCENDING)
                                    limit(1)
                                }.decodeList<ChatMessage>().firstOrNull()
                        } catch (e: Exception) {
                            Napier.e("Error loading last message for $chatId", e, tag = TAG)
                            null
                        }
                    }
                }
            }

            val lastMessageByChat = lastMessagesDeferred.mapValues { it.value.await() }

            val conversations = chatIds.mapNotNull { chatId ->
                val otherUserId = otherParticipantsByChat[chatId]?.firstOrNull()?.userId ?: return@mapNotNull null
                val otherUser = otherUsers[otherUserId]
                val lastMsg = lastMessageByChat[chatId]

                Conversation(
                    chatId = chatId,
                    participantId = otherUserId,
                    participantName = otherUser?.displayName ?: otherUser?.username ?: otherUserId,
                    participantAvatar = otherUser?.avatar,
                    lastMessage = lastMsg?.content ?: "No messages yet",
                    lastMessageTime = lastMsg?.createdAt,
                    unreadCount = 0,
                    isOnline = otherUser?.status?.name == "ONLINE"
                )
            }

            Result.success(conversations.sortedByDescending { it.lastMessageTime })
        } catch (e: Exception) {
            Napier.e("Error loading conversations", e, tag = TAG)
            Result.failure(e)
        }
    }

    /**
     * Fetch messages for a specific chat, ordered chronologically.
     */
    suspend fun getMessages(chatId: String, limit: Int = 50): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val messages = client.from("messages")
                .select {
                    filter {
                        eq("chat_id", chatId)
                        eq("is_deleted", false)
                    }
                    order("created_at", Order.ASCENDING)
                    limit(limit.toLong())
                }.decodeList<ChatMessage>()

            Result.success(messages)
        } catch (e: Exception) {
            Napier.e("Error loading messages for chat $chatId", e, tag = TAG)
            Result.failure(e)
        }
    }

    /**
     * Send a text message.
     */
    suspend fun sendMessage(chatId: String, content: String, mediaUrl: String? = null, messageType: String = "text"): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val senderId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val newMessage = NewMessageDto(
                chatId = chatId,
                senderId = senderId,
                content = content,
                mediaUrl = mediaUrl,
                messageType = messageType
            )

            val inserted = client.from("messages")
                .insert(newMessage) {
                    select()
                }.decodeSingle<ChatMessage>()

            Result.success(inserted)
        } catch (e: Exception) {
            Napier.e("Error sending message to chat $chatId", e, tag = TAG)
            Result.failure(e)
        }
    }

    /**
     * Create or get an existing 1:1 chat between the current user and another user.
     * Returns the chat_id.
     */
    suspend fun getOrCreateChat(otherUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            // Check if a chat already exists between these two users
            val myChats = client.from("chat_participants")
                .select(columns = Columns.list("chat_id")) {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<ChatParticipantDto>()

            val chatIds = myChats.map { it.chatId }

            if (chatIds.isNotEmpty()) {
                val commonChat = client.from("chat_participants")
                    .select(columns = Columns.list("chat_id")) {
                        filter {
                            isIn("chat_id", chatIds)
                            eq("user_id", otherUserId)
                        }
                    }.decodeList<ChatParticipantDto>().firstOrNull()

                if (commonChat != null) {
                    return@withContext Result.success(commonChat.chatId)
                }
            }

            // No existing chat — create a new one
            val chatId = java.util.UUID.randomUUID().toString()

            // Insert both participants
            val participants = listOf(
                ChatParticipantDto(chatId = chatId, userId = currentUserId, isAdmin = true),
                ChatParticipantDto(chatId = chatId, userId = otherUserId)
            )
            client.from("chat_participants").insert(participants)

            Result.success(chatId)
        } catch (e: Exception) {
            Napier.e("Error creating chat with $otherUserId", e, tag = TAG)
            Result.failure(e)
        }
    }

    /**
     * Subscribe to real-time new messages for a specific chat.
     *
     * Implementation mirrors the zero-message-loss pattern in ChatRealtimeDataSource:
     * 1. Ensure the WebSocket is connected before creating the channel.
     * 2. Register the [postgresChangeFlow] collector BEFORE subscribing — the flow
     *    only registers a filter at this point and does not open the socket.
     * 3. Use [CompletableDeferred] so the subscribe coroutine waits until the collector
     *    has started (i.e. the flow's onStart has fired), closing the race window.
     * 4. Call [subscribe] with blockUntilSubscribed=true so messages sent during the
     *    handshake are buffered by the server and delivered upon confirmation.
     */
    fun subscribeToMessages(chatId: String): Flow<ChatMessage> {
        return callbackFlow {
            // 1. Ensure the WebSocket transport is connected.
            try {
                client.realtime.connect()
            } catch (e: Exception) {
                Napier.e("Failed to connect to Realtime WebSocket", e, tag = TAG)
            }

            val channel = client.channel("chat-messages-$chatId-${java.util.UUID.randomUUID()}") {}

            // 2. Register the change filter BEFORE subscribing.
            //    postgresChangeFlow only attaches a filter — it does NOT open the socket.
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
                filter("chat_id", FilterOperator.EQ, chatId)
            }

            // 3. Start the collector and signal readiness via CompletableDeferred.
            val subscriptionReady = kotlinx.coroutines.CompletableDeferred<Unit>()
            val collector = launch(Dispatchers.IO) {
                changeFlow
                    .onStart { subscriptionReady.complete(Unit) }
                    .collect { action ->
                        try {
                            send(action.decodeRecord<ChatMessage>())
                        } catch (e: Exception) {
                            Napier.e("Error decoding realtime chat message", e, tag = TAG)
                        }
                    }
            }

            // 4. Wait for the collector to be ready, then subscribe with blocking confirmation.
            launch(Dispatchers.IO) {
                try {
                    subscriptionReady.await()
                    kotlinx.coroutines.yield()
                    channel.subscribe(blockUntilSubscribed = true)
                    Napier.d("Successfully subscribed to chat channel for chatId=$chatId", tag = TAG)
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Napier.e("Failed to subscribe to chat realtime channel", e, tag = TAG)
                        close(e)
                    }
                }
            }

            awaitClose {
                Napier.d("Closing realtime channel for chatId=$chatId", tag = TAG)
                collector.cancel()
                launch {
                    try {
                        kotlinx.coroutines.yield()
                        channel.unsubscribe()
                        client.realtime.removeChannel(channel)
                    } catch (e: Exception) {
                        Napier.w("Failed to unsubscribe from chat channel", e, tag = TAG)
                    }
                }
            }
        }
    }

    /**
     * Get the other user's profile for a given chat.
     */
    suspend fun getChatParticipantProfile(chatId: String): User? = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext null

            val otherParticipants = client.from("chat_participants")
                .select(columns = Columns.list("user_id")) {
                    filter {
                        eq("chat_id", chatId)
                        neq("user_id", currentUserId)
                    }
                }.decodeList<ChatParticipantDto>()

            val otherUserId = otherParticipants.firstOrNull()?.userId ?: return@withContext null

            client.from("users")
                .select {
                    filter { eq("uid", otherUserId) }
                    limit(1)
                }.decodeSingleOrNull<User>()
        } catch (e: Exception) {
            Napier.e("Error fetching chat participant profile", e, tag = TAG)
            null
        }
    }
}
