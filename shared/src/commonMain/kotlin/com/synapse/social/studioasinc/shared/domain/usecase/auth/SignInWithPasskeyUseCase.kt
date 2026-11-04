package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyPlatformManager
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository

/**
 * Signs the user in with a passkey. The ceremony lives here (business logic): fetch request
 * options from the backend, run the platform WebAuthn assertion (discoverable, no email needed),
 * verify the signed credential, then import the returned session.
 */
class SignInWithPasskeyUseCase(
    private val authRepository: AuthRepository,
    private val passkeyRepository: PasskeyRepository,
    private val passkeyPlatformManager: PasskeyPlatformManager
) {

    suspend operator fun invoke(): Result<Unit> {
        if (!passkeyPlatformManager.isSupported) {
            return Result.failure(IllegalStateException("Passkeys are not supported on this device"))
        }

        return runCatching {
            val options = passkeyRepository.authenticationOptions().getOrThrow()
            val credentialJson = passkeyPlatformManager.assertCredential(options.optionsJson)
            val session = passkeyRepository.verifyAuthentication(
                challengeId = options.challengeId,
                credentialJson = credentialJson
            ).getOrThrow()
            authRepository.importSession(session.accessToken, session.refreshToken).getOrThrow()
        }
    }
}
