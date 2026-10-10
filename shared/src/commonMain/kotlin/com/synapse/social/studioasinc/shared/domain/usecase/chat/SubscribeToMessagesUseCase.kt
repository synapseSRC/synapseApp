package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class SubscribeToMessagesUseCase(
    private val repository: ChatRepository
) {
    operator fun invoke(chatId: String): Flow<Message> {
        // Decryption is already handled at the repository layer (SupabaseChatRepository.subscribeToMessages).
        // Do NOT decrypt again here — Signal Protocol's Double Ratchet consumes the key on first use,
        // so a second decryption attempt would fail and leave messages showing "Message is encrypted".
        return repository.subscribeToMessages(chatId)
    }
}
