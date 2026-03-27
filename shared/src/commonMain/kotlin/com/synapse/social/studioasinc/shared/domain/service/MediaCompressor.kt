package com.synapse.social.studioasinc.shared.domain.service

interface MediaCompressor {
    suspend fun compress(filePath: String): Result<String>
}
