package com.synapse.social.studioasinc.shared.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class XteaSecurityCipherTest {

    @Test
    fun testEncryptionDecryption() {
        val cipher = WasmJsSecurityCipher()
        val originalText = "Hello, Synapse!"

        val encryptedText = cipher.encrypt(originalText)
        assertNotEquals(originalText, encryptedText)

        val decryptedText = cipher.decrypt(encryptedText)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testEmptyString() {
        val cipher = WasmJsSecurityCipher()
        val originalText = ""

        val encryptedText = cipher.encrypt(originalText)
        assertEquals("", encryptedText)

        val decryptedText = cipher.decrypt(encryptedText)
        assertEquals("", decryptedText)
    }

    @Test
    fun testLongString() {
        val cipher = WasmJsSecurityCipher()
        val originalText = "This is a much longer string to test the block padding and multiple iterations of the XTEA algorithm. It should work perfectly fine regardless of the length of the input string, as long as we handle padding correctly."

        val encryptedText = cipher.encrypt(originalText)
        assertNotEquals(originalText, encryptedText)

        val decryptedText = cipher.decrypt(encryptedText)
        assertEquals(originalText, decryptedText)
    }
}
