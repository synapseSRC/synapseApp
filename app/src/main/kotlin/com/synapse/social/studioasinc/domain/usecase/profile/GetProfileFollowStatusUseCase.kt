package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.domain.repository.ProfilePrivacyRepository
import com.synapse.social.studioasinc.domain.model.ProfileFollowStatus
import javax.inject.Inject

class GetProfileFollowStatusUseCase @Inject constructor(
    private val repository: ProfilePrivacyRepository
) {
    suspend operator fun invoke(targetUid: String): Result<ProfileFollowStatus> =
        repository.getFollowStatus(targetUid)
}
