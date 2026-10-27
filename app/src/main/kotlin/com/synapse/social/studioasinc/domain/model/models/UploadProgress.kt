package com.synapse.social.studioasinc.domain.model.models

enum class UploadState {
    QUEUED,
    COMPRESSING,
    UPLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
