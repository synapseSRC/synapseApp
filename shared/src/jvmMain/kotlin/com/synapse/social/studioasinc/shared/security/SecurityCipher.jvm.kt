package com.synapse.social.studioasinc.shared.security

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import java.util.Base64

class JvmSecurityCipher : SecurityCipher {
    private val key: SecretKey

    init {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        key = keyGen.generateKey()
    }

    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(plainText.toByteArray())
        return Base64.getEncoder().encodeToString(encrypted)
    }

    override fun decrypt(cipherText: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decoded = Base64.getDecoder().decode(cipherText)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted)
    }
}
