package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository

class GetCurrentUserEmailUseCase(private val repository: AuthRepository) {
    operator fun invoke(): String? {
        return repository.getCurrentUserEmail()
    }
}
