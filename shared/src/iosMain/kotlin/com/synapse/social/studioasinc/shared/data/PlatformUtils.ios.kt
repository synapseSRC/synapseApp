package com.synapse.social.studioasinc.shared.data

import kotlinx.cinterop.*
import platform.CoreCrypto.*

@OptIn(ExperimentalForeignApi::class)
actual object PlatformUtils {



    actual fun sha1(input: String): String {
        val data = input.encodeToByteArray()
        return memScoped {
            val digest = allocArray<UByteVar>(CC_SHA1_DIGEST_LENGTH)
            if (data.isEmpty()) {
                CC_SHA1(null, 0u, digest)
            } else {
                data.usePinned { pinned ->
                    CC_SHA1(pinned.addressOf(0), data.size.toUInt(), digest)
                }
            }
            digest.toHex(CC_SHA1_DIGEST_LENGTH)
        }
    }

    actual fun sha256(input: String): String {
        val data = input.encodeToByteArray()
        return memScoped {
            val digest = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
            if (data.isEmpty()) {
                CC_SHA256(null, 0u, digest)
            } else {
                data.usePinned { pinned ->
                    CC_SHA256(pinned.addressOf(0), data.size.toUInt(), digest)
                }
            }
            digest.toHex(CC_SHA256_DIGEST_LENGTH)
        }
    }

    actual fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val dataBytes = data.encodeToByteArray()
        return memScoped {
            val result = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)


            if (key.isEmpty() && dataBytes.isEmpty()) {
                CCHmac(kCCHmacAlgSHA256, null, 0u, null, 0u, result)
            } else if (key.isEmpty()) {
                dataBytes.usePinned { pinnedData ->
                    CCHmac(
                        kCCHmacAlgSHA256,
                        null, 0u,
                        pinnedData.addressOf(0), dataBytes.size.toULong(),
                        result
                    )
                }
            } else if (dataBytes.isEmpty()) {
                key.usePinned { pinnedKey ->
                    CCHmac(
                        kCCHmacAlgSHA256,
                        pinnedKey.addressOf(0), key.size.toULong(),
                        null, 0u,
                        result
                    )
                }
            } else {
                key.usePinned { pinnedKey ->
                    dataBytes.usePinned { pinnedData ->
                        CCHmac(
                            kCCHmacAlgSHA256,
                            pinnedKey.addressOf(0), key.size.toULong(),
                            pinnedData.addressOf(0), dataBytes.size.toULong(),
                            result
                        )
                    }
                }
            }
            result.readBytes(CC_SHA256_DIGEST_LENGTH)
        }
    }

    private fun CPointer<UByteVar>.toHex(length: Int): String {
        return (0 until length).joinToString("") { i ->
            this[i].toString(16).padStart(2, '0')
        }
    }
}
