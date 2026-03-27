package com.synapse.social.studioasinc.domain.usecase.ai

import com.synapse.social.studioasinc.domain.repository.ai.AiRepository

class SummarizeMessageUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(content: String): Result<String> = aiRepository.summarizeMessage(content)
}
