package com.synapse.social.studioasinc.domain.usecase.ai

import com.synapse.social.studioasinc.domain.repository.ai.AiRepository

class SummarizeChatUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(messages: List<String>): Result<String> {
        return aiRepository.summarizeChat(messages)
    }
}
