package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository

/**
 * Removes a registered passkey by id.
 */
class RemovePasskeyUseCase(
    private val passkeyRepository: PasskeyRepository
) {

    suspend operator fun invoke(id: String): Result<Unit> = runCatching {
        passkeyRepository.deletePasskey(id).getOrThrow()
    }
}
