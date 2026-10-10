package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInterestsSkillsUseCase @Inject constructor(
    private val repository: EditProfileRepositoryImpl
) {
    suspend fun getCurrentUserId(): String? = repository.getCurrentUserId()

    operator fun invoke(userId: String): Flow<Result<UserProfile>> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return repository.getUserProfile(userId)
    }
}
