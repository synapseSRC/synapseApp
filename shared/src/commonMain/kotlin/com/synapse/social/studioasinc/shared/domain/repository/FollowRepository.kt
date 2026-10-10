package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.User

interface FollowRepository {
    suspend fun getFollowers(userId: String): Result<List<User>>
    suspend fun getFollowing(userId: String): Result<List<User>>
}
