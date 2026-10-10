package com.synapse.social.studioasinc.shared.domain.service

import com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality

interface MediaCompressor {
    suspend fun compress(filePath: String): Result<String>
    suspend fun compress(filePath: String, quality: MediaUploadQuality): Result<String>
}
