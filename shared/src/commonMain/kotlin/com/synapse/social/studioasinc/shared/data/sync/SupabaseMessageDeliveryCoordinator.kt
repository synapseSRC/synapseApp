package com.synapse.social.studioasinc.shared.data.sync

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.datasource.ChatMessageDataSource
import com.synapse.social.studioasinc.shared.data.datasource.ChatRealtimeDataSource
import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.MessageDeliveryCoordinator
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Transport-level delivery is independent of decryption, read receipts, and screen visibility. */
class SupabaseMessageDeliveryCoordinator(
    client: SupabaseClient,
    private val authRepository: AuthRepository
) : MessageDeliveryCoordinator {
    private val messages = ChatMessageDataSource(client)
    private val realtime = ChatRealtimeDataSource(client)
    private val scope = CoroutineScope(SupervisorJob() + AppDispatchers.IO)
    private val lifecycleMutex = Mutex()
    private val syncMutex = Mutex()
    private var sessionJob: Job? = null

    // A signal received during an RPC remains queued for a trailing pass. No arrival is discarded
    // merely because another acknowledgment is in flight. The server is the durable pending queue.
    private val syncRequests = Channel<Unit>(Channel.CONFLATED)

    override fun start() {
        scope.launch {
            lifecycleMutex.withLock {
                if (sessionJob?.isActive == true) return@withLock
                sessionJob = scope.launch {
                    authRepository.sessionStatus
                        .filter { it != AuthSessionStatus.INITIALIZING && it != AuthSessionStatus.REFRESHING }
                        .map { if (it == AuthSessionStatus.AUTHENTICATED) authRepository.getCurrentUserId() else null }
                        .distinctUntilChanged()
                        .collectLatest { userId ->
                            // collectLatest cancels and joins the old account's receiver and retries.
                            if (userId != null) receiveForSession(userId)
                        }
                }
            }
        }
    }

    override fun requestSync() {
        start()
        syncRequests.trySend(Unit)
    }

    private suspend fun receiveForSession(userId: String) = coroutineScope {
        launch {
            for (request in syncRequests) {
                var retryDelay = 1_000L
                while (isActive && authRepository.getCurrentUserId() == userId) {
                    if (synchronize().isSuccess) break
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(60_000L)
                }
            }
        }
        // Catch up even if the socket cannot connect. Another pass after subscription closes the
        // snapshot/subscription gap and is repeated after every reconnect.
        syncRequests.trySend(Unit)
        while (isActive && authRepository.getCurrentUserId() == userId) {
            try {
                realtime.subscribeToInboxUpdates(emptyList()) { syncRequests.trySend(Unit) }
                    .collect { message ->
                        if (message.senderId != userId && message.deliveredAt == null) {
                            syncRequests.trySend(Unit)
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("Delivery stream interrupted; reconnecting", tag = TAG)
            }
            delay(2_000L)
        }
    }

    override suspend fun synchronize(): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            // Push can cold-start the process before persisted authentication has been restored.
            val status = withTimeoutOrNull(10_000L) {
                authRepository.sessionStatus.first {
                    it == AuthSessionStatus.AUTHENTICATED || it == AuthSessionStatus.NOT_AUTHENTICATED
                }
            } ?: return@withContext Result.failure(IllegalStateException("Delivery session is not ready"))
            if (status == AuthSessionStatus.NOT_AUTHENTICATED) return@withContext Result.success(Unit)
            val userId = authRepository.getCurrentUserId()
                ?: return@withContext Result.failure(IllegalStateException("Delivery session is not ready"))
            val completed = withTimeoutOrNull(60_000L) {
                syncMutex.withLock {
                    // Always query the server, not inbox badges or the first page of conversations.
                    // This includes new, archived, and muted chats. The receipt RPC is idempotent,
                    // participant-authorized, sender-excluding, and uses any-recipient group semantics.
                    while (true) {
                        checkAccount(userId)
                        val pending = messages.getPendingDeliveries(userId)
                        checkAccount(userId)
                        if (pending.isEmpty()) break
                        // A row without an ID cannot be acknowledged. Drop only that row rather than
                        // aborting the whole batch, which would leave every other pending delivery
                        // unacknowledged and retry forever against the same malformed page.
                        val ids = pending.mapNotNull { it.id }
                        if (ids.isEmpty()) break
                        messages.acknowledgeReceivedMessages(userId, ids)
                    }
                }
                true
            } ?: false
            if (completed) Result.success(Unit)
            else Result.failure(IllegalStateException("Delivery synchronization timed out"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Delivery synchronization failed; retry required", tag = TAG)
            Result.failure(e)
        }
    }

    private fun checkAccount(userId: String) {
        check(authRepository.getCurrentUserId() == userId) { "Delivery account changed" }
    }

    private companion object {
        const val TAG = "MessageDelivery"
    }
}
