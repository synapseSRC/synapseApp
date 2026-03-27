package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import kotlinx.coroutines.flow.first
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider

class UploadMediaUseCase(
    private val repository: ChatRepository,
    private val storageRepository: StorageRepository,
    private val mediaUploadRepository: MediaUploadRepository,
    private val fileUploader: FileUploader
) {
    suspend operator fun invoke(
        chatId: String,
        filePath: String,
        fileName: String,
        contentType: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> {
        val config = storageRepository.getStorageConfig().first()
        val isImage = contentType.startsWith("image/")
        val isVideo = contentType.startsWith("video/")

        val sizeInBytes = fileUploader.getFileSize(filePath)
        val mb = 1024 * 1024L
        if (isVideo && sizeInBytes > 50 * mb) {
            return Result.failure(Exception("Video exceeds 50MB limit"))
        }
        if (isImage && sizeInBytes > 10 * mb) {
            return Result.failure(Exception("Image exceeds 10MB limit"))
        }

        val provider = when {
            isImage -> config.photoProvider
            isVideo -> config.videoProvider
            else -> config.otherProvider
        }

        val providersToTry = getProvidersToTry(
            selectedProvider = provider,
            config = config,
            isImage = isImage
        )

        if (providersToTry.isEmpty()) {
            val mediaType = if (isImage) "image" else if (isVideo) "video" else "file"
            val errorMessage = if (provider != StorageProvider.DEFAULT) {
                "Selected storage provider '\$provider' is not configured for \$mediaType upload. " +
                    "Please configure it in Settings -> Storage Providers, or select 'Default' to use any available provider."
            } else {
                "No configured storage provider available for \$mediaType upload. " +
                    "Please configure at least one provider (ImgBB, Cloudinary, Supabase, or Cloudflare R2) in Settings -> Storage Providers."
            }
            return Result.failure(IllegalStateException(errorMessage))
        }

        val failures = mutableListOf<String>()
        for (providerToUse in providersToTry) {
            val result = mediaUploadRepository.upload(
                filePath = filePath,
                provider = providerToUse,
                config = config,
                bucketName = "chat_attachments",
                onProgress = { progressFloat ->
                    onProgress?.invoke((progressFloat * 100).toInt())
                }
            )

            if (result.isSuccess) {
                return result
            } else {
                failures.add("$providerToUse: " + (result.exceptionOrNull()?.message ?: "Unknown error"))
            }
        }

        return Result.failure(
            Exception(
                "Media upload failed for all configured providers. " +
                    failures.joinToString(separator = " | ")
            )
        )
    }

    private fun getProvidersToTry(
        selectedProvider: StorageProvider,
        config: StorageConfig,
        isImage: Boolean
    ): List<StorageProvider> {
        if (selectedProvider != StorageProvider.DEFAULT) {
            return if (config.isProviderConfigured(selectedProvider)) {
                listOf(selectedProvider)
            } else {
                emptyList()
            }
        }

        return buildList {
            if (config.isProviderConfigured(StorageProvider.CLOUDFLARE_R2)) add(StorageProvider.CLOUDFLARE_R2)
            if (config.isProviderConfigured(StorageProvider.CLOUDINARY)) add(StorageProvider.CLOUDINARY)
            if (config.isProviderConfigured(StorageProvider.SUPABASE)) add(StorageProvider.SUPABASE)

            if (isImage && config.isProviderConfigured(StorageProvider.IMGBB)) {
                add(StorageProvider.IMGBB)
            }
        }
    }
}
