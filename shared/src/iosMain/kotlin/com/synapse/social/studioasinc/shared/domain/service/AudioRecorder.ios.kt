package com.synapse.social.studioasinc.shared.domain.service

actual class AudioRecorder {
    actual fun startRecording(outputPath: String) {
        // iOS implementation using AVAudioRecorder
        // TODO: Implement using CoreAudio via cinterop
    }

    actual fun stopRecording(): ByteArray? {
        // TODO: Implement
        return null
    }

    actual fun isRecording(): Boolean = false
}
