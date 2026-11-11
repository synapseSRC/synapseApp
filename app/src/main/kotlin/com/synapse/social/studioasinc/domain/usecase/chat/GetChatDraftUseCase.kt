package com.synapse.social.studioasinc.domain.usecase.chat

import com.synapse.social.studioasinc.domain.repository.ChatDraftRepository
import javax.inject.Inject

/** Reads the unsent text stored for one conversation, if any. */
class GetChatDraftUseCase @Inject constructor(
    private val repository: ChatDraftRepository
) {
    operator fun invoke(chatId: String): String? = repository.getDraft(chatId)
}
