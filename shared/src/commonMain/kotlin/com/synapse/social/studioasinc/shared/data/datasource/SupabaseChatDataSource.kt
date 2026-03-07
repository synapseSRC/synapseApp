package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantDto
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.NewMessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.UserPublicKeyDto
import com.synapse.social.studioasinc.shared.domain.model.User
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
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
import kotlin.coroutines.cancellation.CancellationException

class SupabaseChatDataSource(private val client: SupabaseClientLib = SupabaseClient.client) {
    
    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getConversations(): List<Pair<ChatParticipantDto, User?>> = withContext(Dispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: return@withContext emptyList()
        
        val myParticipations = client.postgrest.from("chat_participants")
            .select(columns = Columns.list("chat_id", "user_id", "is_archived", "last_read_at")) {
                filter { eq("user_id", currentUserId) }
            }.decodeList<ChatParticipantDto>()

        myParticipations.filter { !it.isArchived }.mapNotNull { participation ->
            try {
                val otherParticipants = client.postgrest.from("chat_participants")
                    .select(columns = Columns.list("user_id")) {
                        filter {
                            eq("chat_id", participation.chatId)
                            neq("user_id", currentUserId)
                        }
                    }.decodeList<ChatParticipantDto>()

                val otherUserId = otherParticipants.firstOrNull()?.userId ?: return@mapNotNull null
                val otherUser = client.postgrest.from("users").select {
                    filter { eq("uid", otherUserId) }
                    limit(1)
                }.decodeSingleOrNull<User>()

                participation to otherUser
            } catch (e: Exception) {
                Napier.e("Error loading conversation ${participation.chatId}", e)
                null
            }
        }
    }

    suspend fun getLastMessage(chatId: String): MessageDto? = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from("messages").select {
                filter {
                    eq("chat_id", chatId)
                    eq("is_deleted", false)
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeList<MessageDto>().firstOrNull()
        } catch (e: Exception) {
            Napier.e("Error loading last message for $chatId", e)
            null
        }
    }

    suspend fun getUnreadCount(chatId: String, lastReadAt: String?): Int = withContext(Dispatchers.IO) {
        if (lastReadAt == null) return@withContext 0
        try {
            val messages = client.postgrest.from("messages").select {
                filter {
                    eq("chat_id", chatId)
                    eq("is_deleted", false)
                    gt("created_at", lastReadAt)
                }
            }.decodeList<MessageDto>()
            messages.size
        } catch (e: Exception) {
            Napier.e("Error counting unread messages", e)
            0
        }
    }

    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null): List<MessageDto> = 
        withContext(Dispatchers.IO) {
            try {
                client.postgrest.from("messages").select {
                    filter {
                        eq("chat_id", chatId)
                        eq("is_deleted", false)
                        if (before != null) {
                            lt("created_at", before)
                        }
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }.decodeList<MessageDto>().reversed()
            } catch (e: Exception) {
                Napier.e("Error loading messages for $chatId", e)
                emptyList()
            }
        }

    suspend fun sendMessage(chatId: String, content: String, mediaUrl: String? = null, messageType: String = "text", isEncrypted: Boolean = false, encryptedContent: String? = null): MessageDto =
        withContext(Dispatchers.IO) {
            val senderId = getCurrentUserId() ?: throw Exception("Not authenticated")
            val newMessage = NewMessageDto(chatId, senderId, content, messageType, mediaUrl, isEncrypted, encryptedContent)
            client.postgrest.from("messages").insert(newMessage) { select() }.decodeSingle<MessageDto>()
        }

    /**
     * Looks up the other participant in a chat from the chat_participants table.
     * This is more reliable than parsing chatId strings.
     */
    suspend fun getOtherParticipantId(chatId: String, currentUserId: String): String? = withContext(Dispatchers.IO) {
        try {
            val participants = client.postgrest.from("chat_participants")
                .select(columns = Columns.list("user_id")) {
                    filter {
                        eq("chat_id", chatId)
                        neq("user_id", currentUserId)
                    }
                    limit(1)
                }.decodeList<ChatParticipantDto>()
            participants.firstOrNull()?.userId
        } catch (e: Exception) {
            Napier.e("Error looking up other participant for chat $chatId", e)
            null
        }
    }

    suspend fun getUserPublicKey(userId: String): UserPublicKeyDto? = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from("user_public_keys").select {
                filter { eq("user_id", userId) }
                limit(1)
            }.decodeSingleOrNull<UserPublicKeyDto>()
        } catch (e: Exception) {
            Napier.e("Error fetching public key for $userId", e)
            null
        }
    }

    suspend fun uploadUserPublicKey(publicKey: String) = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not authenticated")
            val dto = UserPublicKeyDto(currentUserId, publicKey)
            client.postgrest.from("user_public_keys").upsert(dto)
        } catch (e: Exception) {
            Napier.e("Error uploading public key", e)
            throw e
        }
    }

    suspend fun getOrCreateChat(otherUserId: String): String? = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext null
            
            val myChats = client.postgrest.from("chat_participants")
                .select(columns = Columns.list("chat_id")) {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<ChatParticipantDto>()

            if (myChats.isNotEmpty()) {
                val myChatIds = myChats.map { it.chatId }

                val otherInChat = client.postgrest.from("chat_participants")
                    .select(columns = Columns.list("chat_id")) {
                        filter {
                            isIn("chat_id", myChatIds)
                            eq("user_id", otherUserId)
                        }
                        limit(1)
                    }.decodeList<ChatParticipantDto>()

                val existingChatId = otherInChat.firstOrNull()?.chatId
                if (existingChatId != null) return@withContext existingChatId
            }

            val chatId = com.synapse.social.studioasinc.shared.util.UUIDUtils.randomUUID()
            val participants = listOf(
                ChatParticipantDto(chatId = chatId, userId = currentUserId, isAdmin = true),
                ChatParticipantDto(chatId = chatId, userId = otherUserId)
            )
            client.postgrest.from("chat_participants").insert(participants)
            chatId
        } catch (e: Exception) {
            Napier.e("Error creating chat", e)
            null
        }
    }

    suspend fun markMessagesAsRead(chatId: String) = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext
            val now = kotlinx.datetime.Clock.System.now().toString()
            
            client.postgrest.from("chat_participants").update({
                set("last_read_at", now)
            }) {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", currentUserId)
                }
            }

            val messages = client.postgrest.from("messages").select {
                filter {
                    eq("chat_id", chatId)
                    neq("sender_id", currentUserId)
                }
            }.decodeList<MessageDto>()

            messages.forEach { msg ->
                msg.id?.let { messageId ->
                    val readByList = msg.readBy?.split(",")?.toMutableList() ?: mutableListOf()
                    if (!readByList.contains(currentUserId)) {
                        readByList.add(currentUserId)
                        client.postgrest.from("messages").update({
                            set("read_by", readByList.joinToString(","))
                        }) {
                            filter { eq("id", messageId) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("Error marking messages as read", e)
        }
    }

    suspend fun editMessage(messageId: String, newContent: String) = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from("messages").update({
                set("content", newContent)
                set("is_edited", true)
            }) {
                filter { eq("id", messageId) }
            }
        } catch (e: Exception) {
            Napier.e("Error editing message", e)
            throw e
        }
    }

    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from("messages").update({
                set("is_deleted", true)
                set("content", "")
            }) {
                filter { eq("id", messageId) }
            }
        } catch (e: Exception) {
            Napier.e("Error deleting message", e)
            throw e
        }
    }

    suspend fun uploadMedia(chatId: String, fileBytes: ByteArray, fileName: String, contentType: String): String =
        withContext(Dispatchers.IO) {
            try {
                val path = "chat_media/$chatId/$fileName"
                client.storage.from("chat_attachments").upload(path, fileBytes)
                client.storage.from("chat_attachments").publicUrl(path)
            } catch (e: Exception) {
                Napier.e("Error uploading media", e)
                throw e
            }
        }

    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean, channel: RealtimeChannel) = 
        withContext(Dispatchers.IO) {
            try {
                // Typing status via presence - simplified for now
                // Will be implemented with proper Supabase Presence API
            } catch (e: Exception) {
                Napier.e("Error broadcasting typing status", e)
            }
        }

    fun subscribeToMessages(chatId: String): Flow<MessageDto> = callbackFlow {
        val channel = client.realtime.channel("chat-$chatId")
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        val collector = launch {
            flow.map { it.decodeRecord<MessageDto>() }.collect { message ->
                trySend(message)
            }
        }

        launch(Dispatchers.IO) {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to chat", e)
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
                    Napier.w("Failed to unsubscribe", e)
                }
            }
        }
    }

    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<MessageDto> = callbackFlow {
        if (chatIds.isEmpty()) {
            close()
            return@callbackFlow
        }

        val channel = client.realtime.channel("inbox-updates")
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }

        val collector = launch {
            flow.map { it.decodeRecord<MessageDto>() }
                .collect { message ->
                    if (message.chatId in chatIds) {
                        trySend(message)
                    }
                }
        }

        launch(Dispatchers.IO) {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to inbox", e)
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
                    Napier.w("Failed to unsubscribe", e)
                }
            }
        }
    }

    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> = callbackFlow {
        // Simplified - will implement with proper Supabase Presence API
        awaitClose { }
    }

    fun subscribeToReadReceipts(chatId: String): Flow<MessageDto> = callbackFlow {
        val channel = client.realtime.channel("read-receipts-$chatId")
        val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        val collector = launch {
            flow.map { it.decodeRecord<MessageDto>() }.collect { message ->
                trySend(message)
            }
        }

        launch(Dispatchers.IO) {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to read receipts", e)
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
                    Napier.w("Failed to unsubscribe", e)
                }
            }
        }
    }
}
