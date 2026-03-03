package com.synapse.social.studioasinc.shared.domain.service

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

actual class AudioRecorder {
    private var recorder: AVAudioRecorder? = null
    private var isRecordingState: Boolean = false
    private var currentOutputPath: String? = null

    @OptIn(ExperimentalForeignApi::class)
    actual fun startRecording(outputPath: String) {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayAndRecord, mode = AVAudioSessionModeDefault, options = 0u, error = null)
        session.setActive(true, error = null)

        val url = NSURL.fileURLWithPath(outputPath)
        val settings = mapOf<Any?, Any>(
            AVFormatIDKey to kAudioFormatMPEG4AAC,
            AVSampleRateKey to 44100.0,
            AVNumberOfChannelsKey to 1
        )

        recorder = memScoped {
            val error = alloc<kotlinx.cinterop.ObjCObjectVar<NSError?>>()
            AVAudioRecorder(url, settings, error.ptr)
        }

        recorder?.record()
        isRecordingState = true
        currentOutputPath = outputPath
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun stopRecording(): ByteArray? {
        if (!isRecordingState || recorder == null || currentOutputPath == null) return null

        recorder?.stop()
        isRecordingState = false

        val url = NSURL.fileURLWithPath(currentOutputPath!!)
        val data = NSData.dataWithContentsOfURL(url) ?: return null

        val length = data.length.toInt()
        if (length == 0) return null

        val byteArray = ByteArray(length)
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }

        val session = AVAudioSession.sharedInstance()
        session.setActive(false, error = null)

        recorder = null
        currentOutputPath = null

        return byteArray
    }

    actual fun isRecording(): Boolean = isRecordingState
}
