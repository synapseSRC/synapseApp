package com.synapse.social.studioasinc.shared.domain.usecase.story

import com.synapse.social.studioasinc.shared.domain.repository.StoryRepository

class CreateStoryUseCase(private val repository: StoryRepository) {
    @Throws(Exception::class)
    suspend operator fun invoke(
        mediaUrl: String,
        mediaType: String,
        textOverlay: String?
    ) {
        repository.createStory(
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            textOverlay = textOverlay
        )
    }
}
