package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository

/**
 * Loads the passkeys registered for the currently signed-in user. The backend derives the user
 * from the active session, so no user id is required here.
 */
class LoadPasskeysUseCase(
    private val passkeyRepository: PasskeyRepository
) {

    suspend operator fun invoke(): Result<List<Passkey>> = runCatching {
        passkeyRepository.listPasskeys().getOrThrow()
    }
}
