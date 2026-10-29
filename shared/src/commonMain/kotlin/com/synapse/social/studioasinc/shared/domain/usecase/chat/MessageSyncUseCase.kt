package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import kotlinx.datetime.Instant

class MessageSyncUseCase(
    private val getMessagesUseCase: GetMessagesUseCase
) {

    private val PAGE_SIZE = 50

    /**
     * Synchronizes missed messages for a chat starting from the last known message.
     * Returns new messages fetched from the server, paginating until no more are available.
     */
    suspend fun syncMissedMessages(
        chatId: String,
        lastConfirmedMessage: Message?
    ): Result<List<Message>> {
        val createdAt: String
        val cursorId: String

        if (lastConfirmedMessage != null) {
            createdAt = lastConfirmedMessage.createdAt
            cursorId = lastConfirmedMessage.id
        } else {
            // Empty chat: start from the beginning
            val initialResult = getMessagesUseCase(chatId, forceNetwork = true)
            if (initialResult.isFailure) {
                return initialResult
            }
            val initialMessages = initialResult.getOrDefault(emptyList())
            if (initialMessages.isEmpty()) {
                return Result.success(emptyList())
            }
            val last = initialMessages.last()
            createdAt = last.createdAt
            cursorId = last.id
            return Result.success(initialMessages)
        }

        val allMessages = mutableListOf<Message>()
        var currentCreatedAt = createdAt
        var currentId = cursorId

        while (true) {
            val pageResult = getMessagesUseCase.after(chatId, currentCreatedAt, currentId, PAGE_SIZE)
            if (pageResult.isFailure) {
                return pageResult
            }
            val page = pageResult.getOrDefault(emptyList())
            if (page.isEmpty()) {
                break
            }
            allMessages.addAll(page)
            val nextCursor = page.last()
            if (compareSyncCursor(nextCursor, currentCreatedAt, currentId) <= 0) {
                break
            }
            currentCreatedAt = nextCursor.createdAt
            currentId = nextCursor.id
        }

        return Result.success(allMessages)
    }

    private fun compareSyncCursor(message: Message, createdAt: String, id: String): Int {
        val timestampComparison = try {
            Instant.parse(message.createdAt).compareTo(Instant.parse(createdAt))
        } catch (_: Exception) {
            message.createdAt.compareTo(createdAt)
        }
        return if (timestampComparison != 0) timestampComparison else message.id.compareTo(id)
    }
}