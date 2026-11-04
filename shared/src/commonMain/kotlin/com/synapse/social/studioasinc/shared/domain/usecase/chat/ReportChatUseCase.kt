package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class ReportChatUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, reason: String): Result<Unit> {
        if (chatId.isBlank()) return Result.failure(IllegalArgumentException("Chat ID cannot be blank"))
        if (reason.isBlank()) return Result.failure(IllegalArgumentException("Report reason cannot be blank"))
        return repository.reportChat(chatId, reason.trim())
    }
}
