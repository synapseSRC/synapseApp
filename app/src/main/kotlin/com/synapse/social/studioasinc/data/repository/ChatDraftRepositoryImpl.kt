package com.synapse.social.studioasinc.data.repository

import android.app.Application
import android.content.Context
import com.synapse.social.studioasinc.domain.repository.ChatDraftRepository
import javax.inject.Inject

/**
 * Stores each conversation's draft under its chat id in its own preferences file, separate from the
 * create-post draft so the two never overwrite each other.
 */
class ChatDraftRepositoryImpl @Inject constructor(
    private val application: Application
) : ChatDraftRepository {
    private val prefs by lazy {
        application.getSharedPreferences("chat_message_drafts", Context.MODE_PRIVATE)
    }

    override fun getDraft(chatId: String): String? {
        return prefs.getString(chatId, null)
    }

    override fun saveDraft(chatId: String, text: String) {
        prefs.edit()
            .putString(chatId, text)
            .apply()
    }

    override fun clearDraft(chatId: String) {
        prefs.edit()
            .remove(chatId)
            .apply()
    }
}
