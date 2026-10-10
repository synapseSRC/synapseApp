package com.synapse.social.studioasinc.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import javax.inject.Inject

class LoadPasskeysUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val passkeyRepository: PasskeyRepository
) {
    suspend operator fun invoke(): Result<List<Passkey>> {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            return Result.success(emptyList())
        }

        return try {
            passkeyRepository.getPasskeys(userId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to load passkeys: ${e.message}"))
        }
    }
}
