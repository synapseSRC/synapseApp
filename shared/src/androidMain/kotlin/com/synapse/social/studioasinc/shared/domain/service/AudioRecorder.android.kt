package com.synapse.social.studioasinc.shared.domain.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

actual class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    actual fun startRecording(outputPath: String) {
        outputFile = File(context.cacheDir, outputPath)
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile?.absolutePath)
            prepare()
            start()
        }
    }

    actual fun stopRecording(): ByteArray? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return outputFile?.readBytes()
    }

    actual fun isRecording(): Boolean = recorder != null
}
