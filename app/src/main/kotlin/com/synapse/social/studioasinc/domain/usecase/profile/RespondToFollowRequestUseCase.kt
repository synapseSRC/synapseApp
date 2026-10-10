package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.domain.repository.ProfilePrivacyRepository
import javax.inject.Inject

class RespondToFollowRequestUseCase @Inject constructor(
    private val repository: ProfilePrivacyRepository
) {
    suspend operator fun invoke(requesterUid: String, accept: Boolean): Result<Unit> =
        repository.respondToFollowRequest(requesterUid, accept)
}
