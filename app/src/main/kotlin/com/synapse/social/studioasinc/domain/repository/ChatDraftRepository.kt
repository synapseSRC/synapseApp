package com.synapse.social.studioasinc.domain.repository

/**
 * Local storage for the chat composer's unsent text.
 *
 * A draft belongs to the conversation it was typed in, so every operation is keyed by chat id and
 * one conversation's text can never be read back for another. Only the composer writes here; the
 * conversation itself is owned by the chat repository.
 */
interface ChatDraftRepository {

    /** The unsent text last stored for [chatId], or null when that conversation has no draft. */
    fun getDraft(chatId: String): String?

    /** Stores [text] as the unsent text of [chatId]. */
    fun saveDraft(chatId: String, text: String)

    /** Drops the draft of [chatId], for text that was sent or deleted by the user. */
    fun clearDraft(chatId: String)
}
