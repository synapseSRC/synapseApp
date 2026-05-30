package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.domain.model.Story

interface StoryDao {
    suspend fun insert(story: Story)
    suspend fun insertAll(stories: List<Story>)
    suspend fun getAll(): List<Story>
    suspend fun deleteById(id: String)
    suspend fun deleteAll()
}
