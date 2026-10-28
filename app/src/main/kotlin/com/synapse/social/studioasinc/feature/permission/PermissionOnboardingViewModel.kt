package com.synapse.social.studioasinc.feature.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.permission.AndroidPermissionStatusProvider
import com.synapse.social.studioasinc.data.permission.AppPermission
import com.synapse.social.studioasinc.data.permission.PermissionOnboardingStore
import com.synapse.social.studioasinc.data.permission.PermissionStatus
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PermissionOnboardingViewModel @Inject constructor(
    private val statusProvider: AndroidPermissionStatusProvider,
    private val store: PermissionOnboardingStore,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionOnboardingUiState())
    val uiState: StateFlow<PermissionOnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PermissionOnboardingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PermissionOnboardingEvent> = _events.asSharedFlow()

    private var requestCounter = 0

    /** Re-reads the live OS state; never touches [PermissionOnboardingUiState.pending]. */
    fun refresh(canAsk: (String) -> Boolean) {
        val snapshots = statusProvider.snapshot(canAsk)
        _uiState.update { it.copy(permissions = snapshots) }
    }

    /** Handles only the two statuses that can still show a system dialog. */
    fun onActionClick(permission: AppPermission) {
        val state = _uiState.value
        if (state.pending != null || state.isCompleting) return
        when (state.permissions.firstOrNull { it.permission == permission }?.status) {
            PermissionStatus.NOT_REQUESTED -> launchRequest(permission)
            PermissionStatus.DENIED -> _uiState.update { it.copy(rationaleFor = permission) }
            else -> Unit
        }
    }

    fun confirmRationale() {
        val state = _uiState.value
        val permission = state.rationaleFor ?: return
        _uiState.update { it.copy(rationaleFor = null) }
        if (state.pending == null && !state.isCompleting) launchRequest(permission)
    }

    fun dismissRationale() {
        _uiState.update { it.copy(rationaleFor = null) }
    }

    fun onRequestLaunched() {
        val pending = _uiState.value.pending ?: return
        if (pending.launched) return
        store.markRequested(pending.permission)
        _uiState.update { it.copy(pending = pending.copy(launched = true)) }
    }

    fun onRequestResult(canAsk: (String) -> Boolean) {
        val snapshots = statusProvider.snapshot(canAsk)
        _uiState.update { it.copy(pending = null, permissions = snapshots) }
    }

    fun continueToSummary(canAsk: (String) -> Boolean) {
        if (_uiState.value.pending != null) return
        val snapshots = statusProvider.snapshot(canAsk)
        _uiState.update {
            it.copy(
                step = PermissionOnboardingStep.SUMMARY,
                skipped = false,
                permissions = snapshots
            )
        }
    }

    fun backToCards(canAsk: (String) -> Boolean) {
        val snapshots = statusProvider.snapshot(canAsk)
        _uiState.update {
            it.copy(step = PermissionOnboardingStep.CARDS, permissions = snapshots)
        }
    }

    fun skip(canAsk: (String) -> Boolean) {
        if (_uiState.value.pending != null) return
        val snapshots = statusProvider.snapshot(canAsk)
        _uiState.update {
            it.copy(
                step = PermissionOnboardingStep.SUMMARY,
                skipped = true,
                permissions = snapshots
            )
        }
    }

    /**
     * Records that this account has seen the onboarding and only then opens the app.
     *
     * Nothing about the individual grants is stored: Android remains the source of truth, and the
     * permission manager keeps working after this flow is finished.
     */
    fun complete() {
        val state = _uiState.value
        if (state.isCompleting || state.pending != null) return
        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                withContext(Dispatchers.IO) {
                    store.markCompleted(userId, skipped = state.skipped)
                }
            }
            _uiState.update { it.copy(isCompleting = false) }
            _events.emit(PermissionOnboardingEvent.EnterMainApp)
        }
    }

    private fun launchRequest(permission: AppPermission) {
        requestCounter += 1
        _uiState.update {
            it.copy(pending = PendingPermissionRequest(permission, requestCounter))
        }
    }
}
