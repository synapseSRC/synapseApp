package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider

interface MediaUploadRepository {
    suspend fun upload(
        filePath: String,
        provider: StorageProvider,
        config: StorageConfig,
        bucketName: String?,
        onProgress: (Float) -> Unit
    ): Result<String>

    fun deleteFile(filePath: String)
}
