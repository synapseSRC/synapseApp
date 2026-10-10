package com.synapse.social.studioasinc.shared.domain.usecase.user

import com.synapse.social.studioasinc.shared.domain.repository.UserRepository

class GetCurrentUserAvatarUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): Result<String?> = repository.getCurrentUserAvatar()
}
