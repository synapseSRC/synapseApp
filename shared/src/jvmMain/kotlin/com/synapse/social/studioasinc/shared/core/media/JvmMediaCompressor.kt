package com.synapse.social.studioasinc.shared.core.media

import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor

class JvmMediaCompressor : MediaCompressor {
    override suspend fun compress(filePath: String): Result<String> = Result.success(filePath)
}
