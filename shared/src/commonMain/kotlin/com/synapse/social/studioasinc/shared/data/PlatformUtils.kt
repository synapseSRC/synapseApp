package com.synapse.social.studioasinc.shared.data

expect object PlatformUtils {
    fun sha1(input: String): String
    fun sha256(input: String): String
    fun sha256(input: ByteArray): String
    fun hmacSha256(key: ByteArray, data: String): ByteArray
}
