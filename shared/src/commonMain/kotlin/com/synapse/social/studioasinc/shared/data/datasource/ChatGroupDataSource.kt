package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantDto
import com.synapse.social.studioasinc.shared.data.dto.chat.GroupInviteCodeDto
import com.synapse.social.studioasinc.shared.data.dto.chat.GroupInvitePreviewDto
import com.synapse.social.studioasinc.shared.data.dto.chat.GroupJoinRequestDto
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupInvitePreview
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinOutcome
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinRequest
import com.synapse.social.studioasinc.shared.domain.model.User
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class ChatGroupDataSource(private val client: SupabaseClientLib) {

    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun reportChat(chatId: String, reason: String) = withContext(AppDispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
        client.postgrest.from("reports").insert(buildJsonObject {
            put("reporter_id", currentUserId)
            put("target_id", chatId)
            put("target_type", "chat")
            put("reason", reason)
        })
    }

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
            client.postgrest.rpc("remove_group_member", mapOf("p_chat_id" to chatId, "p_user_id" to userId))
        } catch (e: Exception) {
            Napier.e("Error removing group member", e)
            throw e
        }
    }

    suspend fun promoteToAdmin(chatId: String, userId: String) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.rpc("set_group_member_admin", buildJsonObject {
                put("p_chat_id", chatId)
                put("p_user_id", userId)
                put("p_is_admin", true)
            })
        } catch (e: Exception) {
            Napier.e("Error promoting member to admin", e)
            throw e
        }
    }

    suspend fun demoteAdmin(chatId: String, userId: String) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.rpc("set_group_member_admin", buildJsonObject {
                put("p_chat_id", chatId)
                put("p_user_id", userId)
                put("p_is_admin", false)
            })
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
            client.postgrest.rpc("update_group_message_permission", buildJsonObject {
                put("p_chat_id", chatId)
                put("p_only_admins_can_message", enabled)
            })
        } catch (e: Exception) {
            Napier.e("Error toggling only_admins_can_message", e)
            throw e
        }
    }

    suspend fun getGroupInviteCode(chatId: String): String? = withContext(AppDispatchers.IO) {
        val row = client.postgrest.from("chats").select(columns = Columns.list("invite_code")) {
            filter { eq("id", chatId) }
            limit(1)
        }.decodeList<GroupInviteCodeDto>().firstOrNull()
        row?.inviteCode
    }

    suspend fun rotateGroupInviteCode(chatId: String): String = withContext(AppDispatchers.IO) {
        client.postgrest.rpc("rotate_group_invite_code", mapOf("p_chat_id" to chatId))
            .decodeAsOrNull<String>()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("The invite link could not be regenerated.")
    }

    suspend fun getPendingGroupJoinRequests(chatId: String): List<GroupJoinRequest> = withContext(AppDispatchers.IO) {
        val pending = client.postgrest.from("group_join_requests").select {
            filter {
                eq("chat_id", chatId)
                eq("status", "pending")
            }
        }.decodeList<GroupJoinRequestDto>()
        if (pending.isEmpty()) return@withContext emptyList()
        val users = client.postgrest.from("users").select {
            filter { isIn("uid", pending.map { it.userId }.distinct()) }
        }.decodeList<User>().associateBy { it.uid }
        pending.mapNotNull { request ->
            users[request.userId]?.let { user -> GroupJoinRequest(request.id, request.chatId, user, request.createdAt) }
        }
    }

    suspend fun approveGroupJoinRequest(requestId: String) = withContext(AppDispatchers.IO) {
        client.postgrest.rpc("approve_group_join_request", mapOf("p_request_id" to requestId))
    }

    suspend fun declineGroupJoinRequest(requestId: String) = withContext(AppDispatchers.IO) {
        client.postgrest.rpc("decline_group_join_request", mapOf("p_request_id" to requestId))
    }

    suspend fun resolveGroupInvite(inviteCode: String): GroupInvitePreview? = withContext(AppDispatchers.IO) {
        client.postgrest.rpc("resolve_group_invite", mapOf("p_invite_code" to inviteCode))
            .decodeList<GroupInvitePreviewDto>().firstOrNull()?.let { dto ->
                GroupInvitePreview(
                    chatId = dto.chatId,
                    name = dto.name,
                    description = dto.description,
                    avatarUrl = dto.avatarUrl,
                    requireJoinApproval = dto.requireJoinApproval,
                    memberCount = dto.memberCount.coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                )
            }
    }

    suspend fun requestGroupJoinByInvite(inviteCode: String): GroupJoinOutcome = withContext(AppDispatchers.IO) {
        when (client.postgrest.rpc("request_group_join_by_invite", mapOf("p_invite_code" to inviteCode)).decodeAsOrNull<String>()) {
            "joined" -> GroupJoinOutcome.JOINED
            "pending" -> GroupJoinOutcome.REQUESTED
            "already_member" -> GroupJoinOutcome.ALREADY_MEMBER
            else -> throw IllegalStateException("The invite response was not recognized.")
        }
    }

}
