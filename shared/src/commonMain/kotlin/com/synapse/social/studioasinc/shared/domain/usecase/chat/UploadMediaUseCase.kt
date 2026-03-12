package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.data.FileUploader
import kotlinx.coroutines.flow.first

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

        return mediaUploadRepository.upload(
            filePath = filePath,
            provider = provider,
            config = config,
            bucketName = "chat_attachments",
            onProgress = { progressFloat ->
                onProgress?.invoke((progressFloat * 100).toInt())
            }
        )
    }
}
