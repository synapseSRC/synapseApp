package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import io.ktor.utils.io.ByteReadChannel

interface UploadService {
    suspend fun upload(
        fileProvider: suspend (Long) -> ByteReadChannel,
        fileSize: Long,
        fileName: String,
        config: StorageConfig,
        bucketName: String? = null,
        onProgress: (Float) -> Unit
    ): String
}
