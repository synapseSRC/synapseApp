package com.synapse.social.studioasinc.shared.data

import com.synapse.social.studioasinc.shared.util.toHexString
import org.kotlincrypto.hash.sha1.SHA1
import org.kotlincrypto.hash.sha2.SHA256
import org.kotlincrypto.macs.hmac.sha2.HmacSHA256

actual object PlatformUtils {
    actual fun sha1(input: String): String =
        SHA1().digest(input.encodeToByteArray()).toHexString()

    actual fun sha256(input: String): String =
        SHA256().digest(input.encodeToByteArray()).toHexString()

    actual fun sha256(input: ByteArray): String =
        SHA256().digest(input).toHexString()

    actual fun hmacSha256(key: ByteArray, data: String): ByteArray =
        HmacSHA256(key).doFinal(data.encodeToByteArray())
}
