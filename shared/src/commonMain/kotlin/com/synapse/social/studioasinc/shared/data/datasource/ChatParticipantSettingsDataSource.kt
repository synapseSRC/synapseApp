package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantSettingsDto
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.withContext

/**
 * Data source for managing cross-device conversation participant settings.
 * Handles synchronization of pinned, muted, and archived status across devices.
 */
internal class ChatParticipantSettingsDataSource(private val client: SupabaseClientLib) {

    private fun getCurrentUserId(): String? = runCatching {
        client.pluginManager.getPluginOrNull(io.github.jan.supabase.auth.Auth)?.currentUserOrNull()?.id
    }.getOrNull()

    /**
     * Retrieves settings for a specific chat and user.
     *
     * @param chatId The chat ID
     * @return The participant settings, or null if not found
     */
    suspend fun getChatSettings(chatId: String): Result<ChatParticipantSettingsDto?> = withContext(AppDispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")

            val settings = client.postgrest.from("chat_participant_settings")
                .select {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<ChatParticipantSettingsDto>()

            Result.success(settings)
        } catch (e: Exception) {
            Napier.e("Error retrieving chat settings for $chatId", e, tag = "ChatSettings")
            Result.failure(e)
        }
    }

    /**
     * Updates settings for a specific chat.
     * Implements optimistic concurrency control using sync_version.
     *
     * @param chatId The chat ID
     * @param isPinned Whether the chat is pinned
     * @param isMuted Whether the chat is muted
     * @param isArchived Whether the chat is archived
     * @param currentVersion The current sync_version for conflict detection
     * @return Result indicating success or failure
     */
    suspend fun updateChatSettings(
        chatId: String,
        isPinned: Boolean? = null,
        isMuted: Boolean? = null,
        isArchived: Boolean? = null,
        currentVersion: Int? = null
    ): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")

            // If version is provided, check for conflicts
            if (currentVersion != null) {
                val existing = client.postgrest.from("chat_participant_settings")
                    .select {
                        filter {
                            eq("chat_id", chatId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingleOrNull<ChatParticipantSettingsDto>()

                if (existing != null && existing.syncVersion > currentVersion) {
                    Napier.w("Conflict detected when updating settings for $chatId", tag = "ChatSettings")
                    return@withContext Result.failure(Exception("Settings were modified on another device"))
                }
            }

            client.postgrest.from("chat_participant_settings")
                .upsert(
                    ChatParticipantSettingsDto(
                        chatId = chatId,
                        userId = userId,
                        isPinned = isPinned ?: false,
                        isMuted = isMuted ?: false,
                        isArchived = isArchived ?: false,
                        syncVersion = (currentVersion ?: 0) + 1
                    )
                ) {
                    onConflict = "chat_id,user_id"
                }

            Napier.d("Updated chat settings for $chatId", tag = "ChatSettings")
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error updating chat settings for $chatId", e, tag = "ChatSettings")
            Result.failure(e)
        }
    }

    /**
     * Retrieves all settings for the current user.
     *
     * @return List of all chat settings for the user
     */
    suspend fun getAllUserSettings(): Result<List<ChatParticipantSettingsDto>> = withContext(AppDispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")

            val settings = client.postgrest.from("chat_participant_settings")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<ChatParticipantSettingsDto>()

            Napier.d("Retrieved ${settings.size} chat settings for user", tag = "ChatSettings")
            Result.success(settings)
        } catch (e: Exception) {
            Napier.e("Error retrieving all user chat settings", e, tag = "ChatSettings")
            Result.failure(e)
        }
    }
}
