package com.synapse.social.studioasinc.shared.data.sync

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors network connectivity and notifies [SyncManager] when the device comes back online
 * so that pending actions are flushed and incremental syncs are triggered promptly.
 *
 * Platform-specific connectivity detection (ConnectivityManager on Android,
 * Network.NWPathMonitor on iOS) is wired in by calling [setOnline] from the platform layer.
 */
class NetworkMonitor(
    private val syncManager: SyncManager? = null
) {
    companion object {
        private const val TAG = "NetworkMonitor"
    }

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

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
