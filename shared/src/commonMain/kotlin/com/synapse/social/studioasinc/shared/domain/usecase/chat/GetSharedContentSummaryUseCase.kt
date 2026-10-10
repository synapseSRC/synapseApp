package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

data class SharedContentSummary(
    val mediaCount: Int,
    val linkCount: Int,
    val fileCount: Int,
    val isSample: Boolean = false
)

class GetSharedContentSummaryUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String): Result<SharedContentSummary> {
        if (chatId.isBlank()) {
            return Result.success(SharedContentSummary(0, 0, 0, false))
        }
        return repository.getMessages(chatId, limit = 100).map { messages ->
            val media = messages.count { it.messageType == MessageType.IMAGE || it.messageType == MessageType.VIDEO }
            val links = messages.count { it.linkPreview != null || it.content.contains("http://") || it.content.contains("https://") }
            val files = messages.count { it.messageType == MessageType.FILE || it.messageType == MessageType.AUDIO }
            SharedContentSummary(
                mediaCount = media,
                linkCount = links,
                fileCount = files,
                isSample = messages.size >= 100
            )
        }
    }
}
