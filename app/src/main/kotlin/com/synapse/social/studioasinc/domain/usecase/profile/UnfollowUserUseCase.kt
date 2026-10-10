package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.ProfileRepository
import javax.inject.Inject

class UnfollowUserUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(userId: String, targetUserId: String): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        if (targetUserId.isBlank()) return Result.failure(IllegalArgumentException("Target user ID cannot be blank"))
        if (userId == targetUserId) return Result.failure(IllegalArgumentException("Cannot unfollow yourself"))
        return repository.unfollowUser(userId, targetUserId)
    }
}
