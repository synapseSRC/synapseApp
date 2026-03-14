package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.local.database.DraftDao
import com.synapse.social.studioasinc.shared.domain.model.Draft
import com.synapse.social.studioasinc.shared.domain.model.DraftType
import com.synapse.social.studioasinc.shared.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DraftRepositoryImpl(
    private val draftDao: DraftDao
) : DraftRepository {

    override suspend fun saveDraft(draft: Draft) {
        draftDao.insert(draft)
    }

    override suspend fun deleteDraftById(id: String) {
        draftDao.deleteById(id)
    }

    override suspend fun deleteDraftByType(type: DraftType) {
        draftDao.deleteByType(type)
    }

    override suspend fun getDraftByType(type: DraftType): Draft? {
        return draftDao.getByType(type).firstOrNull()
    }

    override suspend fun getAllDrafts(): List<Draft> {
        return draftDao.getAll()
    }

    override fun getDraftByTypeAsFlow(type: DraftType): Flow<Draft?> {
        return draftDao.getByTypeAsFlow(type).map { it.firstOrNull() }
    }
}
