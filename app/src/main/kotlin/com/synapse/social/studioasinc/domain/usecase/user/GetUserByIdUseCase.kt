package com.synapse.social.studioasinc.domain.usecase.user

import com.synapse.social.studioasinc.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.domain.model.User
import javax.inject.Inject

class GetUserByIdUseCase @Inject constructor(
    private val repository: UserRepositoryImpl
) {
    suspend operator fun invoke(userId: String): Result<User?> {
        return repository.getUserById(userId)
    }
}
