package com.synapse.social.studioasinc.shared.data.crypto

import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalIdentityKeys
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalOneTimePreKey

interface SignalProtocolManager {
    suspend fun generateIdentityAndKeys(): SignalIdentityKeys
    suspend fun generateOneTimePreKeys(startId: Int, count: Int): List<SignalOneTimePreKey>

    suspend fun processPreKeyBundle(userId: String, bundle: PreKeyBundle)

    suspend fun hasSession(userId: String): Boolean
    suspend fun deleteSession(userId: String)
    suspend fun deleteAllSessions()
    suspend fun deleteIdentity(userId: String)
    suspend fun encryptMessage(recipientId: String, message: ByteArray): EncryptedMessage
    suspend fun decryptMessage(senderId: String, message: EncryptedMessage): ByteArray

    suspend fun getLocalRegistrationId(): Int
    suspend fun getLocalIdentityKey(): String
    suspend fun checkKeyRotationNeeded(thresholdDays: Int = 30): Boolean
    suspend fun hasIdentity(): Boolean
}
