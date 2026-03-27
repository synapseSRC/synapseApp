package com.synapse.social.studioasinc.domain.usecase.auth

import com.synapse.social.studioasinc.domain.repository.DeviceInfoProvider
import com.synapse.social.studioasinc.domain.repository.PasskeyManager
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GeneratePasskeyRequestJsonUseCase
import javax.inject.Inject

class AddPasskeyUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val passkeyRepository: PasskeyRepository,
    private val generatePasskeyRequestJsonUseCase: GeneratePasskeyRequestJsonUseCase,
    private val passkeyManager: PasskeyManager,
    private val deviceInfoProvider: DeviceInfoProvider
) {
    suspend operator fun invoke(): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
        val userEmail = authRepository.getCurrentUserEmail() ?: "user"

        if (userId == null) {
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            val challengeResult = passkeyRepository.generatePasskeyChallenge()
            val challenge = challengeResult.getOrElse {
                return Result.failure(Exception("Failed to get challenge: ${it.message}"))
            }

            val requestJson = generatePasskeyRequestJsonUseCase(
                challenge = challenge.toString(),
                userId = userId,
                userEmail = userEmail
            )

            val responseJson = passkeyManager.createPasskey(requestJson)
            if (responseJson != null) {
                passkeyRepository.registerPasskey(responseJson, deviceInfoProvider.getDeviceModel())
            } else {
                Result.failure(Exception("Unexpected response from credential manager"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error adding passkey: ${e.message}"))
        }
    }
}
