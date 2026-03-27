package com.synapse.social.studioasinc.domain.usecase.ai

import com.synapse.social.studioasinc.domain.repository.ai.AiRepository

class SummarizePostUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(postContent: String, comments: List<String>): Result<String> =
        aiRepository.summarizePost(postContent, comments)
}
