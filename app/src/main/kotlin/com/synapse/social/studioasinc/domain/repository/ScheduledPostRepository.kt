package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.CreatePostRequest
import com.synapse.social.studioasinc.domain.model.ScheduledPost

interface ScheduledPostRepository {
    suspend fun getScheduledPosts(userId: String): Result<List<ScheduledPost>>
    suspend fun getDueScheduledPosts(userId: String): Result<List<ScheduledPost>>
    suspend fun createScheduledPost(userId: String, scheduledAt: String, request: CreatePostRequest): Result<ScheduledPost>
    suspend fun updateScheduledPost(id: String, scheduledAt: String, request: CreatePostRequest): Result<ScheduledPost>
    suspend fun markPublished(id: String, publishedPostId: String): Result<Unit>
    suspend fun markFailed(id: String, reason: String): Result<Unit>
    suspend fun cancelScheduledPost(id: String): Result<Unit>
    suspend fun retryScheduledPost(id: String): Result<Unit>
}
