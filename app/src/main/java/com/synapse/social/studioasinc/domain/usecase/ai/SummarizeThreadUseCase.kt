package com.synapse.social.studioasinc.domain.usecase.ai

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.repository.ai.AiRepository

class SummarizeThreadUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(posts: List<Post>): Result<String> {
        return aiRepository.summarizeThread(posts)
    }
}
