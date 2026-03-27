package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NetworkUsageItem(
    val label: String,
    val iconRes: Int?,
    val sentBytes: Long,
    val receivedBytes: Long
)

@HiltViewModel
class NetworkUsageViewModel @Inject constructor() : ViewModel() {

    private val _usageItems = MutableStateFlow<List<NetworkUsageItem>>(emptyList())
    val usageItems: StateFlow<List<NetworkUsageItem>> = _usageItems.asStateFlow()

    private val _totalSent = MutableStateFlow(0L)
    val totalSent: StateFlow<Long> = _totalSent.asStateFlow()

    private val _totalReceived = MutableStateFlow(0L)
    val totalReceived: StateFlow<Long> = _totalReceived.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(500)

            val items = listOf(
                NetworkUsageItem("Calls", null, 150L * 1024 * 1024, 200L * 1024 * 1024),
                NetworkUsageItem("Media", null, 500L * 1024 * 1024, 1500L * 1024 * 1024),
                NetworkUsageItem("Google Drive", null, 50L * 1024 * 1024, 10L * 1024 * 1024),
                NetworkUsageItem("Messages", null, 10L * 1024 * 1024, 25L * 1024 * 1024),
                NetworkUsageItem("Status", null, 100L * 1024 * 1024, 800L * 1024 * 1024),
                NetworkUsageItem("Roaming", null, 0L, 0L)
            )

            _usageItems.value = items
            _totalSent.value = items.sumOf { it.sentBytes }
            _totalReceived.value = items.sumOf { it.receivedBytes }

            _isLoading.value = false
        }
    }
}
