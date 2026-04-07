package com.synapse.social.studioasinc.feature.inbox.inbox.voice

object VoiceEncoder {

    // Standard PNG IEND chunk byte sequence
    private val IEND_MARKER = byteArrayOf(
        0x49, 0x45, 0x4E, 0x44, // "IEND"
        0xAE.toByte(), 0x42, 0x60, 0x82.toByte() // CRC32
    )

    /**
     * Appends the given audio bytes directly after the provided carrier (PNG) bytes.
     */
    fun encode(audioBytes: ByteArray, carrierBytes: ByteArray): ByteArray {
        val result = ByteArray(carrierBytes.size + audioBytes.size)
        System.arraycopy(carrierBytes, 0, result, 0, carrierBytes.size)
        System.arraycopy(audioBytes, 0, result, carrierBytes.size, audioBytes.size)
        return result
    }

    /**
     * Searches for the PNG IEND marker in the given encoded byte array and extracts everything that follows it.
     * Returns an empty ByteArray if the marker is not found or no data follows it.
     */
    fun decode(encodedBytes: ByteArray): ByteArray {
        val index = indexOfSubArray(encodedBytes, IEND_MARKER)
        if (index == -1) return ByteArray(0)

        val startIndex = index + IEND_MARKER.size
        if (startIndex >= encodedBytes.size) return ByteArray(0)

        val resultSize = encodedBytes.size - startIndex
        val result = ByteArray(resultSize)
        System.arraycopy(encodedBytes, startIndex, result, 0, resultSize)
        return result
    }

    private fun indexOfSubArray(array: ByteArray, subArray: ByteArray): Int {
        if (subArray.isEmpty() || subArray.size > array.size) return -1

        for (i in 0..array.size - subArray.size) {
            var match = true
            for (j in subArray.indices) {
                if (array[i + j] != subArray[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }
}
