package com.synapse.social.studioasinc.domain.usecase.ai

import com.synapse.social.studioasinc.domain.repository.ai.AiRepository

class GenerateSmartRepliesUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(recentMessages: List<String>): Result<List<String>> {
        return aiRepository.generateSmartReplies(recentMessages)
    }
}
