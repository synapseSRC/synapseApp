package com.synapse.social.studioasinc.domain.usecase.chat

import com.synapse.social.studioasinc.domain.repository.ChatDraftRepository
import javax.inject.Inject

/** Stores the composer's unsent text for one conversation. */
class SaveChatDraftUseCase @Inject constructor(
    private val repository: ChatDraftRepository
) {
    operator fun invoke(chatId: String, text: String) = repository.saveDraft(chatId, text)
}
