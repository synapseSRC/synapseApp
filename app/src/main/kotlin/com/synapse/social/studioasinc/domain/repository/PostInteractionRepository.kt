package com.synapse.social.studioasinc.domain.repository

interface PostInteractionRepository {
    suspend fun likePost(postId: String, userId: String): Result<Unit>
    suspend fun unlikePost(postId: String, userId: String): Result<Unit>
    suspend fun savePost(postId: String, userId: String): Result<Unit>
    suspend fun unsavePost(postId: String, userId: String): Result<Unit>
    suspend fun deletePost(postId: String, userId: String): Result<Unit>
    suspend fun reportPost(postId: String, userId: String, reason: String): Result<Unit>
}
