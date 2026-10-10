package com.synapse.social.studioasinc.shared.security

import kotlinx.cinterop.*
import platform.CoreCrypto.*
import platform.Foundation.*
import platform.Security.*
import platform.darwin.OSStatus
import platform.posix.memcpy
import platform.CoreFoundation.*

@OptIn(ExperimentalForeignApi::class)
class IosSecurityCipher : SecurityCipher {

    // Constants
    private val keyAlias = "com.synapse.social.studioasinc.shared.security.key"
    private val keySize = kCCKeySizeAES256.toInt()
    private val blockSize = kCCBlockSizeAES128.toInt()
    private val algorithm = kCCAlgorithmAES
    private val options = kCCOptionPKCS7Padding

    private fun getSecretKey(): ByteArray {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrAccount, CFBridgingRetain(keyAlias as NSString))
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef?, result.ptr)

            if (status == errSecSuccess) {
                val data = result.value
                val nsData = data?.let { CFBridgingRelease(it) as? NSData }
                if (nsData != null && nsData.length.toInt() == keySize) {
                    val bytes = ByteArray(keySize)
                    if (keySize > 0) {
                        bytes.usePinned { pinned ->
                            memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
                        }
                    }
                    bytes
                } else {
                    deleteKey()
                    generateAndStoreKey()
                }
            } else if (status == errSecItemNotFound) {
                generateAndStoreKey()
            } else {
                generateAndStoreKey()
            }
        }
    }

    private fun generateAndStoreKey(): ByteArray {
        val key = ByteArray(keySize)
        val result = SecRandomCopyBytes(kSecRandomDefault, keySize.toULong(), key.refTo(0))
        if (result != errSecSuccess) {
             throw RuntimeException("Failed to generate random bytes for key: $result")
        }

        val data = key.toNSData()
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrAccount, CFBridgingRetain(keyAlias as NSString))
        CFDictionarySetValue(query, kSecValueData, CFBridgingRetain(data))
        CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)

        val status = SecItemAdd(query as CFDictionaryRef?, null)

        if (status == errSecSuccess) {
            return key
        } else if (status == errSecDuplicateItem) {
             deleteKey()
             val retryQuery = CFDictionaryCreateMutable(null, 0, null, null)
             CFDictionarySetValue(retryQuery, kSecClass, kSecClassGenericPassword)
             CFDictionarySetValue(retryQuery, kSecAttrAccount, CFBridgingRetain(keyAlias as NSString))
             CFDictionarySetValue(retryQuery, kSecValueData, CFBridgingRetain(data))
             CFDictionarySetValue(retryQuery, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)

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
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrAccount, CFBridgingRetain(keyAlias as NSString))
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
                val dataOutLength = data.size + blockSize
                val dataOut = allocArray<ByteVar>(dataOutLength)
                val dataOutMoved = alloc<ULongVar>()

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

    private fun ByteArray.toNSData(): NSData = memScoped {
        if (this@toNSData.isEmpty()) return NSData()
        this@toNSData.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = this@toNSData.size.toULong())
        }
    }

    private fun ByteArray.toBase64(): String {
        val data = this.toNSData()
        return data.base64EncodedStringWithOptions(0u)
    }

    private fun String.fromBase64(): ByteArray {
        val data = NSData.create(base64Encoding = this) ?: return ByteArray(0)
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
