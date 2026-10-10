package com.synapse.social.studioasinc.domain.model.models



data class UploadProgress(
    val uploadId: String,
    val fileName: String,
    val progress: Float,
    val bytesUploaded: Long,
    val totalBytes: Long,
    val state: UploadState,
    val error: String? = null,
    val result: MediaUploadResult? = null
)



enum class UploadState {
    QUEUED,
    COMPRESSING,
    UPLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
