package com.synapse.social.studioasinc.shared.data.crypto

import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalIdentityKeys
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalOneTimePreKey

class IosSignalProtocolManager : SignalProtocolManager {
    override suspend fun generateIdentityAndKeys(): SignalIdentityKeys {
        throw NotImplementedError("iOS Signal Protocol not yet implemented")
    }

    override suspend fun generateOneTimePreKeys(startId: Int, count: Int): List<SignalOneTimePreKey> {
         throw NotImplementedError("iOS Signal Protocol not yet implemented")
    }

    override suspend fun processPreKeyBundle(userId: String, bundle: PreKeyBundle) {
         throw NotImplementedError("iOS Signal Protocol not yet implemented")
    }

    override suspend fun hasSession(userId: String): Boolean {
        return false
    }

    override suspend fun deleteSession(userId: String) {
        throw NotImplementedError("iOS Signal Protocol not yet implemented")
    }

    override suspend fun deleteAllSessions() {
        // No-op for now
    }

    override suspend fun deleteIdentity(userId: String) {
        // No-op for now
    }

    override suspend fun encryptMessage(recipientId: String, message: ByteArray): EncryptedMessage {
         throw NotImplementedError("iOS Signal Protocol not yet implemented")
    }

    override suspend fun decryptMessage(senderId: String, message: EncryptedMessage): ByteArray {
         throw NotImplementedError("iOS Signal Protocol not yet implemented")
    }

    override suspend fun getLocalRegistrationId(): Int {
         throw NotImplementedError("iOS Signal Protocol not yet implemented")
    }

    override suspend fun getLocalIdentityKey(): String {
         throw NotImplementedError("iOS Signal Protocol not yet implemented")
    }

    override suspend fun checkKeyRotationNeeded(thresholdDays: Int): Boolean {
        return false
    }

    override suspend fun hasIdentity(): Boolean {
        return false
    }
}
