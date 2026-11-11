package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.datasource.ChatEffectsDataSource
import com.synapse.social.studioasinc.shared.data.mapper.ChatEffectMapper.toDomain
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.ChatEffectAnimation
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect
import com.synapse.social.studioasinc.shared.domain.repository.ChatEffectsRepository
import com.synapse.social.studioasinc.shared.util.Logger
import kotlinx.coroutines.withContext

/**
 * ChatEffectsRepository on Supabase.
 *
 * Thin on purpose: it owns the backend call and the Result boundary, and leaves validation and
 * matching to the domain.
 */
class SupabaseChatEffectsRepository(
    private val dataSource: ChatEffectsDataSource = ChatEffectsDataSource()
) : ChatEffectsRepository {

    override suspend fun getWordEffects(chatId: String): Result<List<WordEffect>> =
        withContext(AppDispatchers.IO) {
            try {
                val effects = dataSource.getWordEffects(chatId)
                    .map { it.toDomain() }
                    .sortedBy { it.trigger }
                Result.success(effects)
            } catch (e: Exception) {
                Logger.e("Failed to load word effects for " + chatId, TAG, e)
                Result.failure(e)
            }
        }

    override suspend fun createWordEffect(
        chatId: String,
        trigger: String,
        emoji: String,
        animation: ChatEffectAnimation
    ): Result<WordEffect> = withContext(AppDispatchers.IO) {
        try {
            val created = dataSource.createWordEffect(
                chatId = chatId,
                trigger = trigger,
                emoji = emoji,
                animation = animation.name
            )
            Result.success(created.toDomain())
        } catch (e: Exception) {
            Logger.e("Failed to create word effect for " + chatId, TAG, e)
            Result.failure(e)
        }
    }

    override suspend fun updateWordEffect(effect: WordEffect): Result<WordEffect> =
        withContext(AppDispatchers.IO) {
            try {
                val updated = dataSource.updateWordEffect(
                    effectId = effect.id,
                    chatId = effect.chatId,
                    trigger = effect.trigger,
                    emoji = effect.emoji,
                    animation = effect.animation.name,
                    isEnabled = effect.isEnabled
                )
                Result.success(updated.toDomain())
            } catch (e: Exception) {
                Logger.e("Failed to update word effect " + effect.id, TAG, e)
                Result.failure(e)
            }
        }

    override suspend fun deleteWordEffect(chatId: String, effectId: String): Result<Unit> =
        withContext(AppDispatchers.IO) {
            try {
                dataSource.deleteWordEffect(chatId, effectId)
                Result.success(Unit)
            } catch (e: Exception) {
                Logger.e("Failed to delete word effect " + effectId, TAG, e)
                Result.failure(e)
            }
        }

    private companion object {
        const val TAG = "ChatEffects"
    }
}
