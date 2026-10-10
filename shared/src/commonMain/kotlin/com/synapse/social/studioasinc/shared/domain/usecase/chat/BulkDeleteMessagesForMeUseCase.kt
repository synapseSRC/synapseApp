package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class BulkDeleteMessagesForMeUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(messageIds: List<String>): Result<Unit> = repository.deleteMessagesForMe(messageIds)
}
