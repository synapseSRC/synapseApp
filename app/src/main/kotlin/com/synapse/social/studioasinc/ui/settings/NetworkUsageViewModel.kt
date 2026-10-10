package com.synapse.social.studioasinc.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.annotation.StringRes

data class NetworkUsageItem(
    @StringRes val labelRes: Int,
    val iconRes: Int?,
    val sentBytes: Long,
    val receivedBytes: Long
)

data class NetworkUsageUiState(
    val usageItems: List<NetworkUsageItem> = emptyList(),
    val totalSent: Long = 0L,
    val totalReceived: Long = 0L,
    val isLoading: Boolean = true,
    val lastResetTime: Long = 0L
)

@HiltViewModel
class NetworkUsageViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("network_stats_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(NetworkUsageUiState())
    val uiState: StateFlow<NetworkUsageUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val totalTxBytes = android.net.TrafficStats.getTotalTxBytes()
            val totalRxBytes = android.net.TrafficStats.getTotalRxBytes()

            val mobileTxBytes = android.net.TrafficStats.getMobileTxBytes()
            val mobileRxBytes = android.net.TrafficStats.getMobileRxBytes()

            val wifiTxBytes = maxOf(0L, totalTxBytes - mobileTxBytes)
            val wifiRxBytes = maxOf(0L, totalRxBytes - mobileRxBytes)

            val myUid = android.os.Process.myUid()
            val appTxBytes = android.net.TrafficStats.getUidTxBytes(myUid).let { if (it == android.net.TrafficStats.UNSUPPORTED.toLong()) 0L else it }
            val appRxBytes = android.net.TrafficStats.getUidRxBytes(myUid).let { if (it == android.net.TrafficStats.UNSUPPORTED.toLong()) 0L else it }

            val items = listOf(
                NetworkUsageItem(com.synapse.social.studioasinc.R.string.network_usage_mobile_data, null, mobileTxBytes, mobileRxBytes),
                NetworkUsageItem(com.synapse.social.studioasinc.R.string.network_usage_wifi, null, wifiTxBytes, wifiRxBytes),
                NetworkUsageItem(com.synapse.social.studioasinc.R.string.network_usage_this_app, null, appTxBytes, appRxBytes)
            )

            val lastResetTime = sharedPrefs.getLong("network_stats_reset_time", 0L)

            _uiState.value = _uiState.value.copy(
                usageItems = items,
                totalSent = totalTxBytes,
                totalReceived = totalRxBytes,
                isLoading = false,
                lastResetTime = lastResetTime
            )
        }
    }

    fun resetStats() {
        sharedPrefs.edit().putLong("network_stats_reset_time", System.currentTimeMillis()).apply()
        loadData()
    }
}
