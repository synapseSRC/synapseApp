package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import kotlinx.coroutines.flow.first

class UploadMediaUseCase(
    private val repository: ChatRepository,
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(
        chatId: String,
        filePath: String,
        fileName: String,
        contentType: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> {
        val config = storageRepository.getStorageConfig().first()
        val provider = when {
            contentType.startsWith("image/") -> config.photoProvider
            contentType.startsWith("video/") -> config.videoProvider
            else -> config.otherProvider
        }
        val finalFileName = "chat_media/$chatId/$fileName"
        return repository.uploadMedia(
            chatId = chatId,
            filePath = filePath,
            fileName = finalFileName,
            contentType = contentType,
            provider = provider,
            config = config,
            onProgress = onProgress
        )
    }
}
