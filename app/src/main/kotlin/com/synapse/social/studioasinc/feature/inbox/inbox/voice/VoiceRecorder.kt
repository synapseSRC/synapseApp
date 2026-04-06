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
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

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
            prepare()
            start()
        }

        recordingJob = scope.launch {
            while (true) {
                _amplitudeFlow.value = recorder?.maxAmplitude ?: 0
                delay(100)
            }
        }
    }

    fun stop(): File {
        recordingJob?.cancel()
        recordingJob = null
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        _amplitudeFlow.value = 0
        return outputFile ?: throw IllegalStateException("Output file is null")
    }

    fun cancel() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
        _amplitudeFlow.value = 0
    }
}
