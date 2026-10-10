package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository

class IsEmailVerifiedUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Boolean {
        return repository.isEmailVerified()
    }
}
