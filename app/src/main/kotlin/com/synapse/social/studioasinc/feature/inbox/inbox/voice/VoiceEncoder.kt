package com.synapse.social.studioasinc.feature.inbox.inbox.voice

object VoiceEncoder {

    private val IEND_MARKER = byteArrayOf(
        0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
    )

    fun encode(audioBytes: ByteArray, carrierBytes: ByteArray): ByteArray {
        val result = ByteArray(carrierBytes.size + audioBytes.size)
        System.arraycopy(carrierBytes, 0, result, 0, carrierBytes.size)
        System.arraycopy(audioBytes, 0, result, carrierBytes.size, audioBytes.size)
        return result
    }

    fun decode(encodedBytes: ByteArray): ByteArray {
        val iendIndex = findMarker(encodedBytes, IEND_MARKER)
        if (iendIndex == -1) {
            // Not found, assume raw audio (or fallback)
            return encodedBytes
        }
        val audioStartIndex = iendIndex + IEND_MARKER.size
        if (audioStartIndex >= encodedBytes.size) {
            return ByteArray(0)
        }
        val audioSize = encodedBytes.size - audioStartIndex
        val audioBytes = ByteArray(audioSize)
        System.arraycopy(encodedBytes, audioStartIndex, audioBytes, 0, audioSize)
        return audioBytes
    }

    private fun findMarker(data: ByteArray, marker: ByteArray): Int {
        for (i in 0..data.size - marker.size) {
            var found = true
            for (j in marker.indices) {
                if (data[i + j] != marker[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }
}
