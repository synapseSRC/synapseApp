package com.synapse.social.studioasinc.shared.domain.service

expect class AudioRecorder {
    fun startRecording(outputPath: String)
    fun stopRecording(): ByteArray?
    fun isRecording(): Boolean
}
