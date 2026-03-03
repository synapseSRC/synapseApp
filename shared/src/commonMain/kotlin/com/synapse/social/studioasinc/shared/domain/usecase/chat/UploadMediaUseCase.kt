package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class UploadMediaUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(
        chatId: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String
    ): Result<String> = repository.uploadMedia(chatId, fileBytes, fileName, contentType)
}
