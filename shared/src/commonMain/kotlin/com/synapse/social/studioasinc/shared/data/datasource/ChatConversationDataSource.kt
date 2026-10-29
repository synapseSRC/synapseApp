package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantDto
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.NewChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.NewDirectChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.NewMinimalGroupChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatInboxSummaryDto
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupCreationOptions
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ChatConversationDataSource(private val client: SupabaseClientLib) {

    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getInboxSummaries(archivedOnly: Boolean = false): List<ChatInboxSummaryDto> = withContext(AppDispatchers.IO) {
        if (getCurrentUserId() == null) throw IllegalStateException("Not logged in: currentUserId is null")
        client.postgrest.rpc("get_chat_inbox_summaries", mapOf("p_archived_only" to archivedOnly))
            .decodeList<ChatInboxSummaryDto>()
    }

    suspend fun getConversations(archivedOnly: Boolean = false): List<Triple<ChatParticipantDto, User?, ChatDto?>> = withContext(AppDispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: throw IllegalStateException("Not logged in: currentUserId is null")

        try {
            // 1. Get all my participations
            val myParticipations = client.postgrest.from("chat_participants")
                .select(columns = Columns.list("chat_id", "user_id", "is_archived", "is_pinned", "is_muted", "last_read_at")) {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<ChatParticipantDto>()

            val activeParticipations = myParticipations.filter { it.isArchived == archivedOnly }
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
            throw e
        }
    }

    suspend fun getOrCreateChat(otherUserId: String): String = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw IllegalStateException("Not logged in")

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
            val newChat = NewDirectChatDto(id = chatId, isGroup = false, createdBy = currentUserId)
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
            throw e
        }
    }

    suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String? = null, options: GroupCreationOptions = GroupCreationOptions()): String? = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext null

            // Create chat entry
            val chatId = com.synapse.social.studioasinc.shared.util.UUIDUtils.randomUUID()
            val newChat = NewChatDto(
                id = chatId, isGroup = true, name = name, description = options.description, avatarUrl = avatarUrl,
                createdBy = currentUserId, onlyAdminsCanMessage = !options.sendMessagesByMembers,
                allowMemberSettingsEdits = options.allowMemberSettingsEdits, requireJoinApproval = options.requireJoinApproval,
                allowMemberInvites = options.allowMemberInvites, disappearingMode = options.disappearingMode.name
            )
            try {
                client.postgrest.from("chats").insert(newChat)
            } catch (e: Exception) {
                // Fallback: insert minimal chat if optional group-settings columns are missing
                Napier.w("Full group chat insert failed, retrying minimal insert", e)
                val minimalChat = NewMinimalGroupChatDto(id = chatId, name = name, createdBy = currentUserId)
                client.postgrest.from("chats").insert(minimalChat)
            }

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
        client.postgrest.from("chat_participants")
            .select(columns = Columns.list("chat_id", "user_id")) {
                filter { eq("chat_id", chatId) }
            }.decodeList<ChatParticipantDto>().map { it.userId }
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

    suspend fun updateConversationPinnedStatus(chatId: String, isPinned: Boolean) = withContext(AppDispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
        client.postgrest.from("chat_participants").update({
            set("is_pinned", isPinned)
        }) {
            filter { eq("chat_id", chatId); eq("user_id", currentUserId) }
        }
    }

    suspend fun updateConversationMutedStatus(chatId: String, isMuted: Boolean) = withContext(AppDispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
        client.postgrest.from("chat_participants").update({
            set("is_muted", isMuted)
        }) {
            filter { eq("chat_id", chatId); eq("user_id", currentUserId) }
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

    suspend fun setDisappearingMode(chatId: String, mode: String) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.rpc("update_group_disappearing_mode", mapOf("p_chat_id" to chatId, "p_disappearing_mode" to mode))
        } catch (e: Exception) {
            Napier.e("Error setting disappearing mode", e)
            throw e
        }
    }

    suspend fun getDisappearingMode(chatId: String): String? = withContext(AppDispatchers.IO) {
        try {
            val chatDto = client.postgrest.from("chats").select {
                filter { eq("id", chatId) }
                limit(1)
            }.decodeSingleOrNull<ChatDto>()
            chatDto?.disappearingMode
        } catch (e: Exception) {
            Napier.e("Error getting disappearing mode", e)
            null
        }
    }
    suspend fun getConversationMuteStatus(chatId: String): Boolean = withContext(AppDispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
        val participant = client.postgrest.from("chat_participants")
            .select(columns = Columns.list("chat_id", "user_id", "is_muted")) {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", currentUserId)
                }
            }
            .decodeList<ChatParticipantDto>()
            .firstOrNull()
            ?: throw IllegalStateException("Chat participant settings were not found")
        participant.isMuted
    }

    suspend fun setConversationMuteStatus(chatId: String, isMuted: Boolean) = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            client.postgrest.from("chat_participants").update({
                set("is_muted", isMuted)
            }) {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", currentUserId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error updating conversation mute status", e)
            throw e
        }
    }
}
