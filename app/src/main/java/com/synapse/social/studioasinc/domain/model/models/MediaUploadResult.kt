package com.synapse.social.studioasinc.domain.model.models



data class MediaUploadResult(
    val url: String,
    val thumbnailUrl: String?,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Long? = null
)
