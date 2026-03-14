package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.Draft
import com.synapse.social.studioasinc.shared.domain.model.DraftType
import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    suspend fun saveDraft(draft: Draft)
    suspend fun deleteDraftById(id: String)
    suspend fun deleteDraftByType(type: DraftType)
    suspend fun getDraftByType(type: DraftType): Draft?
    suspend fun getAllDrafts(): List<Draft>
    fun getDraftByTypeAsFlow(type: DraftType): Flow<Draft?>
}
