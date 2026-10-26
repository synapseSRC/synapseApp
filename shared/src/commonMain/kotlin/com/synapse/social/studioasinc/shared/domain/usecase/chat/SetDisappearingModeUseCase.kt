package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class SetDisappearingModeUseCase(
    private val repository: ChatRepository,
    private val addSystemMessageUseCase: AddSystemMessageUseCase
) {
    suspend operator fun invoke(chatId: String, mode: DisappearingMode): Result<Unit> {
        val result = repository.setDisappearingMode(chatId, mode)
        if (result.isSuccess) {
            // System message content - these are hardcoded in shared module since it's platform-agnostic
            // In a production app, these would be localized strings passed from the UI layer
            val systemMessageContent = when (mode) {
                DisappearingMode.OFF -> "Disappearing messages turned off"
                DisappearingMode.TWENTY_FOUR_HOURS -> "Disappearing messages set to 24 hours"
                DisappearingMode.SEVEN_DAYS -> "Disappearing messages set to 7 days"
                DisappearingMode.NINETY_DAYS -> "Disappearing messages set to 90 days"
            }
            addSystemMessageUseCase(chatId, systemMessageContent)
        }
        return result
    }
}
