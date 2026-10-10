package com.synapse.social.studioasinc.shared.domain.usecase.ai

import com.synapse.social.studioasinc.shared.domain.repository.ai.AiRepository

class GenerateSmartRepliesUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(recentMessages: List<String>, conversationId: String? = null): Result<List<String>> {
        return aiRepository.generateSmartReplies(recentMessages, conversationId)
    }
}
