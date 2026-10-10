package com.synapse.social.studioasinc.shared.domain.usecase.story

import com.synapse.social.studioasinc.shared.domain.repository.StoryRepository

class DeleteStoryUseCase(private val repository: StoryRepository) {
    @Throws(Exception::class)
    suspend operator fun invoke(storyId: String) {
        repository.deleteStory(storyId)
    }
}
