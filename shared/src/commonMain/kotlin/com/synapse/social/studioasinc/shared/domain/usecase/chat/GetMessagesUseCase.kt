package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class GetMessagesUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, limit: Int = 50, before: String? = null): Result<List<Message>> {
        // Decryption is already handled at the repository layer (SupabaseChatRepository.decryptIfNecessary).
        // Do NOT decrypt again here — Signal Protocol's Double Ratchet consumes the key on first use,
        // so a second decryption attempt would fail and leave messages showing "Message is encrypted".
        return repository.getMessages(chatId, limit, before)
    }
}
