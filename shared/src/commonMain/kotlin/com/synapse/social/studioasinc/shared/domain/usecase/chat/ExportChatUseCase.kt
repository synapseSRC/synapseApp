package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.util.TimestampFormatter

class ExportChatUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String): Result<String> {
        return chatRepository.getMessages(chatId, limit = 1000).map { messages ->
            formatMessagesAsText(messages)
        }
    }

    private fun formatMessagesAsText(messages: List<Message>): String {
        return buildString {
            appendLine("=== Chat Export ===")
            appendLine("Total messages: ${messages.size}")
            appendLine()

            messages.forEach { message ->
                val timestamp = TimestampFormatter.formatRelative(message.createdAt)
                val sender = message.senderId.takeIf { it.isNotBlank() } ?: "Unknown"
                val content = message.content
                val mediaInfo = message.attachments?.firstOrNull()?.let { 
                    "[Media: ${it.type}]"
                } ?: ""

                appendLine("[$timestamp] $sender:")
                if (content.isNotBlank()) {
                    appendLine(content)
                }
                if (mediaInfo.isNotBlank()) {
                    appendLine(mediaInfo)
                }
                appendLine()
            }

            appendLine("=== End of Export ===")
        }
    }
}
