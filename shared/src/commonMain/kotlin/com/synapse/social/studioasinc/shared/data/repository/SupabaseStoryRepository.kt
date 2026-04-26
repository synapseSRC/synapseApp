package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.domain.model.Story
import com.synapse.social.studioasinc.shared.data.model.StoryDto
import com.synapse.social.studioasinc.shared.data.model.toDomain
import com.synapse.social.studioasinc.shared.domain.repository.StoryRepository
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock

class SupabaseStoryRepository : StoryRepository {
    private val client = SupabaseClient.client
    private val TAG = "SupabaseStoryRepository"

    override suspend fun getStories(): List<Story> {
        return try {
            val storiesDto = client.from("stories")
                .select() {
                    order(column = "created_at", order = Order.DESCENDING)
                }.decodeList<StoryDto>()

            storiesDto.map { it.toDomain() }
        } catch (e: Exception) {
            Napier.e("Failed to fetch stories", e, tag = TAG)
            throw e
        }
    }

    override suspend fun createStory(
        mediaUrl: String,
        mediaType: String,
        textOverlay: String?
    ) {
        try {
            val currentUser = client.auth.currentUserOrNull() ?: throw Exception("Not logged in")

            val storyData = mapOf(
                "user_id" to currentUser.id,
                "media_url" to mediaUrl,
                "media_type" to mediaType,
                "content" to textOverlay,
                "created_at" to Clock.System.now().toString()
            )

            client.from("stories").insert(storyData)
        } catch (e: Exception) {
            Napier.e("Failed to create story: ${e::class.simpleName}: ${e.message}", e, tag = TAG)
            throw Exception("Story creation failed: ${e.message ?: e::class.simpleName}", e)
        }
    }
}
