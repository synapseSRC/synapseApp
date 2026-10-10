package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class GetDisappearingModeUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String): Result<DisappearingMode> {
        return repository.getDisappearingMode(chatId)
    }
}
