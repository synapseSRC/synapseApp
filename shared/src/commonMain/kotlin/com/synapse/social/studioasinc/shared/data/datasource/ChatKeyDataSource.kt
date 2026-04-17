package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantDto
import com.synapse.social.studioasinc.shared.data.dto.chat.UserPublicKeyDto
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ChatKeyDataSource(private val client: SupabaseClientLib) {

    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    /**
     * Looks up the other participant in a chat from the chat_participants table.
     * This is more reliable than parsing chatId strings.
     */
    suspend fun getOtherParticipantId(chatId: String, currentUserId: String): String? = withContext(AppDispatchers.IO) {
        try {
            val participants = client.postgrest.from("chat_participants")
                .select(columns = Columns.list("chat_id", "user_id")) {
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

    suspend fun getUserPublicKey(userId: String): UserPublicKeyDto? = withContext(AppDispatchers.IO) {
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

    suspend fun uploadUserPublicKey(publicKey: String) = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            val dto = UserPublicKeyDto(currentUserId, publicKey)
            client.postgrest.from("user_public_keys").upsert(dto)
            Unit
        } catch (e: Exception) {
            Napier.e("Error uploading public key", e)
            throw e
        }
    }
}
