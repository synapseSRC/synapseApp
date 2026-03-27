package com.synapse.social.studioasinc.domain.usecase.reaction

import com.synapse.social.studioasinc.data.repository.ReactionRepository
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import javax.inject.Inject

class PopulateMessageReactionsUseCase @Inject constructor(
    private val repository: ReactionRepository
) {
    suspend operator fun invoke(messages: List<Message>): List<Message> {
        return repository.populateMessageReactions(messages)
    }
}
