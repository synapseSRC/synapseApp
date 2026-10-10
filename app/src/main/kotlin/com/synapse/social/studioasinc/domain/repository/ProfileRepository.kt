package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfile(userId: String, refresh: Boolean = false): Flow<Result<UserProfile>>
    suspend fun updateProfile(userId: String, profile: UserProfile): Result<UserProfile>
    suspend fun followUser(userId: String, targetUserId: String): Result<Unit>
    suspend fun unfollowUser(userId: String, targetUserId: String): Result<Unit>
    suspend fun getFollowers(userId: String, limit: Int = 20, offset: Int = 0): Result<List<UserProfile>>
    suspend fun getFollowing(userId: String, limit: Int = 20, offset: Int = 0): Result<List<UserProfile>>
    suspend fun getProfilePosts(userId: String, limit: Int = 10, offset: Int = 0): Result<List<Post>>
    suspend fun getProfilePhotos(userId: String, limit: Int = 20, offset: Int = 0): Result<List<MediaItem>>
    suspend fun getProfileReels(userId: String, limit: Int = 20, offset: Int = 0): Result<List<MediaItem>>
    suspend fun getProfileReplies(userId: String, limit: Int = 10, offset: Int = 0): Result<List<CommentWithUser>>
    suspend fun isFollowing(userId: String, targetUserId: String): Result<Boolean>
}
