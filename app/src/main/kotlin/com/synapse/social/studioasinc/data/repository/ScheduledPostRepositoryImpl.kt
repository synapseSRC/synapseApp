package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.CreatePostRequest
import com.synapse.social.studioasinc.domain.model.ScheduledPost
import com.synapse.social.studioasinc.domain.repository.ScheduledPostRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledPostRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : ScheduledPostRepository {

    override suspend fun getScheduledPosts(userId: String): Result<List<ScheduledPost>> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("scheduled_posts")
                .select {
                    filter {
                        eq("owner_uid", userId)
                        neq("status", ScheduledPost.STATUS_PUBLISHED)
                    }
                    order("scheduled_at", Order.ASCENDING)
                }
                .decodeList<ScheduledPostDto>()
                .map { it.toDomain() }
        }
    }

    override suspend fun getDueScheduledPosts(userId: String): Result<List<ScheduledPost>> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("scheduled_posts")
                .select {
                    filter {
                        eq("owner_uid", userId)
                        eq("status", ScheduledPost.STATUS_PENDING)
                        lte("scheduled_at", Clock.System.now().toString())
                    }
                    order("scheduled_at", Order.ASCENDING)
                }
                .decodeList<ScheduledPostDto>()
                .map { it.toDomain() }
        }
    }

    override suspend fun createScheduledPost(
        userId: String,
        scheduledAt: String,
        request: CreatePostRequest
    ): Result<ScheduledPost> = withContext(Dispatchers.IO) {
        runCatching {
            val dto = ScheduledPostDto(
                ownerUid = userId,
                payload = request.toDto(),
                scheduledAt = scheduledAt,
                status = ScheduledPost.STATUS_PENDING
            )

            client.from("scheduled_posts")
                .insert(dto) {
                    select()
                }
                .decodeSingle<ScheduledPostDto>()
                .toDomain()
        }
    }

    override suspend fun updateScheduledPost(
        id: String,
        scheduledAt: String,
        request: CreatePostRequest
    ): Result<ScheduledPost> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("scheduled_posts")
                .update({
                    set("payload", request.toDto())
                    set("scheduled_at", scheduledAt)
                    set("status", ScheduledPost.STATUS_PENDING)
                    set("last_error", null as String?)
                    set("published_post_id", null as String?)
                    set("published_at", null as String?)
                }) {
                    filter {
                        eq("id", id)
                    }
                    select()
                }
                .decodeSingle<ScheduledPostDto>()
                .toDomain()
        }
    }

    override suspend fun markPublished(id: String, publishedPostId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("scheduled_posts")
                .update({
                    set("status", ScheduledPost.STATUS_PUBLISHED)
                    set("published_post_id", publishedPostId)
                    set("published_at", Clock.System.now().toString())
                    set("last_error", null as String?)
                }) {
                    filter {
                        eq("id", id)
                    }
                }
            Unit
        }
    }

    override suspend fun markFailed(id: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("scheduled_posts")
                .update({
                    set("status", ScheduledPost.STATUS_FAILED)
                    set("last_error", reason)
                }) {
                    filter {
                        eq("id", id)
                    }
                }
            Unit
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
                    set("status", ScheduledPost.STATUS_PENDING)
                    set("last_error", null as String?)
                }) {
                    filter {
                        eq("id", id)
                    }
                }
            Unit
        }
    }
}
