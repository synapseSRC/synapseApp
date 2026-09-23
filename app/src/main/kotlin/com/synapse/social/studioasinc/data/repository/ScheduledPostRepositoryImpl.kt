package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.CreatePostRequest
import com.synapse.social.studioasinc.domain.model.ScheduledPost
import com.synapse.social.studioasinc.domain.repository.ScheduledPostRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledPostRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : ScheduledPostRepository {

    override suspend fun getScheduledPosts(userId: String): Result<List<ScheduledPost>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.from("scheduled_posts")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<ScheduledPostDto>()

            response.map { it.toDomain() }
        }
    }

    override suspend fun createScheduledPost(
        userId: String,
        scheduledAt: String,
        request: CreatePostRequest
    ): Result<ScheduledPost> = withContext(Dispatchers.IO) {
        runCatching {
            val dto = ScheduledPostDto(
                userId = userId,
                postData = request.toDto(),
                scheduledAt = scheduledAt,
                status = ScheduledPost.STATUS_SCHEDULED
            )

            val inserted = client.from("scheduled_posts")
                .insert(dto) {
                    select()
                }
                .decodeSingle<ScheduledPostDto>()

            inserted.toDomain()
        }
    }

    override suspend fun updateScheduledPost(
        id: String,
        scheduledAt: String,
        request: CreatePostRequest
    ): Result<ScheduledPost> = withContext(Dispatchers.IO) {
        runCatching {
            val updated = client.from("scheduled_posts")
                .update({
                    set("post_data", request.toDto())
                    set("scheduled_at", scheduledAt)
                    set("status", ScheduledPost.STATUS_SCHEDULED)
                    set("error_message", null as String?)
                }) {
                    filter {
                        eq("id", id)
                    }
                    select()
                }
                .decodeSingle<ScheduledPostDto>()

            updated.toDomain()
        }
    }

    override suspend fun cancelScheduledPost(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("scheduled_posts")
                .delete {
                    filter {
                        eq("id", id)
                    }
                }
            Unit
        }
    }

    override suspend fun retryScheduledPost(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("scheduled_posts")
                .update({
                    set("status", ScheduledPost.STATUS_SCHEDULED)
                    set("error_message", null as String?)
                }) {
                    filter {
                        eq("id", id)
                    }
                }
            Unit
        }
    }
}
