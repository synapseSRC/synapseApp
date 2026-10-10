package com.synapse.social.studioasinc.shared.data.crypto

import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalIdentityKeys
import com.synapse.social.studioasinc.shared.data.crypto.models.SignalOneTimePreKey
import platform.CoreCrypto.*
import kotlinx.cinterop.*
import platform.posix.uint8_tVar
import platform.posix.size_tVar
import com.russhwolf.settings.Settings
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import io.github.aakira.napier.Napier

@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSignalProtocolManager : SignalProtocolManager {

    private val settings = Settings()
    private val PREF_REG_ID = "signal_reg_id"
    private val PREF_ID_PUB = "signal_id_pub"
    private val PREF_ID_PRIV = "signal_id_priv"
    private val PREF_SIGNED_PRE_ID = "signal_signed_pre_id"
    private val PREF_SIGNED_PRE_PUB = "signal_signed_pre_pub"
    private val PREF_SIGNED_PRE_PRIV = "signal_signed_pre_priv"
    private val PREF_LAST_ROTATION = "signal_last_rotation"

    private fun getSessionKey(userId: String) = "signal_session_$userId"

    override suspend fun generateIdentityAndKeys(): SignalIdentityKeys {
        var regId = settings.getInt(PREF_REG_ID, -1)
        if (regId == -1) {
            regId = Random.nextInt(1, 16380)
            settings.putInt(PREF_REG_ID, regId)
        }

        val idPub = settings.getStringOrNull(PREF_ID_PUB)
        val idPriv = settings.getStringOrNull(PREF_ID_PRIV)

        val (pub, priv) = if (idPub == null || idPriv == null) {
            generateKeyPair()
        } else {
            Pair(idPub, idPriv)
        }

        settings.putString(PREF_ID_PUB, pub)
        settings.putString(PREF_ID_PRIV, priv)

        val signedPreId = (io.ktor.util.date.getTimeMillis() / 1000 % 0x00FFFFFF).toInt()
        val (signedPub, signedPriv) = generateKeyPair()

        settings.putInt(PREF_SIGNED_PRE_ID, signedPreId)
        settings.putString(PREF_SIGNED_PRE_PUB, signedPub)
        settings.putString(PREF_SIGNED_PRE_PRIV, signedPriv)
        settings.putLong(PREF_LAST_ROTATION, io.ktor.util.date.getTimeMillis())

        return SignalIdentityKeys(
            registrationId = regId,
            identityKey = pub,
            signedPreKeyId = signedPreId,
            signedPreKey = signedPub,
            signedPreKeySignature = "MOCK_SIGNATURE" // CoreCrypto doesn't have an easy Ed25519 signature exposed here
        )
    }

    override suspend fun generateOneTimePreKeys(startId: Int, count: Int): List<SignalOneTimePreKey> {
        val keys = mutableListOf<SignalOneTimePreKey>()
        for (i in 0 until count) {
            val (pub, priv) = generateKeyPair()
            val keyId = startId + i
            settings.putString("signal_prekey_pub_$keyId", pub)
            settings.putString("signal_prekey_priv_$keyId", priv)
            keys.add(SignalOneTimePreKey(keyId, pub))
        }
        return keys
    }

    override suspend fun processPreKeyBundle(userId: String, bundle: PreKeyBundle) {
        // Derive shared secret
        val myIdPriv = settings.getString(PREF_ID_PRIV, "")
        val theirSignedPrePub = bundle.signedPreKeyPublic

        if (myIdPriv.isNotEmpty() && theirSignedPrePub.isNotEmpty()) {
            val sharedSecret = deriveSharedSecret(myIdPriv, theirSignedPrePub)
            if (sharedSecret != null) {
                settings.putString(getSessionKey(userId), sharedSecret)
            }
        }
    }

    override suspend fun hasSession(userId: String): Boolean {
        return settings.getStringOrNull(getSessionKey(userId)) != null
    }

    override suspend fun deleteSession(userId: String) {
        settings.remove(getSessionKey(userId))
    }

    override suspend fun deleteAllSessions() {
        settings.keys.filter { it.startsWith("signal_session_") }.forEach {
            settings.remove(it)
        }
    }

    override suspend fun deleteIdentity(userId: String) {
        settings.remove(PREF_REG_ID)
        settings.remove(PREF_ID_PUB)
        settings.remove(PREF_ID_PRIV)
        settings.remove(PREF_SIGNED_PRE_ID)
        settings.remove(PREF_SIGNED_PRE_PUB)
        settings.remove(PREF_SIGNED_PRE_PRIV)
        deleteAllSessions()
    }

    override suspend fun encryptMessage(recipientId: String, message: ByteArray): EncryptedMessage {
        val sharedSecretB64 = settings.getStringOrNull(getSessionKey(recipientId))
            ?: throw Exception("No session for user $recipientId")

        val sharedSecret = Base64.decode(sharedSecretB64)

        // Use first 16 bytes of shared secret as key, next 16 as IV
        val key = sharedSecret.copyOfRange(0, 16)
        val iv = sharedSecret.copyOfRange(16, 32)

        val encryptedBytes = performAesCrypt(kCCEncrypt, key, iv, message)

        return EncryptedMessage(
            type = 1, // SignalMessage
            body = Base64.encode(encryptedBytes),
            registrationId = getLocalRegistrationId()
        )
    }

    override suspend fun decryptMessage(senderId: String, message: EncryptedMessage): ByteArray {
        val sharedSecretB64 = settings.getStringOrNull(getSessionKey(senderId))
            ?: throw Exception("No session for user $senderId")

        val sharedSecret = Base64.decode(sharedSecretB64)
        val key = sharedSecret.copyOfRange(0, 16)
        val iv = sharedSecret.copyOfRange(16, 32)

        val cipherBytes = Base64.decode(message.body)
        return performAesCrypt(kCCDecrypt, key, iv, cipherBytes)
    }

    override suspend fun getLocalRegistrationId(): Int {
        return settings.getInt(PREF_REG_ID, 0)
    }

    override suspend fun getLocalIdentityKey(): String {
        return settings.getString(PREF_ID_PUB, "")
    }

    override suspend fun checkKeyRotationNeeded(thresholdDays: Int): Boolean {
        val lastRotation = settings.getLong(PREF_LAST_ROTATION, 0L)
        if (lastRotation == 0L) return false
        val daysSinceRotation = (io.ktor.util.date.getTimeMillis() - lastRotation) / (1000L * 60 * 60 * 24)
        return daysSinceRotation >= thresholdDays
    }

    override suspend fun hasIdentity(): Boolean {
        return settings.getInt(PREF_REG_ID, 0) != 0 && settings.getStringOrNull(PREF_ID_PUB) != null
    }

    private fun generateKeyPair(): Pair<String, String> {
        memScoped {
            val pk = allocArray<uint8_tVar>(32)
            val sk = allocArray<uint8_tVar>(32)
            cccurve25519_make_key_pair(pk, sk)
            val pkBytes = pk.readBytes(32)
            val skBytes = sk.readBytes(32)
            return Pair(Base64.encode(pkBytes), Base64.encode(skBytes))
        }
    }

    private fun deriveSharedSecret(privB64: String, pubB64: String): String? {
        val priv = Base64.decode(privB64)
        val pub = Base64.decode(pubB64)
        if (priv.size != 32 || pub.size != 32) return null

        memScoped {
            val sharedSecret = allocArray<uint8_tVar>(32)
            val sk = allocArray<uint8_tVar>(32)
            val pk = allocArray<uint8_tVar>(32)

            for (i in 0 until 32) {
                sk[i] = priv[i].toUByte()
                pk[i] = pub[i].toUByte()
            }

            val result = cccurve25519(sharedSecret, sk, pk)
            if (result == 0) {
                return Base64.encode(sharedSecret.readBytes(32))
            }
            return null
        }
    }

    private fun performAesCrypt(op: CCOperation, key: ByteArray, iv: ByteArray, dataIn: ByteArray): ByteArray {
        memScoped {
            val keyPtr = allocArray<uint8_tVar>(key.size)
            for (i in key.indices) keyPtr[i] = key[i].toUByte()

            val ivPtr = allocArray<uint8_tVar>(iv.size)
            for (i in iv.indices) ivPtr[i] = iv[i].toUByte()

            val inPtr = allocArray<uint8_tVar>(dataIn.size)
            for (i in dataIn.indices) inPtr[i] = dataIn[i].toUByte()

            // Add padding space
            val outSize = dataIn.size + kCCBlockSizeAES128.toInt()
            val outPtr = allocArray<uint8_tVar>(outSize)
            val outMoved = alloc<size_tVar>()

            val status = CCCrypt(
                op,
                kCCAlgorithmAES,
                kCCOptionPKCS7Padding,
                keyPtr,
                key.size.toULong(),
                ivPtr,
                inPtr,
                dataIn.size.toULong(),
                outPtr,
                outSize.toULong(),
                outMoved.ptr
            )

            if (status == kCCSuccess) {
                return outPtr.readBytes(outMoved.value.toInt())
            } else {
                throw Exception("CCCrypt failed with status $status")
            }
        }
    }
}
