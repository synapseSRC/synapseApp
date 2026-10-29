package com.synapse.social.studioasinc.shared.data.local

import com.synapse.social.studioasinc.shared.data.sync.SyncState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persistence contract for sync state metadata (last-synced timestamps etc.).
 */
interface SyncStateStorage {
    suspend fun save(state: SyncState)
    suspend fun load(entityType: String): SyncState?
    suspend fun loadAll(): List<SyncState>
    suspend fun clear(entityType: String)
    suspend fun clearAll()
}

/**
 * In-memory implementation — adequate for the current app lifetime.
 * Replace with a SQLDelight or Settings-backed variant for cross-restart durability.
 */
class InMemorySyncStateStorage : SyncStateStorage {

    private val mutex = Mutex()
    private val states = mutableMapOf<String, SyncState>()

    override suspend fun save(state: SyncState): Unit = mutex.withLock {
        states[state.entityType] = state
    }

    override suspend fun load(entityType: String): SyncState? = mutex.withLock {
        states[entityType]
    }

    override suspend fun loadAll(): List<SyncState> = mutex.withLock {
        states.values.toList()
    }

    override suspend fun clear(entityType: String): Unit = mutex.withLock {
        states.remove(entityType)
    }

    override suspend fun clearAll(): Unit = mutex.withLock {
        states.clear()
    }
}
