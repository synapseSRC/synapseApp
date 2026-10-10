package com.synapse.social.studioasinc.data.repository

import android.app.Application
import android.content.Context
import com.synapse.social.studioasinc.domain.repository.DraftRepository
import javax.inject.Inject

class DraftRepositoryImpl @Inject constructor(
    private val application: Application
) : DraftRepository {
    private val prefs by lazy {
        application.getSharedPreferences("create_post_draft", Context.MODE_PRIVATE)
    }

    override fun getDraftText(): String? {
        return prefs.getString("draft_text", null)
    }

    override fun saveDraftText(text: String) {
        prefs.edit()
            .putString("draft_text", text)
            .apply()
    }

    override fun clearDraft() {
        prefs.edit()
            .remove("draft_text")
            .apply()
    }
}
