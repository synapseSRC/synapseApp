package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.FileUploader
import com.synapse.social.studioasinc.shared.data.source.remote.CloudinaryUploadService
import com.synapse.social.studioasinc.shared.data.source.remote.ImgBBUploadService
import com.synapse.social.studioasinc.shared.data.source.remote.R2UploadService
import com.synapse.social.studioasinc.shared.data.source.remote.SupabaseUploadService
import com.synapse.social.studioasinc.shared.data.source.remote.UploadService
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.util.TimeProvider

class MediaUploadRepositoryImpl(
    private val fileUploader: FileUploader,
    private val imgBBUploadService: ImgBBUploadService,
    private val cloudinaryUploadService: CloudinaryUploadService,
    private val supabaseUploadService: SupabaseUploadService,
    private val r2UploadService: R2UploadService
) : MediaUploadRepository {

    override suspend fun upload(
        filePath: String,
        provider: StorageProvider,
        config: StorageConfig,
        bucketName: String?,
        onProgress: (Float) -> Unit
    ): Result<String> {
        return try {
            val fileSize = fileUploader.getFileSize(filePath)
            val fileName = fileUploader.getFileName(filePath).ifBlank { "upload_${TimeProvider.nowMillis()}" }

            val service = getUploadService(provider)
            val url = service.upload(
                fileProvider = { offset -> fileUploader.readFile(filePath, offset) },
                fileSize = fileSize,
                fileName = fileName,
                config = config,
                bucketName = bucketName,
                onProgress = onProgress
            )
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun deleteFile(filePath: String) {
        fileUploader.deleteFile(filePath)
    }

    private fun getUploadService(provider: StorageProvider): UploadService {
        return when (provider) {
            StorageProvider.DEFAULT -> throw IllegalStateException("Default provider should have been resolved")
            StorageProvider.IMGBB -> imgBBUploadService
            StorageProvider.CLOUDINARY -> cloudinaryUploadService
            StorageProvider.SUPABASE -> supabaseUploadService
            StorageProvider.CLOUDFLARE_R2 -> r2UploadService
        }
    }
}
