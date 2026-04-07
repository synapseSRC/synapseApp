package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var amplitudeJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _amplitudeFlow = MutableStateFlow(0)
    val amplitudeFlow: StateFlow<Int> = _amplitudeFlow.asStateFlow()

    fun start(outputFile: File) {
        this.outputFile = outputFile

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)

            try {
                prepare()
                start()
                startAmplitudeUpdates()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop(): File? {
        stopAmplitudeUpdates()
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            // Can happen if stopped immediately after starting
            e.printStackTrace()
            outputFile?.delete()
            outputFile = null
        } finally {
            recorder?.release()
            recorder = null
        }
        return outputFile
    }

    fun cancel() {
        stopAmplitudeUpdates()
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            // Ignore
        } finally {
            recorder?.release()
            recorder = null
        }
        outputFile?.delete()
        outputFile = null
    }

    private fun startAmplitudeUpdates() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (true) {
                val maxAmp = try { recorder?.maxAmplitude ?: 0 } catch (e: Exception) { 0 }
                _amplitudeFlow.value = maxAmp
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
