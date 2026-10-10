package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository

/**
 * Use case for signing in with Google ID token.
 * This is used for native Google Sign-In flow on Android.
 */
class SignInWithGoogleIdTokenUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<String> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("ID token cannot be empty"))
        }
        return repository.signInWithGoogleIdToken(idToken)
    }
}
