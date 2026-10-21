package com.synapse.social.studioasinc.shared.data.sync

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors network connectivity and notifies [SyncManager] when the device comes back online
 * so that pending actions are flushed and incremental syncs are triggered promptly.
 *
 * Platform-specific connectivity detection (ConnectivityManager on Android,
 * Network.NWPathMonitor on iOS) can be wired in by calling [setOnline] from the
 * platform layer. The polling fallback below provides a backstop for platforms
 * where a native callback is not yet integrated.
 */
class NetworkMonitor(
    private val syncManager: SyncManager? = null
) {
    companion object {
        private const val TAG = "NetworkMonitor"
        private const val POLL_INTERVAL_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // ── Lifecycle ──────────────────────────────────────────────────────────

    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                // Platforms that inject real connectivity events don't need this;
                // it's a safety net to re-trigger sync if events were missed.
                if (_isOnline.value) {
                    syncManager?.onNetworkReconnected()
                }
            }
        }
        Napier.d("Network polling started", tag = TAG)
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        Napier.d("Network polling stopped", tag = TAG)
    }

    // ── Called by platform connectivity callbacks ─────────────────────────

    /**
     * Update network status. Called from platform-specific code (e.g. Android
     * ConnectivityManager callback) or from Ktor error interceptors.
     */
    fun setOnline(nowOnline: Boolean) {
        val wasOnline = _isOnline.value
        _isOnline.value = nowOnline

        if (!wasOnline && nowOnline) {
            Napier.d("Network: back online", tag = TAG)
            syncManager?.onNetworkReconnected()
        } else if (wasOnline && !nowOnline) {
            Napier.d("Network: went offline", tag = TAG)
            syncManager?.onNetworkDisconnected()
        }
    }
}
