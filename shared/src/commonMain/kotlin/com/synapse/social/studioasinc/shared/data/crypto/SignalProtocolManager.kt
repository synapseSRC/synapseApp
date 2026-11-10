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

    // ─── Identity verification (safety numbers) ──────────────────────────────
    // Local, synchronous trust state: the safety number the user verified for a peer, and a
    // detected identity-key change that has not been accepted yet.
    fun verifiedSafetyNumber(peerId: String): String?
    fun setVerifiedSafetyNumber(peerId: String, safetyNumber: String)
    fun clearVerifiedSafetyNumber(peerId: String)
    fun pendingIdentityChange(peerId: String): PendingIdentityChange?

    /** Advances the stored key to the pending one, drops the verification and the stale session. */
    fun acceptIdentityChange(peerId: String): Boolean
}
