package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantDto
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.NewChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.domain.model.User
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ChatConversationDataSource(private val client: SupabaseClientLib) {

    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getConversations(): List<Triple<ChatParticipantDto, User?, ChatDto?>> = withContext(AppDispatchers.IO) {
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

    suspend fun getLastMessage(chatId: String): MessageDto? = withContext(AppDispatchers.IO) {
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

    suspend fun getUnreadCount(chatId: String, lastReadAt: String?): Int = withContext(AppDispatchers.IO) {
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

    suspend fun getOrCreateChat(otherUserId: String): String? = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext null

            // 1. Get all chats the current user is in
            val myChats = client.postgrest.from("chat_participants")
                .select(columns = Columns.list("chat_id", "user_id")) {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<ChatParticipantDto>()

            if (myChats.isNotEmpty()) {
                val myChatIds = myChats.map { it.chatId }

                // 2. Filter these chats to only include 1-on-1 chats (not groups)
                val directChats = client.postgrest.from("chats")
                    .select(columns = Columns.list("id")) {
                        filter {
                            isIn("id", myChatIds)
                            eq("is_group", false)
                        }
                    }.decodeList<ChatDto>()

                val directChatIds = directChats.mapNotNull { it.id }

                if (directChatIds.isNotEmpty()) {
                    // 3. Check if the other user is in any of these direct chats
                    val otherInChat = client.postgrest.from("chat_participants")
                        .select(columns = Columns.list("chat_id", "user_id")) {
                            filter {
                                isIn("chat_id", directChatIds)
                                eq("user_id", otherUserId)
                            }
                            limit(1)
                        }.decodeList<ChatParticipantDto>()

                    val existingChatId = otherInChat.firstOrNull()?.chatId
                    if (existingChatId != null) return@withContext existingChatId
                }
            }

            // 4. If no existing direct chat, create a new one in the 'chats' table first
            val chatId = com.synapse.social.studioasinc.shared.util.UUIDUtils.randomUUID()
            val newChat = NewChatDto(id = chatId, isGroup = false, createdBy = currentUserId)
            client.postgrest.from("chats").insert(newChat)

            // 5. Then add both participants to 'chat_participants'
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

    suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String? = null): String? = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext null

            // Create chat entry
            val chatId = com.synapse.social.studioasinc.shared.util.UUIDUtils.randomUUID()
            val newChat = NewChatDto(id = chatId, isGroup = true, name = name, avatarUrl = avatarUrl, createdBy = currentUserId)
            client.postgrest.from("chats").insert(newChat)

            // Create participants
            val participants = buildList(participantIds.size + 1) {
                add(ChatParticipantDto(chatId = chatId, userId = currentUserId, isAdmin = true))
                participantIds.forEach {
                    if (it != currentUserId) {
                        add(ChatParticipantDto(chatId = chatId, userId = it))
                    }
                }
            }

            client.postgrest.from("chat_participants").insert(participants)
            chatId
        } catch (e: Exception) {
            Napier.e("Error creating group chat", e)
            null
        }
    }

    suspend fun getChatInfo(chatId: String): ChatDto? = withContext(AppDispatchers.IO) {
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

    suspend fun getParticipantIds(chatId: String): List<String> = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from("chat_participants")
                .select(columns = Columns.list("chat_id", "user_id")) {
                    filter { eq("chat_id", chatId) }
                }.decodeList<ChatParticipantDto>().map { it.userId }
        } catch (e: Exception) {
            Napier.e("Error getting participant ids", e)
            emptyList()
        }
    }

    suspend fun updateConversationArchiveStatus(chatId: String, isArchived: Boolean) = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            client.postgrest.from("chat_participants").update({
                set("is_archived", isArchived)
            }) {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", currentUserId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error updating archive status", e)
            throw e
        }
    }

    suspend fun deleteConversation(chatId: String) = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            // Instead of deleting the participant which might break group logic, we might just soft-delete or remove the participant
            client.postgrest.from("chat_participants").delete {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", currentUserId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error deleting conversation", e)
            throw e
        }
    }
}
