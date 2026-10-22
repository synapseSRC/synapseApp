package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal data class VoiceEncryptionMaterial(
    val key: ByteArray,
    val nonce: ByteArray
)

internal data class VoiceMediaReference(
    val remoteUrl: String,
    val key: ByteArray,
    val nonce: ByteArray
)

/** Encrypts voice bytes locally; its reference is safe to send only inside the Signal E2EE payload. */
internal object VoiceMediaCrypto {
    private const val REFERENCE_PREFIX = "synapse-e2ee-voice:v1:"
    private const val KEY_BYTES = 32
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    fun encryptFile(source: File, destination: File): VoiceEncryptionMaterial {
        val random = SecureRandom()
        val key = ByteArray(KEY_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))

        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    val encrypted = cipher.update(buffer, 0, read)
                    if (encrypted != null && encrypted.isNotEmpty()) output.write(encrypted)
                }
                output.write(cipher.doFinal())
            }
        }
        return VoiceEncryptionMaterial(key, nonce)
    }

    fun createReference(remoteUrl: String, material: VoiceEncryptionMaterial): String {
        require(remoteUrl.startsWith("https://") || remoteUrl.startsWith("http://")) {
            "Voice storage returned an invalid URL."
        }
        val payload = JSONObject()
            .put("v", 1)
            .put("url", remoteUrl)
            .put("key", encode(material.key))
            .put("nonce", encode(material.nonce))
            .toString()
        return REFERENCE_PREFIX + encode(payload.toByteArray(Charsets.UTF_8))
    }

    fun parseReference(value: String): VoiceMediaReference? {
        if (!value.startsWith(REFERENCE_PREFIX)) return null
        val token = value.removePrefix(REFERENCE_PREFIX)
        require(token.isNotBlank()) { "Encrypted voice reference is empty." }
        val payload = JSONObject(String(Base64.decode(token, BASE64_FLAGS), Charsets.UTF_8))
        require(payload.optInt("v") == 1) { "Unsupported encrypted voice reference version." }
        val remoteUrl = payload.getString("url")
        require(remoteUrl.startsWith("https://") || remoteUrl.startsWith("http://")) {
            "Encrypted voice reference contains an invalid URL."
        }
        val key = Base64.decode(payload.getString("key"), BASE64_FLAGS)
        val nonce = Base64.decode(payload.getString("nonce"), BASE64_FLAGS)
        require(key.size == KEY_BYTES && nonce.size == NONCE_BYTES) {
            "Encrypted voice reference contains invalid key material."
        }
        return VoiceMediaReference(remoteUrl, key, nonce)
    }

    fun decrypt(ciphertext: ByteArray, reference: VoiceMediaReference): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(reference.key, "AES"),
            GCMParameterSpec(TAG_BITS, reference.nonce)
        )
        return cipher.doFinal(ciphertext)
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, BASE64_FLAGS)
}
