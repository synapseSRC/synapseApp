package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantDto
import com.synapse.social.studioasinc.shared.domain.model.User
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ChatGroupDataSource(private val client: SupabaseClientLib) {

    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getGroupMembers(chatId: String): List<Pair<User, Boolean>> = withContext(AppDispatchers.IO) {
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

    suspend fun addGroupMembers(chatId: String, userIds: List<String>) = withContext(AppDispatchers.IO) {
        try {
            val participants = userIds.map { ChatParticipantDto(chatId = chatId, userId = it) }
            client.postgrest.from("chat_participants").insert(participants)
        } catch (e: Exception) {
            Napier.e("Error adding group member", e)
            throw e
        }
    }

    suspend fun removeGroupMember(chatId: String, userId: String) = withContext(AppDispatchers.IO) {
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

    suspend fun promoteToAdmin(chatId: String, userId: String) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from("chat_participants").update({
                set("is_admin", true)
            }) {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error promoting member to admin", e)
            throw e
        }
    }

    suspend fun demoteAdmin(chatId: String, userId: String) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from("chat_participants").update({
                set("is_admin", false)
            }) {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error demoting admin", e)
            throw e
        }
    }

    suspend fun leaveGroup(chatId: String) = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            client.postgrest.from("chat_participants").delete {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", currentUserId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error leaving group", e)
            throw e
        }
    }

    suspend fun toggleOnlyAdminsCanMessage(chatId: String, enabled: Boolean) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from("chats").update({
                set("only_admins_can_message", enabled)
            }) {
                filter {
                    eq("id", chatId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error toggling only_admins_can_message", e)
            throw e
        }
    }
}
