package com.synapse.social.studioasinc.shared.domain.usecase.ai

import com.synapse.social.studioasinc.shared.domain.repository.ai.AiRepository

class CheckMessageToxicityUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(text: String): Result<Boolean> {
        return aiRepository.isContentToxic(text)
    }
}
