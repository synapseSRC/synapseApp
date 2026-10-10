package com.synapse.social.studioasinc.shared.domain.usecase.story

import com.synapse.social.studioasinc.shared.domain.repository.StoryRepository

class MarkStoryAsSeenUseCase(private val repository: StoryRepository) {
    @Throws(Exception::class)
    suspend operator fun invoke(storyId: String, viewerId: String) {
        repository.markAsSeen(storyId, viewerId)
    }
}
