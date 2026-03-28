package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.ProfileRepository
import javax.inject.Inject

class GetProfilePostsUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(userId: String, limit: Int = 10, offset: Int = 0): Result<List<Any>> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(limit > 0) { "Limit must be positive" }
        require(offset >= 0) { "Offset cannot be negative" }
        return repository.getProfilePosts(userId, limit, offset)
    }
}
