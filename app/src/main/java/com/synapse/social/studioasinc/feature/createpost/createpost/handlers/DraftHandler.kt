package com.synapse.social.studioasinc.feature.createpost.createpost.handlers

import android.app.Application
import android.content.Context
import javax.inject.Inject

class DraftHandler @Inject constructor(
    private val application: Application
) {
    private val prefs by lazy {
        application.getSharedPreferences("create_post_draft", Context.MODE_PRIVATE)
    }

    fun getDraftText(): String? {
        return prefs.getString("draft_text", null)
    }

    fun saveDraftText(text: String) {
        prefs.edit()
            .putString("draft_text", text)
            .apply()
    }

    fun clearDraft() {
        prefs.edit()
            .remove("draft_text")
            .apply()
    }
}
