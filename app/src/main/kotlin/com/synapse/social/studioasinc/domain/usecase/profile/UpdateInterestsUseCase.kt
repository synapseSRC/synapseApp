package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import javax.inject.Inject

class UpdateInterestsUseCase @Inject constructor(
    private val repository: EditProfileRepositoryImpl
) {
    suspend operator fun invoke(userId: String, interests: List<String>): Result<Unit> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        val sanitizedInterests = interests.map { it.trim() }.filter { it.isNotBlank() }
        val updateData = mapOf("interests" to sanitizedInterests)
        return repository.updateProfile(userId, updateData)
    }
}
