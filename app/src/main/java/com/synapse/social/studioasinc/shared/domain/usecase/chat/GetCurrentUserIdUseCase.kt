package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import javax.inject.Inject

class GetCurrentUserIdUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): String? {
        return chatRepository.getCurrentUserId()
    }
}
