package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.PasskeyPlatformManager
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository

/**
 * Registers a new passkey for the currently signed-in user. The ceremony lives here (business
 * logic): fetch creation options from the backend, run the platform WebAuthn prompt, then verify
 * the signed credential with the backend. The user is identified by the active session.
 */
class AddPasskeyUseCase(
    private val passkeyRepository: PasskeyRepository,
    private val passkeyPlatformManager: PasskeyPlatformManager
) {

    suspend operator fun invoke(deviceName: String): Result<Unit> {
        if (!passkeyPlatformManager.isSupported) {
            return Result.failure(IllegalStateException("Passkeys are not supported on this device"))
        }

        return runCatching {
            val options = passkeyRepository.registrationOptions(deviceName).getOrThrow()
            val credentialJson = passkeyPlatformManager.createCredential(options.optionsJson)
            passkeyRepository.verifyRegistration(
                challengeId = options.challengeId,
                credentialJson = credentialJson
            ).getOrThrow()
        }
    }
}
