package com.synapse.social.studioasinc.data.crypto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

/** Signal-compatible 60-digit numeric fingerprint for the app's stable Supabase user IDs. */
internal object SignalSafetyNumberGenerator {
    private const val ITERATIONS = 5_200
    private const val VERSION = 0

    fun generate(localStableId: String, localIdentityKey: String, remoteStableId: String, remoteIdentityKey: String): String {
        require(localStableId.isNotBlank() && remoteStableId.isNotBlank())
        val localKey = Base64.getDecoder().decode(localIdentityKey)
        val remoteKey = Base64.getDecoder().decode(remoteIdentityKey)
        require(localKey.isNotEmpty() && remoteKey.isNotEmpty())
        val halves = listOf(fingerprint(localStableId, localKey), fingerprint(remoteStableId, remoteKey)).sorted()
        return halves.joinToString(separator = "")
    }

    private fun fingerprint(stableId: String, serializedIdentityKey: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        var hash = byteArrayOf((VERSION shr 8).toByte(), VERSION.toByte()) +
            serializedIdentityKey + stableId.toByteArray(StandardCharsets.UTF_8)
        repeat(ITERATIONS) {
            digest.update(hash)
            hash = digest.digest(serializedIdentityKey)
        }
        return (0 until 6).joinToString(separator = "") { groupIndex ->
            var value = 0L
            val start = groupIndex * 5
            for (index in start until start + 5) value = (value shl 8) or (hash[index].toLong() and 0xffL)
            (value % 100_000L).toString().padStart(5, '0')
        }
    }
}
