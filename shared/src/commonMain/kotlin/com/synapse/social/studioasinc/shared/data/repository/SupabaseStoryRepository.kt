package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.local.database.StoryDao
import com.synapse.social.studioasinc.shared.data.model.StoryDto
import com.synapse.social.studioasinc.shared.data.model.toDomain
import com.synapse.social.studioasinc.shared.domain.model.Story
import com.synapse.social.studioasinc.shared.domain.repository.StoryRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.aakira.napier.Napier
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class SupabaseStoryRepository(
    private val storyDao: StoryDao? = null
) : StoryRepository {
    private val client = SupabaseClient.client
    private val TAG = "SupabaseStoryRepository"

    override suspend fun getStories(): List<Story> = withContext(AppDispatchers.IO) {
        try {
            // Priority: Network fetch with local sync for offline support
            val storiesDto = client.from("stories")
                .select() {
                    order(column = "created_at", order = Order.DESCENDING)
                }.decodeList<StoryDto>()

            val stories = storiesDto.map { it.toDomain() }

            // Sync with local cache
            storyDao?.let { dao ->
                dao.deleteAll()
                dao.insertAll(stories)
            }

            stories
        } catch (e: Exception) {
            Napier.e("Failed to fetch stories, falling back to local cache", e, tag = TAG)
            // Fallback to local cache if network fails
            storyDao?.getAll() ?: throw e
        }
    }

    override suspend fun createStory(
        mediaUrl: String,
        mediaType: String,
        textOverlay: String?
    ): Unit = withContext(AppDispatchers.IO) {
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
            // Note: We don't manually insert into local cache here as getStories() will sync it on next call
            // or we could trigger a refresh.
        } catch (e: Exception) {
            Napier.e("Failed to create story: ${e::class.simpleName}: ${e.message}", e, tag = TAG)
            throw Exception("Story creation failed: ${e.message ?: e::class.simpleName}", e)
        }
    }

    override suspend fun markAsSeen(storyId: String, viewerId: String): Unit = withContext(AppDispatchers.IO) {
        try {
            val view = mapOf(
                "story_id" to storyId,
                "viewer_id" to viewerId
            )
            client.from("story_views").upsert(view) {
                onConflict = "story_id,viewer_id"
                ignoreDuplicates = true
            }
        } catch (e: Exception) {
            Napier.e("Failed to mark story as seen", e, tag = TAG)
            throw e
        }
    }

    override suspend fun deleteStory(storyId: String): Unit = withContext(AppDispatchers.IO) {
        try {
            client.from("stories").delete {
                filter {
                    eq("id", storyId)
                }
            }
            storyDao?.deleteById(storyId)
        } catch (e: Exception) {
            Napier.e("Failed to delete story", e, tag = TAG)
            throw e
        }
    }
}
