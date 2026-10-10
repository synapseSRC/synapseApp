package com.synapse.social.studioasinc.shared.domain.usecase.user

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository

class SearchUsersUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(query: String): Result<List<User>> {
        return repository.searchUsers(query)
    }
}
