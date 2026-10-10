package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface VoiceRecorderStopResult {
    data class Success(val file: File) : VoiceRecorderStopResult
    object EmptyOrTooShort : VoiceRecorderStopResult
    data class Error(val cause: Exception) : VoiceRecorderStopResult
}

class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var amplitudeJob: Job? = null
    private var isPausedInternal = false
    private var startTimeMs: Long = 0L
    private var pauseStartTimeMs: Long = 0L
    private var pausedAccumulatedDurationMs: Long = 0L

    private val _amplitudeFlow = MutableStateFlow(0)
    val amplitudeFlow: StateFlow<Int> = _amplitudeFlow.asStateFlow()

    fun start(outputFile: File, scope: CoroutineScope) {
        cancel() // Safely stop and release any existing recorder instance before starting
        this.outputFile = outputFile
        isPausedInternal = false
        startTimeMs = System.currentTimeMillis()
        pauseStartTimeMs = 0L
        pausedAccumulatedDurationMs = 0L

        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(outputFile.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            startAmplitudeUpdates(scope)
        } catch (e: Exception) {
            rec.release()
            recorder = null
            outputFile.delete()
            this.outputFile = null
            throw e
        }
    }

    fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recorder != null) {
            try {
                recorder?.pause()
                if (!isPausedInternal) {
                    isPausedInternal = true
                    pauseStartTimeMs = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun resume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recorder != null) {
            try {
                recorder?.resume()
                if (isPausedInternal) {
                    isPausedInternal = false
                    if (pauseStartTimeMs > 0L) {
                        pausedAccumulatedDurationMs += (System.currentTimeMillis() - pauseStartTimeMs)
                        pauseStartTimeMs = 0L
                    }
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun getRecordedDurationMs(): Long {
        if (startTimeMs <= 0L) return 0L
        val currentPauseMs = if (isPausedInternal && pauseStartTimeMs > 0L) {
            System.currentTimeMillis() - pauseStartTimeMs
        } else {
            0L
        }
        return (System.currentTimeMillis() - startTimeMs - pausedAccumulatedDurationMs - currentPauseMs).coerceAtLeast(0L)
    }

    fun stop(): VoiceRecorderStopResult {
        stopAmplitudeUpdates()
        val resultFile = outputFile
        val durationMs = getRecordedDurationMs()

        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            isPausedInternal = false
            outputFile = null

            if (resultFile != null && resultFile.exists() && resultFile.length() > 0 && durationMs >= 500L) {
                VoiceRecorderStopResult.Success(resultFile)
            } else {
                resultFile?.delete()
                VoiceRecorderStopResult.EmptyOrTooShort
            }
        } catch (e: RuntimeException) {
            recorder?.release()
            recorder = null
            isPausedInternal = false
            resultFile?.delete()
            outputFile = null
            // RuntimeException on stop is standard MediaRecorder behavior when stopped <1s after start
            if (durationMs < 600L) {
                VoiceRecorderStopResult.EmptyOrTooShort
            } else {
                VoiceRecorderStopResult.Error(e)
            }
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            isPausedInternal = false
            resultFile?.delete()
            outputFile = null
            VoiceRecorderStopResult.Error(e)
        }
    }

    fun cancel() {
        stopAmplitudeUpdates()
        try {
            recorder?.stop()
        } catch (_: RuntimeException) {
            // Safe cancellation cleanup
        } finally {
            recorder?.release()
            recorder = null
            isPausedInternal = false
        }
        outputFile?.delete()
        outputFile = null
    }

    private fun startAmplitudeUpdates(scope: CoroutineScope) {
        stopAmplitudeUpdates()
        amplitudeJob = scope.launch {
            while (true) {
                if (!isPausedInternal) {
                    val maxAmp = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
                    _amplitudeFlow.value = maxAmp
                } else {
                    _amplitudeFlow.value = 0
                }
                delay(100)
            }
        }
    }

    private fun stopAmplitudeUpdates() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _amplitudeFlow.value = 0
    }
}
