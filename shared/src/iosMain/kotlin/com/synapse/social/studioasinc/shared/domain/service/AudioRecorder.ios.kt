package com.synapse.social.studioasinc.shared.domain.service

import platform.AVFAudio.*
import platform.Foundation.*
import platform.CoreAudioTypes.*
import kotlinx.cinterop.*
import platform.posix.memcpy
import platform.darwin.OSStatus

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class AudioRecorder {
    private var recorder: AVAudioRecorder? = null
    private var outputUrl: NSURL? = null

    actual fun startRecording(outputPath: String) {
        val fileManager = NSFileManager.defaultManager
        val documentDirectory = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        outputUrl = documentDirectory?.URLByAppendingPathComponent(outputPath)

        outputUrl?.let { url ->
            val settings = mapOf<Any?, Any>(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to 44100.0,
                AVNumberOfChannelsKey to 1
            )

            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryRecord, error = null)
            session.setActive(true, error = null)

            recorder = AVAudioRecorder(url, settings, null)
            recorder?.prepareToRecord()
            recorder?.record()
        }
    }

    actual fun stopRecording(): ByteArray? {
        recorder?.stop()
        recorder = null

        return outputUrl?.let { url ->
            val data = NSData.dataWithContentsOfURL(url)
            data?.let {
                if (it.length == 0uL) return@let ByteArray(0)
                val bytes = ByteArray(it.length.toInt())
                memcpy(bytes.refTo(0), it.bytes, it.length)
                bytes
            }
        }
    }

    actual fun isRecording(): Boolean = recorder?.recording ?: false
}
