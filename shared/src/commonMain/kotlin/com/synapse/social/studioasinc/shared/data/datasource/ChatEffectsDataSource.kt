package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.dto.chat.effects.WordEffectDto
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.withContext

/**
 * PostgREST access for public.chat_word_effects.
 *
 * Throws on failure, the way the rest of the chat data sources do; SupabaseChatEffectsRepository is
 * the layer that turns a throw into a Result.
 */
class ChatEffectsDataSource(private val client: SupabaseClientLib = SupabaseClient.client) {

    private fun getCurrentUserId(): String? = runCatching {
        client.auth.currentUserOrNull()?.id
    }.getOrNull()

    suspend fun getWordEffects(chatId: String): List<WordEffectDto> = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from(TABLE)
                .select { filter { eq("chat_id", chatId) } }
                .decodeList<WordEffectDto>()
        } catch (e: Exception) {
            Napier.e("Error loading word effects for " + chatId, e, tag = TAG)
            throw e
        }
    }

    suspend fun getWordEffect(chatId: String, effectId: String): WordEffectDto? =
        withContext(AppDispatchers.IO) {
            try {
                client.postgrest.from(TABLE)
                    .select {
                        filter {
                            eq("chat_id", chatId)
                            eq("id", effectId)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<WordEffectDto>()
            } catch (e: Exception) {
                Napier.e("Error loading word effect " + effectId, e, tag = TAG)
                throw e
            }
        }

    suspend fun createWordEffect(
        chatId: String,
        trigger: String,
        emoji: String,
        animation: String
    ): WordEffectDto = withContext(AppDispatchers.IO) {
        val userId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
        try {
            client.postgrest.from(TABLE).insert(
                WordEffectDto(
                    chatId = chatId,
                    trigger = trigger,
                    emoji = emoji,
                    animation = animation,
                    createdBy = userId
                )
            )
            // The insert does not return the row, so the stored row is read back: the caller gets
            // exactly what the backend kept, including the id and the timestamps.
            getWordEffectByTrigger(chatId, trigger)
                ?: throw IllegalStateException("Word effect was not created")
        } catch (e: Exception) {
            Napier.e("Error creating word effect for " + chatId, e, tag = TAG)
            throw e
        }
    }

    suspend fun updateWordEffect(
        effectId: String,
        chatId: String,
        trigger: String,
        emoji: String,
        animation: String,
        isEnabled: Boolean
    ): WordEffectDto = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from(TABLE).update({
                set("trigger", trigger)
                set("emoji", emoji)
                set("animation", animation)
                set("is_enabled", isEnabled)
            }) {
                filter {
                    eq("id", effectId)
                    eq("chat_id", chatId)
                }
            }
            getWordEffect(chatId, effectId)
                ?: throw IllegalStateException("Word effect was not updated")
        } catch (e: Exception) {
            Napier.e("Error updating word effect " + effectId, e, tag = TAG)
            throw e
        }
    }

    suspend fun deleteWordEffect(chatId: String, effectId: String) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from(TABLE).delete {
                filter {
                    eq("id", effectId)
                    eq("chat_id", chatId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error deleting word effect " + effectId, e, tag = TAG)
            throw e
        }
    }

    private suspend fun getWordEffectByTrigger(chatId: String, trigger: String): WordEffectDto? =
        withContext(AppDispatchers.IO) {
            client.postgrest.from(TABLE)
                .select {
                    filter {
                        eq("chat_id", chatId)
                        eq("trigger", trigger)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<WordEffectDto>()
        }

    private companion object {
        const val TABLE = "chat_word_effects"
        const val TAG = "ChatEffects"
    }
}
