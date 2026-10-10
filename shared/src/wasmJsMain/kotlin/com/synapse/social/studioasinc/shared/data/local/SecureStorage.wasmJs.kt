package com.synapse.social.studioasinc.shared.data.local

import org.w3c.dom.Storage
import kotlinx.browser.window
import com.synapse.social.studioasinc.shared.security.SecurityCipher

class WasmJsSecureStorage(private val cipher: SecurityCipher) : SecureStorage {
    private val storage: Storage = window.localStorage

    companion object {
        private const val ENCRYPTION_PREFIX = "v1:"
    }

    override fun save(key: String, value: String) {
        val encryptedValue = cipher.encrypt(value)
        storage.setItem(key, ENCRYPTION_PREFIX + encryptedValue)
    }

    override fun getString(key: String): String? {
        val storedValue = storage.getItem(key) ?: return null

        return if (storedValue.startsWith(ENCRYPTION_PREFIX)) {
            val encryptedContent = storedValue.substring(ENCRYPTION_PREFIX.length)
            val decryptedValue = cipher.decrypt(encryptedContent)
            if (decryptedValue == encryptedContent && encryptedContent.isNotEmpty()) {
                // Decryption failed (returned original)
                storedValue
            } else {
                decryptedValue
            }
        } else {
            // Legacy plain text data
            storedValue
        }
    }

    override fun clear(key: String) {
        storage.removeItem(key)
    }
}
