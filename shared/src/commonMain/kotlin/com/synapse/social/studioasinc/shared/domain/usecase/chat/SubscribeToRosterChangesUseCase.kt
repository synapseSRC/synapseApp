package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes changes to the current user's own chat participation. Emits the chat id when the user
 * is added to a chat (including a brand-new group without any messages yet) or removed from one,
 * letting the inbox refresh itself without waiting for the first message.
 */
class SubscribeToRosterChangesUseCase(private val repository: ChatRepository) {
    operator fun invoke(): Flow<String> = repository.subscribeToRosterChanges()
}
