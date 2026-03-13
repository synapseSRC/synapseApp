package com.synapse.social.studioasinc.shared.data.crypto

import android.content.Context
import android.util.Base64
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalIdentityKeys
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalOneTimePreKey
import com.synapse.social.studioasinc.shared.util.Logger
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper
import org.whispersystems.libsignal.state.PreKeyBundle as SignalNativePreKeyBundle

class AndroidSignalProtocolManager(context: Context) : SignalProtocolManager {

    private val store = AndroidSignalProtocolStore(context)
    private val DEVICE_ID = 1

    override suspend fun generateIdentityAndKeys(): SignalIdentityKeys {
        val identityKeyPair: IdentityKeyPair
        val registrationId: Int

        if (store.hasIdentity()) {
            // Reuse existing identity — only refresh the signed pre-key
            Logger.d("E2EE_INIT: Reusing existing identity key pair", tag = "E2EE")
            identityKeyPair = store.identityKeyPair
            registrationId = store.getLocalRegistrationId()
        } else {
            // Fresh generation
            Logger.d("E2EE_INIT: Generating new identity key pair", tag = "E2EE")
            identityKeyPair = KeyHelper.generateIdentityKeyPair()
            registrationId = KeyHelper.generateRegistrationId(false)
            store.storeLocalIdentity(identityKeyPair, registrationId)
        }

        // Generate a signed pre-key. Using an ID derived from current time to avoid overwriting issues
        // while staying within the expected Int range for Signal protocols.
        val signedPreKeyId = (System.currentTimeMillis() / 1000 % 0x00FFFFFF).toInt()
        val signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, signedPreKeyId)
        store.storeSignedPreKey(signedPreKey.id, signedPreKey)
        Logger.d("E2EE_INIT: Stored identity and signed pre-key locally (regId: $registrationId, signedKeyId: $signedPreKeyId)", tag = "E2EE")

        return SignalIdentityKeys(
            registrationId = registrationId,
            identityKey = Base64.encodeToString(identityKeyPair.publicKey.serialize(), Base64.NO_WRAP),
            signedPreKeyId = signedPreKey.id,
            signedPreKey = Base64.encodeToString(signedPreKey.keyPair.publicKey.serialize(), Base64.NO_WRAP),
            signedPreKeySignature = Base64.encodeToString(signedPreKey.signature, Base64.NO_WRAP)
        )
    }

    override suspend fun generateOneTimePreKeys(startId: Int, count: Int): List<SignalOneTimePreKey> {
        Logger.d("E2EE_INIT: Generating $count one-time pre-keys starting from ID $startId", tag = "E2EE")
        val preKeys = KeyHelper.generatePreKeys(startId, count)
        val resultList = mutableListOf<SignalOneTimePreKey>()
        for (preKey in preKeys) {
            store.storePreKey(preKey.id, preKey)
            resultList.add(
                SignalOneTimePreKey(
                    keyId = preKey.id,
                    publicKey = Base64.encodeToString(preKey.keyPair.publicKey.serialize(), Base64.NO_WRAP)
                )
            )
        }
        return resultList
    }

    override suspend fun processPreKeyBundle(userId: String, bundle: PreKeyBundle) {
        Logger.d("E2EE_SESSION: Processing pre-key bundle for user $userId", tag = "E2EE")
        val address = SignalProtocolAddress(userId, DEVICE_ID)
        val sessionBuilder = SessionBuilder(store, address)

        val nativeBundle = SignalNativePreKeyBundle(
            bundle.registrationId,
            bundle.deviceId,
            bundle.preKeyId ?: -1,
            bundle.preKeyPublic?.let { org.whispersystems.libsignal.ecc.Curve.decodePoint(Base64.decode(it, Base64.DEFAULT), 0) },
            bundle.signedPreKeyId,
            org.whispersystems.libsignal.ecc.Curve.decodePoint(Base64.decode(bundle.signedPreKeyPublic, Base64.DEFAULT), 0),
            Base64.decode(bundle.signedPreKeySignature, Base64.DEFAULT),
            IdentityKey(Base64.decode(bundle.identityKey, Base64.DEFAULT), 0)
        )

        sessionBuilder.process(nativeBundle)
        Logger.d("E2EE_SESSION: Successfully processed bundle and established session for $userId", tag = "E2EE")
    }

    override suspend fun hasSession(userId: String): Boolean {
        val address = SignalProtocolAddress(userId, DEVICE_ID)
        return store.containsSession(address)
    }

    override suspend fun deleteSession(userId: String) {
        Logger.d("E2EE_SESSION: Deleting session for user $userId", tag = "E2EE")
        val address = SignalProtocolAddress(userId, DEVICE_ID)
        store.deleteSession(address)
        Logger.d("E2EE_SESSION: Session deleted for user $userId", tag = "E2EE")
    }

    override suspend fun encryptMessage(recipientId: String, message: ByteArray): EncryptedMessage {
        Logger.d("E2EE_ENCRYPT: Encrypting message for recipient $recipientId", tag = "E2EE")
        val address = SignalProtocolAddress(recipientId, DEVICE_ID)
        val sessionCipher = SessionCipher(store, address)
        val ciphertextMessage = sessionCipher.encrypt(message)
        Logger.d("E2EE_ENCRYPT: Successfully encrypted message (type: ${ciphertextMessage.type})", tag = "E2EE")

        return EncryptedMessage(
            type = ciphertextMessage.type,
            body = Base64.encodeToString(ciphertextMessage.serialize(), Base64.NO_WRAP),
            registrationId = store.getLocalRegistrationId()
        )
    }

    override suspend fun decryptMessage(senderId: String, message: EncryptedMessage): ByteArray {
        Logger.d("E2EE_DECRYPT: Decrypting message from sender $senderId (type: ${message.type})", tag = "E2EE")
        val address = SignalProtocolAddress(senderId, DEVICE_ID)
        val sessionCipher = SessionCipher(store, address)

        val decodedBody = Base64.decode(message.body, Base64.DEFAULT)

        val decrypted = if (message.type == CiphertextMessage.PREKEY_TYPE) {
            sessionCipher.decrypt(PreKeySignalMessage(decodedBody))
        } else {
            sessionCipher.decrypt(SignalMessage(decodedBody))
        }
        Logger.d("E2EE_DECRYPT: Successfully decrypted message from $senderId", tag = "E2EE")
        return decrypted
    }

    override suspend fun getLocalRegistrationId(): Int {
        return store.getLocalRegistrationId()
    }

    override suspend fun getLocalIdentityKey(): String {
        return Base64.encodeToString(store.identityKeyPair.publicKey.serialize(), Base64.NO_WRAP)
    }

    override suspend fun checkKeyRotationNeeded(thresholdDays: Int): Boolean {
        return store.checkKeyRotationNeeded(thresholdDays)
    }

    override suspend fun hasIdentity(): Boolean {
        return store.hasIdentity()
    }
}
