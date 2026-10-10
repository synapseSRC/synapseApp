package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository

class GetOAuthUrlUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(provider: String, redirectUrl: String): Result<String> {
        return repository.getOAuthUrl(provider, redirectUrl)
    }
}
