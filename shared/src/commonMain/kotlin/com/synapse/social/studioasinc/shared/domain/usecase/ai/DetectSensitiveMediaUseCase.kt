package com.synapse.social.studioasinc.shared.domain.usecase.ai

import com.synapse.social.studioasinc.shared.domain.repository.ai.AiRepository

class DetectSensitiveMediaUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(mediaPath: String): Result<Boolean> {
        return aiRepository.detectSensitiveContent(mediaPath)
    }
}
