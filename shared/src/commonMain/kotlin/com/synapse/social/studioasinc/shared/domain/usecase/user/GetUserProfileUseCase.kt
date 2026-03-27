package com.synapse.social.studioasinc.shared.domain.usecase.user

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository

class GetUserProfileUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(uid: String): Result<User?> {
        return repository.getUserProfile(uid)
    }
}
