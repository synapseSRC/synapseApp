package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.util.UUIDUtils

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantDto
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.NewChatDto


import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.NewMessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.UserPublicKeyDto
import com.synapse.social.studioasinc.shared.data.dto.chat.UserDeletedMessageDto
import com.synapse.social.studioasinc.shared.domain.model.User
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
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
import io.github.jan.supabase.realtime.*
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import com.synapse.social.studioasinc.shared.util.SynapseIO

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
import kotlinx.serialization.json.putJsonObject
import kotlin.coroutines.cancellation.CancellationException

class SupabaseChatDataSource(private val client: SupabaseClientLib = SupabaseClient.client) {
    
    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getConversations(): List<Triple<ChatParticipantDto, User?, ChatDto?>> = withContext(Dispatchers.SynapseIO) {
        val currentUserId = getCurrentUserId() ?: return@withContext emptyList()
        
        try {
            // 1. Get all my participations
            val myParticipations = client.postgrest.from("chat_participants")
                .select(columns = Columns.list("chat_id", "user_id", "is_archived", "last_read_at")) {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<ChatParticipantDto>()

            val activeParticipations = myParticipations.filter { !it.isArchived }
            if (activeParticipations.isEmpty()) return@withContext emptyList()

            val chatIds = activeParticipations.map { it.chatId }

            // 2. Get other participants for these chats in one batch
            val allOtherParticipants = client.postgrest.from("chat_participants")
                .select(columns = Columns.list("chat_id", "user_id")) {
                    filter {
                        isIn("chat_id", chatIds)
                        neq("user_id", currentUserId)
                    }
                }.decodeList<ChatParticipantDto>()
            
            val otherParticipantsByChat = allOtherParticipants.groupBy { it.chatId }

            val chats = if (chatIds.isNotEmpty()) {
                client.postgrest.from("chats")
                    .select(columns = Columns.list("id", "name", "avatar_url", "is_group", "created_by")) {
                        filter { isIn("id", chatIds) }
                    }.decodeList<ChatDto>().associateBy { it.id }
            } else {
                emptyMap()
            }

            val otherUserIds = allOtherParticipants.map { it.userId }.distinct()

            // 3. Get other user profiles in one batch
            val otherUsersByUid = if (otherUserIds.isNotEmpty()) {
                client.postgrest.from("users")
                    .select(columns = Columns.list("uid", "username", "display_name", "avatar", "status")) {
                        filter { isIn("uid", otherUserIds) }
                    }.decodeList<User>().associateBy { it.uid }
            } else {
                emptyMap()
            }

            // 4. Combine
            activeParticipations.map { participation ->
                val otherUserId = otherParticipantsByChat[participation.chatId]?.firstOrNull()?.userId
                val otherUser = otherUsersByUid[otherUserId ?: ""]
                Triple(participation, otherUser, chats[participation.chatId])
            }
        } catch (e: Exception) {
            Napier.e("Error loading conversations", e)
            emptyList()
        }
    }

    suspend fun getLastMessage(chatId: String): MessageDto? = withContext(Dispatchers.SynapseIO) {
        try {
            client.postgrest.from("messages").select {
                filter {
                    eq("chat_id", chatId)
                    eq("is_deleted", false)
                    or {
                        filter("expires_at", FilterOperator.IS, "null")
                        gt("expires_at", kotlinx.datetime.Clock.System.now().toString())
                    }
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeList<MessageDto>().firstOrNull()
        } catch (e: Exception) {
            Napier.e("Error loading last message for $chatId", e)
            null
        }
    }

    suspend fun getUnreadCount(chatId: String, lastReadAt: String?): Int = withContext(Dispatchers.SynapseIO) {
        if (lastReadAt == null) return@withContext 0
        try {
            val messages = client.postgrest.from("messages").select {
                filter {
                    eq("chat_id", chatId)
                    eq("is_deleted", false)
                    or {
                        filter("expires_at", FilterOperator.IS, "null")
                        gt("expires_at", kotlinx.datetime.Clock.System.now().toString())
                    }
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
        withContext(Dispatchers.SynapseIO) {
            try {
                client.postgrest.from("messages").select {
                    filter {
                        eq("chat_id", chatId)
                        eq("is_deleted", false)
                    or {
                        filter("expires_at", FilterOperator.IS, "null")
                        gt("expires_at", kotlinx.datetime.Clock.System.now().toString())
                    }
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

    suspend fun sendMessage(
        chatId: String, 
        content: String, 
        mediaUrl: String? = null, 
        messageType: String = "text", 
        isEncrypted: Boolean = false, 
        encryptedContent: String? = null,
        expiresAt: String? = null,
        replyToId: String? = null
    ): MessageDto =
        withContext(Dispatchers.SynapseIO) {
            val senderId = getCurrentUserId() ?: throw Exception("Not authenticated")
            val newMessage = NewMessageDto(chatId, senderId, content, messageType, mediaUrl, isEncrypted, encryptedContent, expiresAt, replyToId)
            client.postgrest.from("messages").insert(newMessage) { select() }.decodeSingle<MessageDto>()
        }

    suspend fun sendMessageNotification(recipientId: String, senderId: String, message: String, chatId: String) = withContext(Dispatchers.SynapseIO) {
        try {
            client.functions.invoke("send-push-notification", buildJsonObject {
                put("recipient_id", recipientId)
                put("sender_id", senderId)
                put("message", message)
                put("type", "NEW_MESSAGE")
                putJsonObject("data") {
                    put("chat_id", chatId)
                }
            })
        } catch (e: Exception) {
            Napier.e("Failed to send notification", e)
        }
    }

    /**
     * Looks up the other participant in a chat from the chat_participants table.
     * This is more reliable than parsing chatId strings.
     */
    suspend fun getOtherParticipantId(chatId: String, currentUserId: String): String? = withContext(Dispatchers.SynapseIO) {
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

    suspend fun getUserPublicKey(userId: String): UserPublicKeyDto? = withContext(Dispatchers.SynapseIO) {
        try {
            Napier.d("E2EE_KEY_FETCH: Fetching public key for user $userId", tag = "E2EE")
            val result = client.postgrest.from("user_public_keys").select {
                filter { eq("user_id", userId) }
                limit(1)
            }.decodeSingleOrNull<UserPublicKeyDto>()
            
            if (result != null) {
                Napier.d("E2EE_KEY_FETCH: Successfully fetched key for $userId", tag = "E2EE")
            } else {
                Napier.w("E2EE_KEY_FETCH: No key found for user $userId", tag = "E2EE")
            }
            result
        } catch (e: Exception) {
            Napier.e("E2EE_KEY_FETCH: Error fetching public key for $userId: ${e.message}", e, tag = "E2EE")
            null
        }
    }

    suspend fun uploadUserPublicKey(publicKey: String) = withContext(Dispatchers.SynapseIO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not authenticated")
            val dto = UserPublicKeyDto(currentUserId, publicKey)
            client.postgrest.from("user_public_keys").upsert(dto)
            Unit
        } catch (e: Exception) {
            Napier.e("Error uploading public key", e)
            throw e
        }
    }

    suspend fun getOrCreateChat(otherUserId: String): String? = withContext(Dispatchers.SynapseIO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext null
            
            val myChats = client.postgrest.from("chat_participants")
                .select(columns = Columns.list("chat_id", "user_id")) {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<ChatParticipantDto>()

            if (myChats.isNotEmpty()) {
                val myChatIds = myChats.map { it.chatId }

                val otherInChat = client.postgrest.from("chat_participants")
                    .select(columns = Columns.list("chat_id", "user_id")) {
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


    suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String? = null): String? = withContext(Dispatchers.SynapseIO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext null

            // Create chat entry
            val newChat = NewChatDto(isGroup = true, name = name, avatarUrl = avatarUrl, createdBy = currentUserId)
            val chat = client.postgrest.from("chats").insert(newChat) { select() }.decodeSingle<ChatDto>()
            val chatId = chat.id ?: return@withContext null

            // Create participants
            val participants = mutableListOf<ChatParticipantDto>()
            participants.add(ChatParticipantDto(chatId = chatId, userId = currentUserId, isAdmin = true))
            for (userId in participantIds) {
                if (userId != currentUserId) {
                    participants.add(ChatParticipantDto(chatId = chatId, userId = userId))
                }
            }

            client.postgrest.from("chat_participants").insert(participants)
            chatId
        } catch (e: Exception) {
            Napier.e("Error creating group chat", e)
            null
        }
    }

    suspend fun getChatInfo(chatId: String): ChatDto? = withContext(Dispatchers.SynapseIO) {
        try {
            client.postgrest.from("chats").select {
                filter { eq("id", chatId) }
                limit(1)
            }.decodeSingleOrNull<ChatDto>()
        } catch (e: Exception) {
            Napier.e("Error fetching chat info $chatId", e)
            null
        }
    }

    suspend fun getGroupMembers(chatId: String): List<Pair<User, Boolean>> = withContext(Dispatchers.SynapseIO) {
        try {
            val participants = client.postgrest.from("chat_participants").select {
                filter { eq("chat_id", chatId) }
            }.decodeList<ChatParticipantDto>()

            val userIds = participants.map { it.userId }
            if (userIds.isEmpty()) return@withContext emptyList()

            val users = client.postgrest.from("users").select {
                filter { isIn("uid", userIds) }
            }.decodeList<User>().associateBy { it.uid }

            participants.mapNotNull { participant ->
                val user = users[participant.userId]
                if (user != null) Pair(user, participant.isAdmin) else null
            }
        } catch (e: Exception) {
            Napier.e("Error getting group members", e)
            emptyList()
        }
    }

    suspend fun addGroupMembers(chatId: String, userIds: List<String>) = withContext(Dispatchers.SynapseIO) {
        try {
            val participants = userIds.map { ChatParticipantDto(chatId = chatId, userId = it) }
            client.postgrest.from("chat_participants").insert(participants)
        } catch (e: Exception) {
            Napier.e("Error adding group member", e)
            throw e
        }
    }

    suspend fun removeGroupMember(chatId: String, userId: String) = withContext(Dispatchers.SynapseIO) {
        try {
            client.postgrest.from("chat_participants").delete {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error removing group member", e)
            throw e
        }
    }

    suspend fun markMessagesAsRead(chatId: String) = withContext(Dispatchers.SynapseIO) {
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

            // Optimize: Fetch only necessary columns
            val messages = client.postgrest.from("messages").select(columns = Columns.list("id", "read_by")) {
                filter {
                    eq("chat_id", chatId)
                    neq("sender_id", currentUserId)
                }
            }.decodeList<MessageDto>()

            // Optimize: Group messages by their current read_by list to perform bulk updates
            messages
                .filter { it.id != null && it.readBy?.contains(currentUserId) != true }
                .groupBy { it.readBy ?: emptyList<String>() }
                .forEach { (oldReadBy, msgs) ->
                    val newReadBy = oldReadBy + currentUserId
                    val ids = msgs.mapNotNull { it.id }

                    if (ids.isNotEmpty()) {
                        client.postgrest.from("messages").update({
                            set("read_by", newReadBy)
                        }) {
                            filter { isIn("id", ids) }
                        }
                    }
                }
        } catch (e: Exception) {
            Napier.e("Error marking messages as read", e)
        }
    }

    suspend fun getMessageById(messageId: String): MessageDto? = withContext(Dispatchers.SynapseIO) {
        try {
            client.postgrest.from("messages").select {
                filter { eq("id", messageId) }
                limit(1)
            }.decodeSingleOrNull<MessageDto>()
        } catch (e: Exception) {
            Napier.e("Error fetching message $messageId", e)
            null
        }
    }

    suspend fun editMessage(
        messageId: String, 
        newContent: String, 
        isEncrypted: Boolean = false, 
        encryptedContent: String? = null
    ) = withContext(Dispatchers.SynapseIO) {
        try {
            client.postgrest.from("messages").update({
                set("content", newContent)
                set("is_edited", true)
                set("is_encrypted", isEncrypted)
                set("encrypted_content", encryptedContent)
            }) {
                filter { eq("id", messageId) }
            }
        } catch (e: Exception) {
            Napier.e("Error editing message", e)
            throw e
        }
    }

    suspend fun deleteMessage(messageId: String): Unit = 
        withContext(Dispatchers.SynapseIO) {
            try {
                client.postgrest.from("messages").update({
                    set("is_deleted", true)
                }) {
                    filter { eq("id", messageId) }
                }
            } catch (e: Exception) {
                Napier.e("Error deleting message", e)
                throw e
            }
        }

    suspend fun deleteMessageForMe(messageId: String): Unit =
        withContext(Dispatchers.SynapseIO) {
            val userId = getCurrentUserId() ?: throw Exception("Not authenticated")
            val deletion = UserDeletedMessageDto(messageId = messageId, userId = userId)
            client.postgrest.from("user_deleted_messages").insert(deletion)
        }


    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean) = 
        withContext(Dispatchers.SynapseIO) {
            try {
                val currentUserId = getCurrentUserId() ?: return@withContext
                val channel = client.realtime.channel("chat-$chatId")
                
                channel.subscribe(blockUntilSubscribed = true)
                channel.track(buildJsonObject {
                    put("user_id", currentUserId)
                    put("is_typing", isTyping)
                })
            } catch (e: Exception) {
                Napier.e("Error broadcasting typing status", e)
            }
        }

    fun subscribeToMessages(chatId: String): Flow<MessageDto> = callbackFlow {
        val channel = client.realtime.channel("chat-$chatId-${UUIDUtils.randomUUID()}")
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        val collector = launch {
            flow.map { it.decodeRecord<MessageDto>() }.collect { message ->
                trySend(message)
            }
        }

        launch(Dispatchers.SynapseIO) {
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
        val channel = client.realtime.channel("inbox-updates-${UUIDUtils.randomUUID()}")
        // No filter on chatId here - let Supabase RLS filter it to only messages the user has access to.
        // This allows us to receive the first message of a NEW chat that wasn't in the 'chatIds' list yet.
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }

        val collector = launch {
            flow.collect { action ->
                try {
                    val message = action.decodeRecord<MessageDto>()
                    // Even though RLS filters it, we can still filter by chatIds if we want 
                    // to be very specific, but for Inbox we want to know about ANY chat.
                    trySend(message)
                } catch (e: Exception) {
                    Napier.e("Error decoding real-time message in inbox", e)
                }
            }
        }

        launch {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                Napier.e("Failed to subscribe to inbox channel", e)
                close(e)
            }
        }

        awaitClose {
            collector.cancel()
            launch {
                try {
                    channel.unsubscribe()
                } catch (_: Exception) { }
            }
        }
    }

    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> = callbackFlow {
        val channel = client.realtime.channel("chat-$chatId")
        
        val collector = launch {
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
                            // When user leaves presence, typing status is false
                            trySend(mapOf("user_id" to userId, "is_typing" to false))
                        }
                    } catch (e: Exception) {
                        Napier.e("Error decoding presence leave state", e)
                    }
                }
            }
        }

        launch(Dispatchers.SynapseIO) {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Napier.e("Failed to subscribe to chat presence", e)
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

    fun subscribeToReadReceipts(chatId: String): Flow<MessageDto> = callbackFlow {
        val channel = client.realtime.channel("read-receipts-$chatId-${UUIDUtils.randomUUID()}")
        val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }

        val collector = launch {
            flow.map { it.decodeRecord<MessageDto>() }.collect { message ->
                trySend(message)
            }
        }

        launch(Dispatchers.SynapseIO) {
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
