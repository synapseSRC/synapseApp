package com.synapse.social.studioasinc.domain.usecase.auth

import android.content.Context
import android.os.Build
import androidx.credentials.exceptions.CreateCredentialException
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GeneratePasskeyRequestJsonUseCase
import com.synapse.social.studioasinc.ui.settings.PasskeyCredentialManager
import javax.inject.Inject

class AddPasskeyUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val passkeyRepository: PasskeyRepository,
    private val generatePasskeyRequestJsonUseCase: GeneratePasskeyRequestJsonUseCase,
    private val passkeyCredentialManager: PasskeyCredentialManager
) {
    suspend operator fun invoke(activityContext: Context): Result<Unit> {
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

            try {
                val response = passkeyCredentialManager.createPasskey(activityContext, requestJson)
                if (response != null) {
                    passkeyRepository.registerPasskey(response.registrationResponseJson, Build.MODEL)
                } else {
                    Result.failure(Exception("Unexpected response from credential manager"))
                }
            } catch (e: CreateCredentialException) {
                Result.failure(Exception("Passkey creation failed: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error adding passkey: ${e.message}"))
        }
    }
}
