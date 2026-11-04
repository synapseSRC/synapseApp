package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.util.EncryptedPayloadDetector
import com.synapse.social.studioasinc.shared.util.TimestampFormatter

class ExportChatUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String): Result<String> {
        if (chatId.isBlank()) return Result.failure(IllegalArgumentException("Chat ID cannot be blank"))

        val senderNames = chatRepository.getGroupMembers(chatId).getOrNull()
            ?.mapNotNull { (user, _) ->
                val userId = user.uid.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val name = user.displayName ?: user.name ?: user.username ?: user.nickname
                name?.takeIf { it.isNotBlank() }?.let { userId to it }
            }
            ?.toMap()
            .orEmpty()

        return chatRepository
            .getMessages(chatId, limit = MAX_EXPORTED_MESSAGES, forceNetwork = true)
            .map { messages -> formatMessagesAsText(messages, senderNames) }
    }

    private fun formatMessagesAsText(messages: List<Message>, senderNames: Map<String, String>): String {
        return buildString {
            appendLine("=== Chat Export ===")
            appendLine("Total messages: ${messages.size}")
            appendLine()

            messages.forEach { message ->
                val timestamp = TimestampFormatter.formatRelative(message.createdAt)
                val sender = message.senderId
                    .takeIf { it.isNotBlank() }
                    ?.let { senderNames[it] ?: it }
                    ?: "Unknown"

                appendLine("[$timestamp] $sender:")
                if (message.isDeleted) {
                    appendLine("[Message deleted]")
                } else if (EncryptedPayloadDetector.isUndecryptable(message.content, message.encryptionFailureReason)) {
                    appendLine("[Encrypted message unavailable]")
                } else {
                    if (message.content.isNotBlank()) {
                        appendLine(message.content)
                    }
                    message.attachments.firstOrNull()?.let {
                        appendLine("[Media: ${it.type}]")
                    }
                }
                appendLine()
            }

            appendLine("=== End of Export ===")
        }
    }

    private companion object {
        const val MAX_EXPORTED_MESSAGES = 1000
    }
}
