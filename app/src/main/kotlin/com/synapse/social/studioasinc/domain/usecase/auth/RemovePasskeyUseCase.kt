package com.synapse.social.studioasinc.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import javax.inject.Inject

class RemovePasskeyUseCase @Inject constructor(
    private val passkeyRepository: PasskeyRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            passkeyRepository.deletePasskey(id)
        } catch (e: Exception) {
            Result.failure(Exception("Error removing passkey: ${e.message}"))
        }
    }
}
