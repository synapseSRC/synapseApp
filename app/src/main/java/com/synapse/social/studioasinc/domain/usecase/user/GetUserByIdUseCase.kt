package com.synapse.social.studioasinc.domain.usecase.user

import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.domain.model.User
import javax.inject.Inject

class GetUserByIdUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<User?> {
        return repository.getUserById(userId)
    }
}
