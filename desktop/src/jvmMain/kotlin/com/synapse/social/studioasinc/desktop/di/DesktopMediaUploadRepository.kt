package com.synapse.social.studioasinc.desktop.di

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository

/**
 * No-op stub for [MediaUploadRepository] on desktop.
 * Media upload is not supported on the Compose Desktop target.
 */
class DesktopMediaUploadRepository : MediaUploadRepository {
    override suspend fun upload(
        filePath: String,
        provider: StorageProvider,
        config: StorageConfig,
        bucketName: String?,
        onProgress: (Float) -> Unit
    ): Result<String> = Result.failure(UnsupportedOperationException("Media upload is not supported on Desktop."))

    override fun deleteFile(filePath: String) {
        // No-op on desktop
    }
}
