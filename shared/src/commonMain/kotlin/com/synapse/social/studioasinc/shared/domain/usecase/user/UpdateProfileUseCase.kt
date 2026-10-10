package com.synapse.social.studioasinc.shared.domain.usecase.user

import com.synapse.social.studioasinc.shared.domain.repository.UserRepository

class UpdateProfileUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(uid: String, updates: Map<String, Any?>): Result<Boolean> {
        return repository.updateUserProfile(uid, updates)
    }
}
