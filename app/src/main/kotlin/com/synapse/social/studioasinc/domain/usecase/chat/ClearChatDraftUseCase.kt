package com.synapse.social.studioasinc.domain.usecase.chat

import com.synapse.social.studioasinc.domain.repository.ChatDraftRepository
import javax.inject.Inject

/** Drops the stored draft of one conversation once its text is no longer unsent. */
class ClearChatDraftUseCase @Inject constructor(
    private val repository: ChatDraftRepository
) {
    operator fun invoke(chatId: String) = repository.clearDraft(chatId)
}
