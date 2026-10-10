package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.models.CallRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CallsViewModel @Inject constructor(
    // TODO: Add call history use case when available
) : ViewModel() {
    private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
    val callHistory = _callHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadCallHistory()
    }

    fun loadCallHistory() {
        // For now, return empty list
        // Future: integrate with call history repository
    }

    fun initiateCall(userId: String, isVideo: Boolean) {
        // TODO: Implement call initiation
    }
}
