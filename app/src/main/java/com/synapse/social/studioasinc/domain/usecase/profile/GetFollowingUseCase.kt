package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.data.repository.ProfileRepository
import javax.inject.Inject

class GetFollowingUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(userId: String, limit: Int = 20, offset: Int = 0): Result<List<UserProfile>> {
        return repository.getFollowing(userId, limit, offset)
    }
}
