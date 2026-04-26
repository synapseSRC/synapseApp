package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.Story

interface StoryRepository {
    suspend fun getStories(): List<Story>
    suspend fun createStory(
        mediaUrl: String,
        mediaType: String,
        textOverlay: String?
    )
}
