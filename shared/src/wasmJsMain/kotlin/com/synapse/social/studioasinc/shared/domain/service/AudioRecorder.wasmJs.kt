package com.synapse.social.studioasinc.shared.domain.service

actual class AudioRecorder {
    actual fun startRecording(outputPath: String) {}
    actual fun stopRecording(): ByteArray? = null
    actual fun isRecording(): Boolean = false
}
