package com.synapse.social.studioasinc.shared.data.sync

import kotlinx.serialization.Serializable

/**
 * Represents the synchronization state for a specific entity type.
 */
@Serializable
data class SyncState(
    val entityType: String,
    val lastSyncedAt: Long = 0L,
    val lastSyncVersion: Int = 0,
    val isSyncing: Boolean = false,
    val lastError: String? = null,
    val pendingCount: Int = 0
)

/**
 * Result of a sync operation containing both successful items and errors.
 */
sealed class SyncResult<out T> {
    data class Success<T>(val items: List<T>, val deletedIds: List<String> = emptyList()) : SyncResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult<Nothing>()
    data class Partial<T>(
        val items: List<T>,
        val failedIds: List<String>,
        val deletedIds: List<String> = emptyList()
    ) : SyncResult<T>()
}

/**
 * Represents an item that can be synchronized.
 * Domain models that participate in sync implement this via their syncers —
 * the interface itself lives in the data layer to stay off domain.
 */
interface SyncableEntity {
    val id: String
    val updatedAt: Long
    val isDeleted: Boolean
    val syncVersion: Int
}

