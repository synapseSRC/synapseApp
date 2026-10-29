package com.synapse.social.studioasinc.feature.permission

import androidx.lifecycle.ViewModel
import com.synapse.social.studioasinc.data.permission.AndroidPermissionStatusProvider
import com.synapse.social.studioasinc.data.permission.AppPermission
import com.synapse.social.studioasinc.data.permission.PermissionOnboardingStore
import com.synapse.social.studioasinc.data.permission.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PermissionManagerViewModel @Inject constructor(
    private val statusProvider: AndroidPermissionStatusProvider,
    private val store: PermissionOnboardingStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionManagerUiState())
    val uiState: StateFlow<PermissionManagerUiState> = _uiState.asStateFlow()

    private var requestCounter = 0

    fun refresh(canAsk: (String) -> Boolean) {
        val snapshots = statusProvider.snapshot(canAsk)
        _uiState.update { it.copy(permissions = snapshots) }
    }

    fun onActionClick(permission: AppPermission) {
        val state = _uiState.value
        if (state.pending != null) return
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
        if (state.pending == null) launchRequest(permission)
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

    private fun launchRequest(permission: AppPermission) {
        requestCounter += 1
        _uiState.update {
            it.copy(pending = PendingPermissionRequest(permission, requestCounter))
        }
    }
}
