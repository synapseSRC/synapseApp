package com.synapse.social.studioasinc.shared.security

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class WasmJsSecurityCipher : SecurityCipher {
    // 128-bit key for XTEA - choosing a random-looking key
    // In a Web/Wasm environment, the key is distributed with the client code.
    // This provides a "basic" encryption layer as requested, primarily protecting
    // against casual inspection of LocalStorage.
    private val key = intArrayOf(
        0x7F2B4A1D.toInt(), 0x1A8C9E3F.toInt(), 0x5D6E7F8A.toInt(), 0x2B3C4D5E.toInt()
    )

    @OptIn(ExperimentalEncodingApi::class)
    override fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        try {
            val bytes = plainText.encodeToByteArray()
            val paddedBytes = pad(bytes)
            val encryptedBytes = ByteArray(paddedBytes.size)

            for (i in 0 until paddedBytes.size step 8) {
                val v0 = ((paddedBytes[i].toInt() and 0xFF) shl 24) or
                         ((paddedBytes[i + 1].toInt() and 0xFF) shl 16) or
                         ((paddedBytes[i + 2].toInt() and 0xFF) shl 8) or
                         (paddedBytes[i + 3].toInt() and 0xFF)
                val v1 = ((paddedBytes[i + 4].toInt() and 0xFF) shl 24) or
                         ((paddedBytes[i + 5].toInt() and 0xFF) shl 16) or
                         ((paddedBytes[i + 6].toInt() and 0xFF) shl 8) or
                         (paddedBytes[i + 7].toInt() and 0xFF)

                var ev0 = v0
                var ev1 = v1
                var sum = 0
                val delta = -0x61c88647 // 0x9E3779B9
                for (j in 0 until 32) {
                    ev0 += (((ev1 shl 4) xor (ev1 ushr 5)) + ev1) xor (sum + key[sum and 3])
                    sum += delta
                    ev1 += (((ev0 shl 4) xor (ev0 ushr 5)) + ev0) xor (sum + key[(sum ushr 11) and 3])
                }

                encryptedBytes[i] = (ev0 ushr 24).toByte()
                encryptedBytes[i + 1] = (ev0 ushr 16).toByte()
                encryptedBytes[i + 2] = (ev0 ushr 8).toByte()
                encryptedBytes[i + 3] = ev0.toByte()
                encryptedBytes[i + 4] = (ev1 ushr 24).toByte()
                encryptedBytes[i + 5] = (ev1 ushr 16).toByte()
                encryptedBytes[i + 6] = (ev1 ushr 8).toByte()
                encryptedBytes[i + 7] = ev1.toByte()
            }

            return Base64.encode(encryptedBytes)
        } catch (e: Exception) {
            return plainText
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        try {
            val encryptedBytes = Base64.decode(cipherText)
            if (encryptedBytes.size % 8 != 0) return cipherText

            val decryptedBytes = ByteArray(encryptedBytes.size)
            for (i in 0 until encryptedBytes.size step 8) {
                val v0 = ((encryptedBytes[i].toInt() and 0xFF) shl 24) or
                         ((encryptedBytes[i + 1].toInt() and 0xFF) shl 16) or
                         ((encryptedBytes[i + 2].toInt() and 0xFF) shl 8) or
                         (encryptedBytes[i + 3].toInt() and 0xFF)
                val v1 = ((encryptedBytes[i + 4].toInt() and 0xFF) shl 24) or
                         ((encryptedBytes[i + 5].toInt() and 0xFF) shl 16) or
                         ((encryptedBytes[i + 6].toInt() and 0xFF) shl 8) or
                         (encryptedBytes[i + 7].toInt() and 0xFF)

                var dv0 = v0
                var dv1 = v1
                val delta = -0x61c88647
                var sum = delta * 32
                for (j in 0 until 32) {
                    dv1 -= (((dv0 shl 4) xor (dv0 ushr 5)) + dv0) xor (sum + key[(sum ushr 11) and 3])
                    sum -= delta
                    dv0 -= (((dv1 shl 4) xor (dv1 ushr 5)) + dv1) xor (sum + key[sum and 3])
                }

                decryptedBytes[i] = (dv0 ushr 24).toByte()
                decryptedBytes[i + 1] = (dv0 ushr 16).toByte()
                decryptedBytes[i + 2] = (dv0 ushr 8).toByte()
                decryptedBytes[i + 3] = dv0.toByte()
                decryptedBytes[i + 4] = (dv1 ushr 24).toByte()
                decryptedBytes[i + 5] = (dv1 ushr 16).toByte()
                decryptedBytes[i + 6] = (dv1 ushr 8).toByte()
                decryptedBytes[i + 7] = dv1.toByte()
            }

            val unpadded = unpad(decryptedBytes)
            return unpadded.decodeToString()
        } catch (e: Exception) {
            return cipherText
        }
    }

    private fun pad(data: ByteArray): ByteArray {
        val padding = 8 - (data.size % 8)
        val padded = ByteArray(data.size + padding)
        data.copyInto(padded)
        for (i in data.size until padded.size) {
            padded[i] = padding.toByte()
        }
        return padded
    }

    private fun unpad(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val padding = data.last().toInt()
        if (padding < 1 || padding > 8) return data
        if (padding > data.size) return data
        for (i in data.size - padding until data.size) {
            if (data[i].toInt() != padding) return data
        }
        return data.copyOfRange(0, data.size - padding)
    }
}
