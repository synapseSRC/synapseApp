package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.domain.repository.ProfilePrivacyRepository
import com.synapse.social.studioasinc.domain.model.ProfileFollowRequest
import javax.inject.Inject

class GetPendingFollowRequestsUseCase @Inject constructor(
    private val repository: ProfilePrivacyRepository
) {
    suspend operator fun invoke(): Result<List<ProfileFollowRequest>> =
        repository.getPendingFollowRequests()
}
