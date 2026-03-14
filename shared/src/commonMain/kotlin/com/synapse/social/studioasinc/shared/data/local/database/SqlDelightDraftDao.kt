package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.Draft
import com.synapse.social.studioasinc.shared.domain.model.DraftType
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import com.synapse.social.studioasinc.shared.data.database.Draft as DbDraft

class SqlDelightDraftDao(
    private val db: StorageDatabase
) : DraftDao {

    override suspend fun insert(draft: Draft) = withContext(Dispatchers.IO) {
        db.draftQueries.insertDraft(toDbDraft(draft))
    }

    override suspend fun deleteById(id: String) = withContext(Dispatchers.IO) {
        db.draftQueries.deleteDraftById(id)
    }

    override suspend fun deleteByType(type: DraftType) = withContext(Dispatchers.IO) {
        db.draftQueries.deleteDraftByType(type.name)
    }

    override suspend fun getByType(type: DraftType): List<Draft> = withContext(Dispatchers.IO) {
        db.draftQueries.selectDraftByType(type.name).executeAsList().map { toDomain(it) }
    }

    override suspend fun getAll(): List<Draft> = withContext(Dispatchers.IO) {
        db.draftQueries.selectAllDrafts().executeAsList().map { toDomain(it) }
    }

    override fun getByTypeAsFlow(type: DraftType): Flow<List<Draft>> {
        return db.draftQueries.selectDraftByType(type.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { toDomain(it) } }
    }

    private fun toDomain(dbDraft: DbDraft): Draft {
        return Draft(
            id = dbDraft.id,
            type = DraftType.valueOf(dbDraft.type),
            content = dbDraft.content,
            updatedAt = dbDraft.updatedAt
        )
    }

    private fun toDbDraft(draft: Draft): DbDraft {
        return DbDraft(
            id = draft.id,
            type = draft.type.name,
            content = draft.content,
            updatedAt = draft.updatedAt
        )
    }
}
