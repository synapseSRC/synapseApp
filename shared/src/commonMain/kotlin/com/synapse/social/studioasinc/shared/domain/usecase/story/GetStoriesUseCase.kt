package com.synapse.social.studioasinc.shared.domain.usecase.story

import com.synapse.social.studioasinc.shared.domain.model.Story
import com.synapse.social.studioasinc.shared.domain.repository.StoryRepository

class GetStoriesUseCase(private val repository: StoryRepository) {
    @Throws(Exception::class)
    suspend operator fun invoke(): List<Story> {
        return repository.getStories()
    }
}
