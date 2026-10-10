package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider

class SignInWithOAuthUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(provider: SocialProvider, redirectUrl: String): Result<Unit> {
        if (redirectUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Redirect URL cannot be empty"))
        }
        return repository.signInWithOAuth(provider, redirectUrl)
    }
}
