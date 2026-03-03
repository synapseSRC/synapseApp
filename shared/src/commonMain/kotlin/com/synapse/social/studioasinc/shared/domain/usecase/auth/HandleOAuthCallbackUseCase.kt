package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.model.OAuthDeepLink

class HandleOAuthCallbackUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(deepLink: OAuthDeepLink): Result<Unit> {
        if (deepLink.error != null) {
            return Result.failure(Exception("OAuth error: ${deepLink.errorDescription ?: deepLink.error}"))
        }
        val result = repository.handleOAuthCallback(deepLink.code, deepLink.accessToken, deepLink.refreshToken)

        if (result.isSuccess) {
            // Ensure profile exists after successful OAuth login
            val userId = repository.getCurrentUserId()
            val email = repository.getCurrentUserEmail()

            if (userId != null && email != null) {
                repository.ensureProfileExists(userId, email)
            }
        }
        return result
    }
}
