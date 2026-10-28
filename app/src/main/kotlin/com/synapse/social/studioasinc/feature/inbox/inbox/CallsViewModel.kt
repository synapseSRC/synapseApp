package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendException
import com.synapse.social.studioasinc.shared.domain.model.call.CallErrorKind
import com.synapse.social.studioasinc.shared.domain.usecase.call.GetCallHistoryUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.ResolveStaleCallSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Calls tab's history.
 *
 * Loading is triggered by the screen rather than by construction: this ViewModel outlives a
 * visit to a chat or a call, and a history read at construction would be stale by the time the
 * user comes back to it.
 */
@HiltViewModel
class CallsViewModel @Inject constructor(
    private val getCallHistory: GetCallHistoryUseCase,
    private val resolveStaleSessions: ResolveStaleCallSessionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallsUiState())
    val uiState: StateFlow<CallsUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    fun refresh() {
        // A pull-to-refresh while the first read is still in flight replaces it instead of
        // racing it: two interleaved responses would let the older one land last.
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Healing first means a call that died with the process is already closed by the
            // time it would appear in the list. A failure here is not a failure to show
            // history — the read below decides whether the tab has anything to say.
            resolveStaleSessions().onFailure {
                Napier.w("Stale call cleanup failed: ${it.message}", tag = TAG)
            }

            getCallHistory()
                .onSuccess { sessions ->
                    _uiState.update {
                        it.copy(history = sessions, isLoading = false, hasLoaded = true, error = null)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, hasLoaded = true, error = error.toErrorKind())
                    }
                }
        }
    }

    fun retry() = refresh()

    private fun Throwable.toErrorKind(): CallErrorKind =
        (this as? CallBackendException)?.kind ?: CallErrorKind.UNKNOWN

    private companion object {
        const val TAG = "Call"
    }
}
