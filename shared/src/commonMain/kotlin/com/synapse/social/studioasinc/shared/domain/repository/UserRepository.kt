package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.User

interface UserRepository {
    suspend fun isUsernameAvailable(username: String): Result<Boolean>
    suspend fun getUserProfile(uid: String): Result<User?>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Result<Boolean>
    suspend fun getCurrentUserAvatar(): Result<String?>
}
