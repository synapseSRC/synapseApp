package com.synapse.social.studioasinc.shared.domain.usecase.follow

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.FollowRepository

class GetFollowingUseCase(
    private val followRepository: FollowRepository
) {
    suspend operator fun invoke(userId: String): Result<List<User>> {
        return followRepository.getFollowing(userId)
    }
}
