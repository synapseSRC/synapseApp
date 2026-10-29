package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class GetMessagesUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, limit: Int = 50, before: String? = null, beforeId: String? = null, forceNetwork: Boolean = false): Result<List<Message>> {
        // Decryption is already handled at the repository layer (SupabaseChatRepository.decryptIfNecessary).
        // Do NOT decrypt again here — Signal Protocol's Double Ratchet consumes the key on first use,
        // so a second decryption attempt would fail and leave messages showing "Message is encrypted".
        return repository.getMessages(chatId, limit, before, beforeId, forceNetwork)
    }

    suspend fun after(chatId: String, createdAt: String, id: String, limit: Int = 50): Result<List<Message>> =
        repository.getMessagesAfter(chatId, createdAt, id, limit)

    suspend fun byIds(chatId: String, messageIds: List<String>): Result<List<Message>> =
        repository.getMessagesByIds(chatId, messageIds)

    suspend fun cacheFirst(chatId: String, limit: Int = 50, refreshInBackground: Boolean = false, before: String? = null, beforeId: String? = null): Result<List<Message>> =
        repository.getMessagesCacheFirst(chatId, limit, refreshInBackground, before, beforeId)
}
