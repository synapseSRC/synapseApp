package com.synapse.social.studioasinc.shared.domain.usecase.ai

import com.synapse.social.studioasinc.shared.domain.repository.ai.AiRepository

class SummarizeChatUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(messages: List<String>): Result<String> {
        return aiRepository.summarizeChat(messages)
    }
}
