package com.synapse.social.studioasinc.shared.security

import kotlinx.cinterop.*
import platform.CoreCrypto.*
import platform.Foundation.*
import platform.Security.*
import platform.darwin.OSStatus
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class IosSecurityCipher : SecurityCipher {

    // Constants
    private val keyAlias = "com.synapse.social.studioasinc.shared.security.key"
    // kCCKeySizeAES256 is 32
    private val keySize = kCCKeySizeAES256.toInt()
    // kCCBlockSizeAES128 is 16
    private val blockSize = kCCBlockSizeAES128.toInt()
    // kCCAlgorithmAES is usually 0
    private val algorithm = kCCAlgorithmAES
    // kCCOptionPKCS7Padding is 1
    private val options = kCCOptionPKCS7Padding

    private fun getSecretKey(): ByteArray {
        val query = NSMutableDictionary.create()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass)
        query.setObject(keyAlias, forKey = kSecAttrAccount)
        query.setObject(kCFBooleanTrue, forKey = kSecReturnData)
        query.setObject(kSecMatchLimitOne, forKey = kSecMatchLimit)

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef?, result.ptr)

            if (status == errSecSuccess) {
                val data = result.value
                val nsData = data?.let { CFBridgingRelease(it) as? NSData }
                if (nsData != null && nsData.length.toInt() == keySize) {
                    val bytes = ByteArray(keySize)
                    bytes.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
                    }
                    bytes
                } else {
                    // Invalid key found, regenerate
                    deleteKey()
                    generateAndStoreKey()
                }
            } else if (status == errSecItemNotFound) {
                generateAndStoreKey()
            } else {
                // Other error, attempt to regenerate
                generateAndStoreKey()
            }
        }
    }

    private fun generateAndStoreKey(): ByteArray {
        val key = ByteArray(keySize)
        // kSecRandomDefault is available in Security framework
        val result = SecRandomCopyBytes(kSecRandomDefault, keySize.toULong(), key.refTo(0))
        if (result != errSecSuccess) {
             throw RuntimeException("Failed to generate random bytes for key: $result")
        }

        val data = key.toNSData()
        val query = NSMutableDictionary.create()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass)
        query.setObject(keyAlias, forKey = kSecAttrAccount)
        query.setObject(data, forKey = kSecValueData)
        query.setObject(kSecAttrAccessibleAfterFirstUnlock, forKey = kSecAttrAccessible)

        val status = SecItemAdd(query as CFDictionaryRef?, null)

        if (status == errSecSuccess) {
            return key
        } else if (status == errSecDuplicateItem) {
             deleteKey()
             // Retry once
             val retryQuery = NSMutableDictionary.create()
             retryQuery.setObject(kSecClassGenericPassword, forKey = kSecClass)
             retryQuery.setObject(keyAlias, forKey = kSecAttrAccount)
             retryQuery.setObject(data, forKey = kSecValueData)
             retryQuery.setObject(kSecAttrAccessibleAfterFirstUnlock, forKey = kSecAttrAccessible)

             val retryStatus = SecItemAdd(retryQuery as CFDictionaryRef?, null)
             if (retryStatus != errSecSuccess) {
                  throw RuntimeException("Failed to store key in Keychain after retry: $retryStatus")
             }
             return key
        } else {
             throw RuntimeException("Failed to store key in Keychain: $status")
        }
    }

    private fun deleteKey() {
        val query = NSMutableDictionary.create()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass)
        query.setObject(keyAlias, forKey = kSecAttrAccount)
        SecItemDelete(query as CFDictionaryRef?)
    }

    override fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""

        try {
            val key = getSecretKey()
            val iv = ByteArray(blockSize)
            val randomResult = SecRandomCopyBytes(kSecRandomDefault, blockSize.toULong(), iv.refTo(0))
            if (randomResult != errSecSuccess) return plainText

            val data = plainText.encodeToByteArray()

            return memScoped {
                // Output size: input size + block size (for padding)
                val dataOutLength = data.size + blockSize
                val dataOut = allocArray<ByteVar>(dataOutLength)
                val dataOutMoved = alloc<ULongVar>() // size_tVar

                val status = CCCrypt(
                    kCCEncrypt,
                    algorithm,
                    options,
                    key.refTo(0), key.size.toULong(),
                    iv.refTo(0),
                    data.refTo(0), data.size.toULong(),
                    dataOut, dataOutLength.toULong(),
                    dataOutMoved.ptr
                )

                if (status == kCCSuccess) {
                    val cipherBytes = dataOut.readBytes(dataOutMoved.value.toInt())
                    // Combined = IV + CipherText
                    val combined = ByteArray(blockSize + cipherBytes.size)
                    iv.copyInto(combined, 0, 0, blockSize)
                    cipherBytes.copyInto(combined, blockSize, 0, cipherBytes.size)

                    combined.toBase64()
                } else {
                    plainText
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return plainText
        }
    }

    override fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""

        try {
            val combined = cipherText.fromBase64()
            if (combined.size < blockSize) return cipherText

            val iv = combined.copyOfRange(0, blockSize)
            val cipherBytes = combined.copyOfRange(blockSize, combined.size)
            val key = getSecretKey()

            return memScoped {
                val dataOutLength = cipherBytes.size + blockSize
                val dataOut = allocArray<ByteVar>(dataOutLength)
                val dataOutMoved = alloc<ULongVar>()

                val status = CCCrypt(
                    kCCDecrypt,
                    algorithm,
                    options,
                    key.refTo(0), key.size.toULong(),
                    iv.refTo(0),
                    cipherBytes.refTo(0), cipherBytes.size.toULong(),
                    dataOut, dataOutLength.toULong(),
                    dataOutMoved.ptr
                )

                if (status == kCCSuccess) {
                    val plainBytes = dataOut.readBytes(dataOutMoved.value.toInt())
                    plainBytes.decodeToString()
                } else {
                    cipherText
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return cipherText
        }
    }

    // Helper extensions
    private fun ByteArray.toNSData(): NSData = memScoped {
        NSData.create(bytes = this@toNSData.refTo(0), length = this@toNSData.size.toULong())
    }

    private fun ByteArray.toBase64(): String {
        val data = this.toNSData()
        return data.base64EncodedStringWithOptions(0u)
    }

    private fun String.fromBase64(): ByteArray {
        val data = NSData(base64EncodedString = this, options = 0u)
        if (data == null) return ByteArray(0)

        val length = data.length.toInt()
        val bytes = ByteArray(length)
        if (length > 0) {
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
        return bytes
    }
}
