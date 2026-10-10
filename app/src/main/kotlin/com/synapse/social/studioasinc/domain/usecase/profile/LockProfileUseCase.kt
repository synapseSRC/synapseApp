package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.domain.repository.ProfilePrivacyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LockProfileUseCase @Inject constructor(
    private val repository: ProfilePrivacyRepository
) {
    operator fun invoke(userId: String, isLocked: Boolean): Flow<Result<Unit>> = flow {
        emit(repository.lockProfile(userId, isLocked))
    }
}
