package com.synapse.social.studioasinc.shared.domain.model.chat

data class MessageAttachment(
    val url: String,
    val type: AttachmentType,
    val size: Long? = null,
    val duration: Int? = null,
    val mediaGroupId: String? = null,
    val uploadProgress: Float = 1.0f,
    val isUploadFailed: Boolean = false
)

enum class AttachmentType {
    IMAGE, VIDEO, AUDIO, FILE
}
