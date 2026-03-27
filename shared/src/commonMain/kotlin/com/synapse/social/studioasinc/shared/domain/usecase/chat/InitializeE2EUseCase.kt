package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class InitializeE2EUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.initializeE2EE()
    }
}
