package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class VoteInPollUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(messageId: String, optionId: String, pollId: String): Result<Unit> {
        return repository.voteInPoll(messageId, optionId, pollId)
    }
}