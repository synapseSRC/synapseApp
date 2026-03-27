package com.synapse.social.studioasinc.domain.repository

interface DraftRepository {
    fun getDraftText(): String?
    fun saveDraftText(text: String)
    fun clearDraft()
}
