package com.synapse.social.studioasinc.shared.data.sync

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.local.SyncStateStorage
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.shared.util.Logger
import com.synapse.social.studioasinc.shared.util.TimeProvider
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

/**
 * Central coordinator for synchronizing the local SQLDelight database with Supabase.
 *
 * Responsibilities:
 * - Maintain per-entity sync state
 * - Push pending local changes before pulling remote changes
 * - Subscribe to Supabase Realtime for live updates
 * - Handle reconnection and retry with back-off
 * - Clear all local data on logout / account switch
 *
 * Supabase is the **authoritative** source for all shared data.
 */
class SyncManager(
    private val supabaseClient: SupabaseClient,
    private val syncStateStorage: SyncStateStorage,
    private val offlineActionRepository: OfflineActionRepository? = null
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val RECONNECT_DELAY_MS = 5_000L
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + AppDispatchers.IO)

    private val entitySyncers = mutableMapOf<String, EntitySyncer<*>>()
    private val realtimeChannels = mutableMapOf<String, RealtimeChannel>()
    private val reconnectJobs = mutableMapOf<String, Job>()

    private val _syncStates = MutableStateFlow<Map<String, SyncState>>(emptyMap())

    private val _syncEvents = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 64)

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    @Volatile
    var currentUserId: String? = null
        private set

    // ── Registration ──────────────────────────────────────────────────────

    fun <T : SyncableEntity> registerSyncer(entityType: String, syncer: EntitySyncer<T>) {
        entitySyncers[entityType] = syncer
        Napier.d("Registered syncer: $entityType", tag = TAG)
    }

    // ── Account ───────────────────────────────────────────────────────────

    // ── Sync ──────────────────────────────────────────────────────────────

    /**
     * Sync a single entity type: push pending local changes, then pull remote.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : SyncableEntity> syncEntity(
        entityType: String,
        forceFullSync: Boolean = false
    ): SyncResult<T> = withContext(AppDispatchers.IO) {
        val syncer = entitySyncers[entityType] as? EntitySyncer<T>
            ?: return@withContext SyncResult.Error("No syncer for $entityType")

        updateState(entityType) { it.copy(isSyncing = true, lastError = null) }

        try {
            pushPendingFor(entityType, syncer)

            val sinceMs = if (forceFullSync) 0L else _syncStates.value[entityType]?.lastSyncedAt ?: 0L
            val result = syncer.pullFromRemote(sinceMs)

            when (result) {
                is SyncResult.Success -> {
                    updateState(entityType) {
                        it.copy(
                            lastSyncedAt = TimeProvider.nowMillis(),
                            isSyncing = false,
                            lastError = null,
                            pendingCount = 0
                        )
                    }
                    _syncEvents.emit(SyncEvent.SyncCompleted(entityType, result.items.size, result.deletedIds))
                }
                is SyncResult.Error -> {
                    updateState(entityType) { it.copy(isSyncing = false, lastError = result.message) }
                    _syncEvents.emit(SyncEvent.SyncFailed(entityType, result.message))
                }
                is SyncResult.Partial -> {
                    updateState(entityType) {
                        it.copy(
                            lastSyncedAt = TimeProvider.nowMillis(),
                            isSyncing = false,
                            lastError = "Partial: ${result.failedIds.size} failed"
                        )
                    }
                    _syncEvents.emit(SyncEvent.SyncCompleted(entityType, result.items.size, result.deletedIds))
                }
            }
            result
        } catch (e: CancellationException) {
            updateState(entityType) { it.copy(isSyncing = false) }
            throw e
        } catch (e: Exception) {
            updateState(entityType) { it.copy(isSyncing = false, lastError = e.message) }
            _syncEvents.emit(SyncEvent.SyncFailed(entityType, e.message ?: "Unknown"))
            SyncResult.Error(e.message ?: "Sync failed", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : SyncableEntity> pushPendingFor(entityType: String, syncer: EntitySyncer<T>) {
        val relevant = offlineActionRepository?.getPendingActions()
            ?.filter { actionBelongsToEntity(it, entityType) }
            ?: return
        if (relevant.isEmpty()) return

        Napier.d("Pushing ${relevant.size} pending actions for $entityType", tag = TAG)

        for (action in relevant) {
            val result = runCatching { syncer.pushChange(action) }.getOrElse { Result.failure(it) }
            if (result.isSuccess) {
                offlineActionRepository.removeAction(action.id)
                _syncEvents.emit(SyncEvent.PendingActionCompleted(action.id))
            } else {
                val newCount = action.retryCount + 1
                if (newCount >= MAX_RETRY_ATTEMPTS) {
                    _syncEvents.emit(SyncEvent.PendingActionFailed(action.id, "Max retries"))
                }
                // Always update retry metadata — never treat network failure as deletion
                offlineActionRepository.updateAction(action.id, newCount, TimeProvider.nowMillis())
            }
        }
    }

    private fun actionBelongsToEntity(action: PendingAction, entityType: String): Boolean {
        return when (entityType) {
            "message" -> action.actionType in listOf(
                PendingAction.ActionType.SEND_MESSAGE,
                PendingAction.ActionType.EDIT,
                PendingAction.ActionType.DELETE
            )
            "post" -> action.actionType in listOf(
                PendingAction.ActionType.EDIT,
                PendingAction.ActionType.DELETE,
                PendingAction.ActionType.LIKE,
                PendingAction.ActionType.BOOKMARK
            )
            else -> false
        }
    }

    // ── Realtime ──────────────────────────────────────────────────────────

    /**
     * Subscribe to Supabase Realtime CDC for a table.
     * Changes are dispatched to the registered syncer for immediate local cache updates.
     */
    fun subscribeRealtime(entityType: String, tableName: String) {
        if (realtimeChannels.containsKey(entityType)) return

        scope.launch {
            var channel: RealtimeChannel? = null
            var registered = false
            try {
                supabaseClient.realtime.connect()
                channel = supabaseClient.realtime.channel("sync_${entityType}_${tableName}")

                // Subscribe first, then start collecting events
                channel.subscribe(blockUntilSubscribed = true)
                realtimeChannels[entityType] = channel
                registered = true
                Napier.d("Subscribed to realtime: $entityType ($tableName)", tag = TAG)

                // Collect Postgres CDC events for this table
                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = tableName
                }.collect { action -> handleRealtimeAction(entityType, action) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("Realtime subscribe failed for $entityType", e, TAG)
            } finally {
                val stillOurs = registered && realtimeChannels[entityType] === channel
                if (stillOurs) {
                    // The channel ended on its own. Evict it so future subscribes are not
                    // short-circuited by a dead entry, then reconnect.
                    realtimeChannels.remove(entityType)
                }
                // An explicit unsubscribeRealtime() removes the entry first, so a deliberate
                // unsubscribe never lands here as an owner.
                if (scope.isActive && (stillOurs || !registered)) {
                    scheduleReconnect(entityType, tableName)
                }
            }
        }
    }

    private suspend fun handleRealtimeAction(entityType: String, action: PostgresAction) {
        val syncer = entitySyncers[entityType] ?: return
        try {
            when (action) {
                is PostgresAction.Insert -> {
                    syncer.handleRemoteInsert(action.record.toStringMap())
                    _syncEvents.emit(SyncEvent.RemoteInsert(entityType, action.record["id"]?.toString() ?: ""))
                }
                is PostgresAction.Update -> {
                    syncer.handleRemoteUpdate(action.record.toStringMap())
                    _syncEvents.emit(SyncEvent.RemoteUpdate(entityType, action.record["id"]?.toString() ?: ""))
                }
                is PostgresAction.Delete -> {
                    val id = action.oldRecord["id"]?.toString() ?: return
                    syncer.handleRemoteDelete(id)
                    _syncEvents.emit(SyncEvent.RemoteDelete(entityType, id))
                }
                else -> Unit
            }
        } catch (e: Exception) {
            Napier.e("Error handling realtime action for $entityType", e, TAG)
        }
    }

    fun unsubscribeRealtime(entityType: String) {
        val channel = realtimeChannels.remove(entityType) ?: return
        scope.launch {
            try {
                supabaseClient.realtime.removeChannel(channel)
            } catch (e: Exception) {
                Napier.e("Error removing realtime channel for $entityType", e, TAG)
            }
        }
    }

    private fun scheduleReconnect(entityType: String, tableName: String) {
        reconnectJobs.remove(entityType)?.cancel()
        reconnectJobs[entityType] = scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (!realtimeChannels.containsKey(entityType)) {
                subscribeRealtime(entityType, tableName)
            }
        }
    }

    // ── Network ───────────────────────────────────────────────────────────

    /**
     * Internal untyped sync dispatch used for batch reconnect retries.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun syncEntityInternal(entityType: String) {
        (entitySyncers[entityType] as? EntitySyncer<SyncableEntity>)?.let {
            syncEntity<SyncableEntity>(entityType)
        }
    }

    fun onNetworkReconnected() {
        _isOnline.value = true
        Napier.d("Network reconnected — resuming sync", tag = TAG)
        scope.launch {
            _syncEvents.emit(SyncEvent.NetworkReconnected)
            // Retry all entity types after reconnection
            for (entityType in entitySyncers.keys.toList()) {
                syncEntityInternal(entityType)
            }
        }
    }

    fun onNetworkDisconnected() {
        _isOnline.value = false
        Napier.d("Network disconnected", tag = TAG)
        scope.launch { _syncEvents.emit(SyncEvent.NetworkDisconnected) }
    }

    // ── Account switch / logout ───────────────────────────────────────────

    /**
     * Clear all local data and sync state.
     * Call on logout or before switching to a different account.
     */
    suspend fun clearAllData() {
        Napier.d("Clearing all sync data (logout/account-switch)", tag = TAG)

        // Unsubscribe all realtime channels first
        realtimeChannels.keys.toList().forEach { unsubscribeRealtime(it) }

        // Clear persisted sync state
        syncStateStorage.clearAll()
        _syncStates.value = emptyMap()

        // Drain pending queue
        offlineActionRepository?.getPendingActions()?.forEach {
            offlineActionRepository.removeAction(it.id)
        }

        // Delegate to each syncer to clear its own local tables
        entitySyncers.values.forEach {
            try { it.clearLocal() } catch (e: Exception) {
                Napier.e("Error clearing local data for ${it.entityType}", e, TAG)
            }
        }

        currentUserId = null
        _syncEvents.emit(SyncEvent.DataCleared)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun updateState(entityType: String, transform: (SyncState) -> SyncState) {
        val updated = _syncStates.updateAndGet { states ->
            states + (entityType to transform(states[entityType] ?: SyncState(entityType)))
        }
        updated[entityType]?.let { state -> scope.launch { syncStateStorage.save(state) } }
    }
}

// ── Realtime payload helpers ──────────────────────────────────────────────────

/** Convert the Supabase JsonObject record to a plain Map<String, Any?> for syncer use. */
private fun kotlinx.serialization.json.JsonObject.toStringMap(): Map<String, Any?> =
    entries.associate { (k, v) ->
        k to when {
            v is kotlinx.serialization.json.JsonPrimitive -> v.content
            v is kotlinx.serialization.json.JsonNull       -> null
            else -> v.toString()
        }
    }

// ── Events ────────────────────────────────────────────────────────────────────

sealed class SyncEvent {
    data class SyncCompleted(val entityType: String, val itemCount: Int, val deletedIds: List<String>) : SyncEvent()
    data class SyncFailed(val entityType: String, val error: String) : SyncEvent()
    data class RemoteInsert(val entityType: String, val id: String) : SyncEvent()
    data class RemoteUpdate(val entityType: String, val id: String) : SyncEvent()
    data class RemoteDelete(val entityType: String, val id: String) : SyncEvent()
    data class PendingActionCompleted(val actionId: String) : SyncEvent()
    data class PendingActionFailed(val actionId: String, val reason: String) : SyncEvent()
    data object NetworkReconnected : SyncEvent()
    data object NetworkDisconnected : SyncEvent()
    data object DataCleared : SyncEvent()
}
