package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.chat.MessageReactionDto
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ChatReactionDataSource(private val client: SupabaseClient) {

    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun toggleReaction(messageId: String, emoji: String, chatId: String? = null): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("Not authenticated"))

            val existingList = client.from("message_reactions").select {
                filter {
                    eq("message_id", messageId)
                    eq("user_id", userId)
                }
            }.decodeList<MessageReactionDto>()

            val existing = existingList.firstOrNull()

            if (existing != null) {
                if (existing.reactionEmoji == emoji) {
                    client.from("message_reactions").delete {
                        filter {
                            eq("message_id", messageId)
                            eq("user_id", userId)
                        }
                    }
                } else {
                    client.from("message_reactions").update(
                        kotlinx.serialization.json.buildJsonObject {
                            put("reaction_type", kotlinx.serialization.json.JsonPrimitive(emoji))
                        }
                    ) {
                        filter {
                            eq("message_id", messageId)
                            eq("user_id", userId)
                        }
                    }
                }
            } else {
                client.from("message_reactions").insert(
                    kotlinx.serialization.json.buildJsonObject {
                        put("message_id", kotlinx.serialization.json.JsonPrimitive(messageId))
                        put("user_id", kotlinx.serialization.json.JsonPrimitive(userId))
                        put("reaction_type", kotlinx.serialization.json.JsonPrimitive(emoji))
                        if (chatId != null) {
                            put("chat_id", kotlinx.serialization.json.JsonPrimitive(chatId))
                        }
                    }
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error toggling reaction", e)
            Result.failure(e)
        }
    }

    suspend fun getReactionsForMessage(messageId: String): List<MessageReactionDto> = withContext(AppDispatchers.IO) {
        try {
            client.from("message_reactions").select {
                filter { eq("message_id", messageId) }
            }.decodeList<MessageReactionDto>()
        } catch (e: Exception) {
            Napier.e("Error fetching reactions for message $messageId", e)
            emptyList()
        }
    }

    suspend fun getReactionsForMessages(messageIds: List<String>): List<MessageReactionDto> = withContext(AppDispatchers.IO) {
        if (messageIds.isEmpty()) return@withContext emptyList()
        try {
            client.from("message_reactions").select {
                filter { isIn("message_id", messageIds) }
            }.decodeList<MessageReactionDto>()
        } catch (e: Exception) {
            Napier.e("Error fetching reactions for multiple messages", e)
            emptyList()
        }
    }
}
