package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.chat.MessageReactionDto
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ChatReactionDataSource(private val client: SupabaseClient) {

    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun toggleReaction(messageId: String, emoji: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("Not authenticated"))

            // Check if reaction exists
            val existing = client.from("message_reactions").select {
                filter {
                    eq("message_id", messageId)
                    eq("user_id", userId)
                    eq("reaction_type", emoji)
                }
            }.decodeSingleOrNull<MessageReactionDto>()

            if (existing != null) {
                // Remove it
                client.from("message_reactions").delete {
                    filter {
                        eq("message_id", messageId)
                        eq("user_id", userId)
                        eq("reaction_type", emoji)
                    }
                }
            } else {
                // Add it
                val newReaction = MessageReactionDto(
                    messageId = messageId,
                    userId = userId,
                    reactionEmoji = emoji
                )
                client.from("message_reactions").insert(newReaction)
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
