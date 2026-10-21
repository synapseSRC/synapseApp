package com.synapse.social.studioasinc.shared.data.sync

import com.synapse.social.studioasinc.shared.domain.model.PendingAction

/**
 * Interface for entity-specific synchronization logic.
 * Supabase is the authoritative source — remote changes always win unless the item has
 * an unsynced local change (SENDING / pending queue).
 */
interface EntitySyncer<T : SyncableEntity> {

    val entityType: String
    val tableName: String

    /**
     * Pull changes from remote since [lastSyncedAt].
     * Merges remote into local cache; marks locally-deleted any items missing from the
     * remote window so they never reappear from stale cache.
     */
    suspend fun pullFromRemote(lastSyncedAt: Long): SyncResult<T>

    /**
     * Push a single pending local change to remote.
     * Returns [Result.failure] for any network error — a network failure must never
     * be mistaken for a successful delete.
     */
    suspend fun pushChange(action: PendingAction): Result<Unit>

    /** Handle a Supabase Realtime INSERT for this entity. */
    suspend fun handleRemoteInsert(record: Map<String, Any?>)

    /** Handle a Supabase Realtime UPDATE for this entity. */
    suspend fun handleRemoteUpdate(record: Map<String, Any?>)

    /**
     * Handle a Supabase Realtime DELETE for this entity.
     * Uses soft-delete locally so the record cannot reappear from cache.
     */
    suspend fun handleRemoteDelete(id: String)

    /** Clear all local data — called on logout / account switch. */
    suspend fun clearLocal()
}

/**
 * Result of reconciling a local snapshot with a remote window.
 */
data class ReconciliationResult<T>(
    val toInsert: List<T>,
    val toUpdate: List<T>,
    val toDelete: List<String>
)

/**
 * Base implementation providing the reconciliation algorithm.
 * Concrete syncers only need to supply fetch / persist operations.
 */
abstract class BaseEntitySyncer<T : SyncableEntity>(
    override val entityType: String,
    override val tableName: String
) : EntitySyncer<T> {

    /** Fetch items from Supabase that changed since [sinceTimestamp]. */
    protected abstract suspend fun fetchFromRemote(sinceTimestamp: Long): List<T>

    /** Return all locally-cached items for this entity type. */
    protected abstract suspend fun getLocalItems(): List<T>

    /** Persist (upsert) the given items to local cache. */
    protected abstract suspend fun saveLocal(items: List<T>)

    /** Hard-delete a single item from local cache. */
    protected abstract suspend fun deleteLocal(id: String)

    /**
     * Soft-delete the given IDs in local cache.
     * Soft-delete is preferred over hard-delete to prevent reappearance from stale cache.
     */
    protected abstract suspend fun markAsDeletedLocally(ids: List<String>)

    /**
     * Return true if this local-only item should be pushed to remote
     * (e.g. message still in SENDING state).
     */
    protected abstract suspend fun shouldPushToRemote(local: T): Boolean

    /**
     * Convert a Supabase Realtime payload map into the entity type.
     * Return null if the record cannot be parsed.
     */
    protected abstract fun mapFromRealtimeRecord(record: Map<String, Any?>): T?

    // ── Core pull ─────────────────────────────────────────────────────────

    override suspend fun pullFromRemote(lastSyncedAt: Long): SyncResult<T> = try {
        val remote = fetchFromRemote(lastSyncedAt)
        val local  = getLocalItems()

        val result = reconcile(local, remote)

        if (result.toInsert.isNotEmpty()) saveLocal(result.toInsert)
        if (result.toUpdate.isNotEmpty()) saveLocal(result.toUpdate)
        if (result.toDelete.isNotEmpty()) markAsDeletedLocally(result.toDelete)

        SyncResult.Success(
            items = remote.filter { !it.isDeleted },
            deletedIds = result.toDelete
        )
    } catch (e: Exception) {
        SyncResult.Error("Failed to pull $entityType: ${e.message}", e)
    }

    // ── Reconciliation ────────────────────────────────────────────────────

    /**
     * Reconcile [local] snapshot against the authoritative [remote] window.
     *
     * Rules:
     * - Remote item not in local  → insert.
     * - Remote item in local, remote is newer → update.
     * - Remote marks isDeleted    → soft-delete locally.
     * - Local item missing from remote window → soft-delete (was deleted server-side),
     *   unless it still has a pending local change (e.g. SENDING).
     */
    private suspend fun reconcile(local: List<T>, remote: List<T>): ReconciliationResult<T> {
        val localById  = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }

        val toInsert = mutableListOf<T>()
        val toUpdate = mutableListOf<T>()
        val toDelete = mutableListOf<String>()

        // Process every remote item
        for (remoteItem in remote) {
            val localItem = localById[remoteItem.id]
            when {
                remoteItem.isDeleted -> toDelete.add(remoteItem.id)
                localItem == null    -> toInsert.add(remoteItem)
                remoteItem.updatedAt > localItem.updatedAt -> toUpdate.add(remoteItem)
                // local is same age or newer (pending local edit) — leave it alone
            }
        }

        // Items only in local that are absent from the remote window — deleted server-side
        for (localItem in local) {
            if (localItem.id !in remoteById && !localItem.isDeleted) {
                // Don't delete if the item has a pending local change
                if (!shouldPushToRemote(localItem)) {
                    toDelete.add(localItem.id)
                }
            }
        }

        return ReconciliationResult(toInsert, toUpdate, toDelete.distinct())
    }

    // ── Realtime handlers ─────────────────────────────────────────────────

    override suspend fun handleRemoteInsert(record: Map<String, Any?>) {
        val entity = mapFromRealtimeRecord(record) ?: return
        val existing = getLocalItems().find { it.id == entity.id }
        // Supabase is authoritative — insert/update unless local is newer with pending changes
        if (existing == null || entity.updatedAt >= existing.updatedAt) {
            saveLocal(listOf(entity))
        }
    }

    override suspend fun handleRemoteUpdate(record: Map<String, Any?>) {
        val entity = mapFromRealtimeRecord(record) ?: return
        val existing = getLocalItems().find { it.id == entity.id }
        if (existing == null || entity.updatedAt >= existing.updatedAt) {
            saveLocal(listOf(entity))
        }
    }

    override suspend fun handleRemoteDelete(id: String) {
        // Soft-delete to prevent reappearance from stale cache
        markAsDeletedLocally(listOf(id))
    }
}
