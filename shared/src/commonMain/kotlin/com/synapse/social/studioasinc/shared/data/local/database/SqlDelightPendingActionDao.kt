package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import com.synapse.social.studioasinc.shared.data.database.PendingAction as DbPendingAction

class SqlDelightPendingActionDao(
    private val db: StorageDatabase
) : PendingActionDao {

    override suspend fun insert(action: PendingAction) {
        withContext(Dispatchers.IO) {
            db.pendingActionQueries.insertAction(toDbAction(action))
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            db.pendingActionQueries.deleteAction(id)
        }
    }

    override suspend fun getAll(): List<PendingAction> = withContext(Dispatchers.IO) {
        db.pendingActionQueries.selectAllActions().executeAsList().map { toDomain(it) }
    }

    override suspend fun update(id: String, retryCount: Int, lastAttemptAt: Long?) {
        withContext(Dispatchers.IO) {
            db.pendingActionQueries.updateAction(retryCount, lastAttemptAt, id)
        }
    }

    override fun getAllAsFlow(): Flow<List<PendingAction>> {
        return db.pendingActionQueries.selectAllActions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { toDomain(it) } }
    }

    private fun toDomain(dbAction: DbPendingAction): PendingAction {
        return PendingAction(
            id = dbAction.id,
            actionType = PendingAction.ActionType.valueOf(dbAction.actionType),
            targetId = dbAction.targetId,
            payload = dbAction.payload,
            createdAt = dbAction.createdAt,
            retryCount = dbAction.retryCount,
            lastAttemptAt = dbAction.lastAttemptAt
        )
    }

    private fun toDbAction(action: PendingAction): DbPendingAction {
        return DbPendingAction(
            id = action.id,
            actionType = action.actionType.name,
            targetId = action.targetId,
            payload = action.payload,
            createdAt = action.createdAt,
            retryCount = action.retryCount,
            lastAttemptAt = action.lastAttemptAt
        )
    }
}
