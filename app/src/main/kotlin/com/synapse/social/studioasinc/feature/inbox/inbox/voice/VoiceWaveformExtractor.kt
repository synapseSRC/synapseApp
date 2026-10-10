package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

object VoiceWaveformExtractor {

    private const val DEFAULT_BAR_COUNT = 36
    private const val MIN_AMPLITUDE = 0.15f
    private const val MAX_AMPLITUDE = 1.0f

    suspend fun extractAmplitudes(
        filePath: String,
        barCount: Int = DEFAULT_BAR_COUNT,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): List<Float> = withContext(dispatcher) {
        try {
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                return@withContext generateFallbackAmplitudes(filePath, barCount)
            }

            // Attempt decoding compressed audio (e.g. OGG/Opus, AAC, MP3) to PCM with MediaExtractor & MediaCodec
            val decodedAmplitudes = decodePcmAmplitudes(filePath, barCount)
            if (decodedAmplitudes.isNotEmpty() && decodedAmplitudes.any { it > MIN_AMPLITUDE }) {
                return@withContext decodedAmplitudes
            }

            // Fallback for raw PCM/WAV byte reading if MediaCodec is not applicable
            val bytes = file.readBytes()
            if (bytes.size >= barCount) {
                val chunkSize = bytes.size / barCount
                val rawAmplitudes = FloatArray(barCount)
                var maxRms = 0f

                for (i in 0 until barCount) {
                    val start = i * chunkSize
                    val end = if (i == barCount - 1) bytes.size else (i + 1) * chunkSize

                    var sumSquares = 0.0
                    val count = end - start
                    for (j in start until end) {
                        val sample = bytes[j].toInt()
                        sumSquares += (sample * sample)
                    }

                    val rms = if (count > 0) sqrt(sumSquares / count).toFloat() else 0f
                    rawAmplitudes[i] = rms
                    if (rms > maxRms) {
                        maxRms = rms
                    }
                }

                if (maxRms > 0f) {
                    return@withContext rawAmplitudes.map { rms ->
                        val normalized = rms / maxRms
                        (MIN_AMPLITUDE + normalized * (MAX_AMPLITUDE - MIN_AMPLITUDE)).coerceIn(MIN_AMPLITUDE, MAX_AMPLITUDE)
                    }
                }
            }

            generateFallbackAmplitudes(filePath, barCount)
        } catch (e: Exception) {
            generateFallbackAmplitudes(filePath, barCount)
        }
    }

    private fun decodePcmAmplitudes(filePath: String, barCount: Int): List<Float> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(filePath)
            var trackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }

            if (trackIndex < 0 || format == null) {
                extractor.release()
                return emptyList()
            }

            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val samples = ArrayList<Short>()
            var sawInputEOS = false
            var sawOutputEOS = false

            while (!sawOutputEOS && samples.size < 1_000_000) {
                if (!sawInputEOS) {
                    val inIndex = codec.dequeueInputBuffer(10000L)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)
                        if (buffer != null) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sawInputEOS = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                codec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(info, 10000L)
                while (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && info.size > 0) {
                        outBuffer.position(info.offset)
                        outBuffer.limit(info.offset + info.size)
                        val shortBuffer = outBuffer.asShortBuffer()
                        while (shortBuffer.hasRemaining()) {
                            samples.add(shortBuffer.get())
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                        break
                    }
                    outIndex = codec.dequeueOutputBuffer(info, 0L)
                }
            }

            if (samples.isEmpty()) return emptyList()

            val chunkSize = samples.size / barCount
            if (chunkSize <= 0) return emptyList()

            val rmsValues = FloatArray(barCount)
            var maxRms = 0f

            for (i in 0 until barCount) {
                val start = i * chunkSize
                val end = if (i == barCount - 1) samples.size else (i + 1) * chunkSize
                var sumSquares = 0.0
                val count = end - start
                for (j in start until end) {
                    val s = samples[j].toDouble()
                    sumSquares += (s * s)
                }
                val rms = if (count > 0) sqrt(sumSquares / count).toFloat() else 0f
                rmsValues[i] = rms
                if (rms > maxRms) maxRms = rms
            }

            if (maxRms <= 0f) return emptyList()

            return rmsValues.map { rms ->
                val normalized = rms / maxRms
                (MIN_AMPLITUDE + normalized * (MAX_AMPLITUDE - MIN_AMPLITUDE)).coerceIn(MIN_AMPLITUDE, MAX_AMPLITUDE)
            }
        } catch (e: Exception) {
            return emptyList()
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (e: Exception) {
                // Ignore cleanup exceptions
            }
        }
    }

    fun generateFallbackAmplitudes(seedStr: String, barCount: Int = DEFAULT_BAR_COUNT): List<Float> {
        val hash = seedStr.hashCode()
        return List(barCount) { index ->
            val angle = (index + (abs(hash) % 10)) * 0.35f
            val wave = (sin(angle.toDouble()).toFloat() * 0.4f + 0.6f)
            wave.coerceIn(MIN_AMPLITUDE, MAX_AMPLITUDE)
        }
    }
}
