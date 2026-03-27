package com.synapse.social.studioasinc.shared.domain.usecase.presence

import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository

class UpdateCurrentChatUseCase(
    private val presenceRepository: PresenceRepository
) {
    suspend operator fun invoke(chatId: String?) = presenceRepository.updatePresence(
        isOnline = true,
        currentChatId = chatId
    )
}
