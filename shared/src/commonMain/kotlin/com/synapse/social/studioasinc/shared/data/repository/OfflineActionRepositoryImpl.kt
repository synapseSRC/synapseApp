package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.local.database.PendingActionDao
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import kotlinx.coroutines.flow.Flow

class OfflineActionRepositoryImpl(
    private val pendingActionDao: PendingActionDao
) : OfflineActionRepository {

    override suspend fun addAction(action: PendingAction) {
        pendingActionDao.insert(action)
    }

    override suspend fun removeAction(id: String) {
        pendingActionDao.delete(id)
    }

    override suspend fun getPendingActions(): List<PendingAction> {
        return pendingActionDao.getAll()
    }

    override suspend fun updateAction(id: String, retryCount: Int, lastAttemptAt: Long?) {
        pendingActionDao.update(id, retryCount, lastAttemptAt)
    }

    override fun getPendingActionsFlow(): Flow<List<PendingAction>> {
        return pendingActionDao.getAllAsFlow()
    }
}
