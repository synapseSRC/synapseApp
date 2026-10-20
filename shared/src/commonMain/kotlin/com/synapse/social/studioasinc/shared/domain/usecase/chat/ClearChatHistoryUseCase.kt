package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

/** Clears the current user's view of a conversation and removes its messages from the local cache. */
class ClearChatHistoryUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String): Result<Unit> {
        if (chatId.isBlank()) return Result.failure(IllegalArgumentException("Chat ID cannot be blank"))

        val processedIds = mutableSetOf<String>()
        var before: String? = null
        var beforeId: String? = null
        var pagesFetched = 0

        while (true) {
            if (pagesFetched >= MAX_PAGES) {
                return Result.failure(IllegalStateException("Chat history pagination exceeded the maximum page count"))
            }
            pagesFetched++
            val pageResult = repository.getMessages(
                chatId = chatId,
                limit = PAGE_SIZE,
                before = before,
                beforeId = beforeId,
                forceNetwork = true
            )
            if (pageResult.isFailure) {
                return Result.failure(
                    pageResult.exceptionOrNull() ?: IllegalStateException("Chat history could not be loaded")
                )
            }
            val page = pageResult.getOrNull().orEmpty()
            if (page.isEmpty()) return Result.success(Unit)

            val newIds = page.asSequence().map(Message::id).filterNot(processedIds::contains).distinct().toList()
            if (newIds.isEmpty()) {
                return Result.failure(IllegalStateException("Chat history pagination did not advance"))
            }

            val deleteResult = repository.deleteMessagesForMe(newIds)
            if (deleteResult.isFailure) {
                return Result.failure(
                    deleteResult.exceptionOrNull() ?: IllegalStateException("Chat history could not be cleared")
                )
            }
            processedIds += newIds

            val oldest = page.minWithOrNull(compareBy<Message> { it.createdAt }.thenBy { it.id })
                ?: return Result.success(Unit)
            if (oldest.createdAt == before && oldest.id == beforeId) {
                return Result.failure(IllegalStateException("Chat history pagination did not advance"))
            }
            before = oldest.createdAt
            beforeId = oldest.id
        }
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 1_000
    }
}
