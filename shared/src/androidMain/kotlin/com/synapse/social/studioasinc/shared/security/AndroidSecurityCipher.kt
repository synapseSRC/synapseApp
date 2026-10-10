package com.synapse.social.studioasinc.shared.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidSecurityCipher : SecurityCipher {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "synapse_db_encryption_key"

    init {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    override fun encrypt(plainText: String): String {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Format: [IV Length (1 byte)] + [IV] + [Encrypted Data]
            val ivLength = iv.size
            val combined = ByteArray(1 + ivLength + encryptedBytes.size)
            combined[0] = ivLength.toByte()
            System.arraycopy(iv, 0, combined, 1, ivLength)
            System.arraycopy(encryptedBytes, 0, combined, 1 + ivLength, encryptedBytes.size)

            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return plainText // Fallback to avoid data loss on encryption failure (should not happen)
        }
    }

    override fun decrypt(cipherText: String): String {
        try {
            // Attempt to decode Base64
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)

            val ivLength = combined[0].toInt()
            // Sanity check for IV length (GCM is typically 12, but can vary)
            if (ivLength <= 0 || ivLength > 16) {
                return cipherText // Not our format, return original
            }

            val iv = ByteArray(ivLength)
            System.arraycopy(combined, 1, iv, 0, ivLength)

            val encryptedBytes = ByteArray(combined.size - 1 - ivLength)
            System.arraycopy(combined, 1 + ivLength, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            // If decryption fails (e.g., legacy plaintext data), return the original string
            return cipherText
        }
    }
}
