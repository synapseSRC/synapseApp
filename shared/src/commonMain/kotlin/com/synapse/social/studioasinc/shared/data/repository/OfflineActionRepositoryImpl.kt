package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.local.database.PendingActionDao
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of [OfflineActionRepository] that manages the persistence and retrieval
 * of failed network operations to ensure eventual consistency in an offline-first architecture.
 *
 * This repository acts as a bridge between the local database and the background synchronization
 * workers that process [PendingAction] objects.
 */
class OfflineActionRepositoryImpl(
    private val pendingActionDao: PendingActionDao
) : OfflineActionRepository {

    /**
     * Persists a new pending action to the local database for later retry.
     */
    override suspend fun addAction(action: PendingAction) {
        pendingActionDao.insert(action)
    }

    /**
     * Deletes a pending action once it has been successfully synchronized or abandoned.
     */
    override suspend fun removeAction(id: String) {
        pendingActionDao.delete(id)
    }

    /**
     * Retrieves all currently queued actions for the next synchronization pass.
     */
    override suspend fun getPendingActions(): List<PendingAction> {
        return pendingActionDao.getAll()
    }

    /**
     * Updates retry metadata for a specific action to track synchronization progress and
     * apply exponential backoff strategies in callers.
     */
    override suspend fun updateAction(id: String, retryCount: Int, lastAttemptAt: Long?) {
        pendingActionDao.update(id, retryCount, lastAttemptAt)
    }

    /**
     * Provides a real-time stream of pending actions for reactive UI updates or
     * active background workers.
     */
    override fun getPendingActionsFlow(): Flow<List<PendingAction>> {
        return pendingActionDao.getAllAsFlow()
    }
}
