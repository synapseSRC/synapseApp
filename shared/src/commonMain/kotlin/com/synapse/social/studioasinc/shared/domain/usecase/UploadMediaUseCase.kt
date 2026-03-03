package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.domain.model.MediaType
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import kotlinx.coroutines.flow.first

class UploadMediaUseCase(
    private val repository: StorageRepository,
    private val mediaUploadRepository: MediaUploadRepository,
    private val mediaCompressor: MediaCompressor
) {
    suspend operator fun invoke(
        filePath: String,
        mediaType: MediaType,
        bucketName: String? = null,
        onProgress: (Float) -> Unit
    ): Result<String> {
        return try {
            val config = repository.getStorageConfig().first()

            var uploadPath = filePath
            var shouldDelete = false

            if ((mediaType == MediaType.PHOTO || mediaType == MediaType.IMAGE) && config.compressImages) {
                mediaCompressor.compress(filePath).onSuccess { compressedPath ->
                    if (compressedPath != filePath) {
                        uploadPath = compressedPath
                        shouldDelete = true
                    }
                }.onFailure {
                    // Compression failed, proceed with original
                }
            }

            try {
                val provider = getProviderForMediaType(config, mediaType)

                val providersToTry = getProvidersToTry(
                    selectedProvider = provider,
                    config = config,
                    mediaType = mediaType
                )

                if (providersToTry.isEmpty()) {
                    val errorMessage = if (provider != StorageProvider.DEFAULT) {
                        "Selected storage provider '$provider' is not configured for $mediaType upload. " +
                            "Please configure it in Settings → Storage Providers, or select 'Default' to use any available provider."
                    } else {
                        "No configured storage provider available for $mediaType upload. " +
                            "Please configure at least one provider (ImgBB, Cloudinary, Supabase, or Cloudflare R2) in Settings → Storage Providers."
                    }
                    return Result.failure(IllegalStateException(errorMessage))
                }

                val failures = mutableListOf<String>()
                providersToTry.forEach { providerToUse ->
                    val result = mediaUploadRepository.upload(
                        filePath = uploadPath,
                        provider = providerToUse,
                        config = config,
                        bucketName = bucketName,
                        onProgress = onProgress
                    )

                    if (result.isSuccess) {
                        return result
                    } else {
                        failures += "$providerToUse: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                    }
                }

                Result.failure(
                    Exception(
                        "Media upload failed for all configured providers. " +
                            failures.joinToString(separator = " | ")
                    )
                )
            } finally {
                // Not deleting here anymore, file deletion of compressed file
                // needs to be handled by FileUploader where we have access to it,
                // however since MediaCompressor returns a new path, we should probably
                // use MediaUploadRepository to delete the temporary compressed file.
                // Let's add delete to MediaUploadRepository.
                if (shouldDelete) {
                    mediaUploadRepository.deleteFile(uploadPath)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getProvidersToTry(
        selectedProvider: StorageProvider,
        config: StorageConfig,
        mediaType: MediaType
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
            if (config.isProviderConfigured(StorageProvider.IMGBB)) add(StorageProvider.IMGBB)
        }
    }

    private fun getProviderForMediaType(config: StorageConfig, mediaType: MediaType): StorageProvider {
        return when (mediaType) {
            MediaType.PHOTO, MediaType.IMAGE -> config.photoProvider
            MediaType.VIDEO -> config.videoProvider
            MediaType.OTHER -> config.otherProvider
            else -> config.otherProvider
        }
    }
}
