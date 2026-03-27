package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey

interface PasskeyRepository {
    suspend fun getPasskeys(userId: String): Result<List<Passkey>>
    suspend fun deletePasskey(id: String): Result<Unit>
    suspend fun generatePasskeyChallenge(): Result<String>
    suspend fun registerPasskey(registrationResponseJson: String, deviceName: String): Result<Unit>
}
