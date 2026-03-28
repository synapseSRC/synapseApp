package com.synapse.social.studioasinc.domain.usecase.reaction

import com.synapse.social.studioasinc.data.repository.ReactionRepositoryImpl
import com.synapse.social.studioasinc.domain.model.ReactionType
import javax.inject.Inject

class ToggleMessageReactionUseCase @Inject constructor(
    private val repository: ReactionRepositoryImpl
) {
    suspend operator fun invoke(
        messageId: String,
        reactionType: ReactionType,
        oldReaction: ReactionType? = null
    ): Result<com.synapse.social.studioasinc.domain.repository.ReactionToggleResult> {
        return repository.toggleMessageReaction(messageId, reactionType, oldReaction)
    }
}
