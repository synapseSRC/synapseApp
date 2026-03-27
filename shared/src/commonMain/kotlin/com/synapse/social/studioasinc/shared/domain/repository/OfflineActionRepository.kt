package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import kotlinx.coroutines.flow.Flow

interface OfflineActionRepository {
    suspend fun addAction(action: PendingAction)
    suspend fun removeAction(id: String)
    suspend fun getPendingActions(): List<PendingAction>
    suspend fun updateAction(id: String, retryCount: Int, lastAttemptAt: Long?)
    fun getPendingActionsFlow(): Flow<List<PendingAction>>
}
