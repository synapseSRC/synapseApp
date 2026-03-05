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
import io.github.jan.supabase.realtime.decodeRecord
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class ChatMessagingRepository @Inject constructor(
    private val client: SupabaseClient
) {
    companion object {
        private const val TAG = "ChatMessagingRepository"
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
    suspend fun sendMessage(chatId: String, content: String): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val senderId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val newMessage = NewMessageDto(
                chatId = chatId,
                senderId = senderId,
                content = content
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
            val chatId = "${currentUserId}_${otherUserId}"

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
     * Uses callbackFlow + channel.subscribe() matching the notification pattern.
     */
    fun subscribeToMessages(chatId: String): Flow<ChatMessage> {
        return callbackFlow {
            val channel = client.channel("chat-$chatId") {}
            val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
                filter("chat_id", FilterOperator.EQ, chatId)
            }

            val collector = launch {
                flow.map { it.decodeRecord<ChatMessage>() }.collect { message ->
                    trySend(message)
                }
            }

            launch(Dispatchers.IO) {
                try {
                    channel.subscribe()
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Napier.e("Failed to subscribe to chat realtime channel", e, tag = TAG)
                        close(e)
                    }
                }
            }

            awaitClose {
                collector.cancel()
                launch {
                    try {
                        channel.unsubscribe()
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
                    filter { eq("id", otherUserId) }
                    limit(1)
                }.decodeSingleOrNull<User>()
        } catch (e: Exception) {
            Napier.e("Error fetching chat participant profile", e, tag = TAG)
            null
        }
    }
}
