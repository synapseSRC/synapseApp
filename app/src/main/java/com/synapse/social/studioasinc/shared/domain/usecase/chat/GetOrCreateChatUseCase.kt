package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import javax.inject.Inject

class GetOrCreateChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(participantId: String): Result<String> {
        return chatRepository.getOrCreateChat(participantId)
    }
}
