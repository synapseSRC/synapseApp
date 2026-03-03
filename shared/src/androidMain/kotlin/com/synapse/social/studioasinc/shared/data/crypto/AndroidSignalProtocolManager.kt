package com.synapse.social.studioasinc.shared.data.crypto

import android.content.Context
import android.util.Base64
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalIdentityKeys
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalOneTimePreKey
import com.synapse.social.studioasinc.shared.data.crypto.store.AndroidSignalStore
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper
import org.whispersystems.libsignal.ecc.Curve

class AndroidSignalProtocolManager(context: Context, private val deviceId: Int = 1) : SignalProtocolManager {

    private val store = AndroidSignalStore(context)

    companion object {
        private const val PREKEY_TYPE = 3
    }

    override suspend fun generateIdentityAndKeys(): SignalIdentityKeys {

        val identityKeyPair: IdentityKeyPair = KeyHelper.generateIdentityKeyPair()
        val registrationId = KeyHelper.generateRegistrationId(false)

        store.saveIdentityKeyPair(identityKeyPair)
        store.saveLocalRegistrationId(registrationId)


        val lastSignedId = store.getLastSignedPreKeyId()
        val signedPreKeyId = lastSignedId + 1
        val signedPreKey: SignedPreKeyRecord = KeyHelper.generateSignedPreKey(identityKeyPair, signedPreKeyId)

        store.storeSignedPreKey(signedPreKeyId, signedPreKey)
        store.setLastSignedPreKeyId(signedPreKeyId)

        return SignalIdentityKeys(
            registrationId = registrationId,
            identityKey = Base64.encodeToString(identityKeyPair.publicKey.serialize(), Base64.NO_WRAP),
            signedPreKeyId = signedPreKeyId,
            signedPreKey = Base64.encodeToString(signedPreKey.keyPair.publicKey.serialize(), Base64.NO_WRAP),
            signedPreKeySignature = Base64.encodeToString(signedPreKey.signature, Base64.NO_WRAP)
        )
    }

    override suspend fun generateOneTimePreKeys(startId: Int, count: Int): List<SignalOneTimePreKey> {
        val preKeys: List<PreKeyRecord> = KeyHelper.generatePreKeys(startId, count)

        return preKeys.map { record ->
            store.storePreKey(record.id, record)
            SignalOneTimePreKey(
                keyId = record.id,
                publicKey = Base64.encodeToString(record.keyPair.publicKey.serialize(), Base64.NO_WRAP)
            )
        }
    }

    override suspend fun processPreKeyBundle(userId: String, bundle: PreKeyBundle) {
        val address = SignalProtocolAddress(userId, deviceId)
        val sessionBuilder = SessionBuilder(store, store, store, store, address)

        val preKeyBundle = org.whispersystems.libsignal.state.PreKeyBundle(
            bundle.registrationId,
            bundle.deviceId,
            bundle.preKeyId ?: 0,
            if (bundle.preKeyPublic != null) Curve.decodePoint(Base64.decode(bundle.preKeyPublic, Base64.NO_WRAP), 0) else null,
            bundle.signedPreKeyId,
            Curve.decodePoint(Base64.decode(bundle.signedPreKeyPublic, Base64.NO_WRAP), 0),
            Base64.decode(bundle.signedPreKeySignature, Base64.NO_WRAP),
            org.whispersystems.libsignal.IdentityKey(Base64.decode(bundle.identityKey, Base64.NO_WRAP), 0)
        )

        sessionBuilder.process(preKeyBundle)
    }

    override suspend fun hasSession(userId: String): Boolean {
        val address = SignalProtocolAddress(userId, deviceId)
        return store.containsSession(address)
    }

    override suspend fun encryptMessage(recipientId: String, message: ByteArray): EncryptedMessage {
        val address = SignalProtocolAddress(recipientId, deviceId)
        val sessionCipher = SessionCipher(store, store, store, store, address)

        val ciphertext = sessionCipher.encrypt(message)

        return EncryptedMessage(
            type = ciphertext.type,
            body = Base64.encodeToString(ciphertext.serialize(), Base64.NO_WRAP),
            registrationId = store.getLocalRegistrationId()
        )
    }

    override suspend fun decryptMessage(senderId: String, message: EncryptedMessage): ByteArray {
        val address = SignalProtocolAddress(senderId, deviceId)
        val sessionCipher = SessionCipher(store, store, store, store, address)

        val messageBytes = Base64.decode(message.body, Base64.NO_WRAP)

        return if (message.type == PREKEY_TYPE) {
            val preKeyMessage = PreKeySignalMessage(messageBytes)
            sessionCipher.decrypt(preKeyMessage)
        } else {
            val signalMessage = SignalMessage(messageBytes)
            sessionCipher.decrypt(signalMessage)
        }
    }

    override suspend fun getLocalRegistrationId(): Int {
        return store.getLocalRegistrationId()
    }

    override suspend fun getLocalIdentityKey(): String {
        val keyPair = store.getIdentityKeyPair()
        return Base64.encodeToString(keyPair.publicKey.serialize(), Base64.NO_WRAP)
    }
}
