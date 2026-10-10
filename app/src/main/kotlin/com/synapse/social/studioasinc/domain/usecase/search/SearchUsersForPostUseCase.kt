package com.synapse.social.studioasinc.domain.usecase.search

import com.synapse.social.studioasinc.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.domain.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SearchUsersForPostUseCase @Inject constructor(
    private val userRepository: UserRepositoryImpl
) {
    operator fun invoke(query: String): Flow<Result<List<User>>> = flow {
        if (query.isBlank()) {
            emit(Result.success(emptyList()))
            return@flow
        }
        delay(300)

        val result = userRepository.searchUsers(query).map { profiles ->
            profiles.map { profile ->
                User(
                    uid = profile.uid,
                    username = profile.username,
                    displayName = profile.displayName,
                    avatar = profile.avatar,
                    verify = profile.verify
                )
            }
        }
        emit(result)
    }
}
