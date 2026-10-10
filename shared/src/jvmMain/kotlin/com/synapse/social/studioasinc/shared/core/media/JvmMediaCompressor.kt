package com.synapse.social.studioasinc.shared.core.media

import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality


class JvmMediaCompressor : MediaCompressor {
    override suspend fun compress(filePath: String): Result<String> = Result.success(filePath)

    override suspend fun compress(filePath: String, quality: MediaUploadQuality): Result<String> {
        return compress(filePath)
    }
}