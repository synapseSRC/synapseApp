package com.synapse.social.studioasinc.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.repository.DeviceInfoProvider
import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import com.synapse.social.studioasinc.shared.domain.usecase.auth.AddPasskeyUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.LoadPasskeysUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.RemovePasskeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable UI state of the Passkeys settings screen.
 */
data class PasskeysUiState(
    val passkeys: List<Passkey> = emptyList(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val removingIds: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class PasskeysViewModel @Inject constructor(
    private val loadPasskeysUseCase: LoadPasskeysUseCase,
    private val addPasskeyUseCase: AddPasskeyUseCase,
    private val removePasskeyUseCase: RemovePasskeyUseCase,
    private val deviceInfoProvider: DeviceInfoProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasskeysUiState())
    val uiState: StateFlow<PasskeysUiState> = _uiState.asStateFlow()

    init {
        loadPasskeys()
    }

    fun loadPasskeys() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loadPasskeysUseCase()
                .onSuccess { passkeys ->
                    _uiState.update { it.copy(passkeys = passkeys, isLoading = false) }
                }
                .onFailure { error ->
                    Log.e(TAG, "Error loading passkeys", error)
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun addPasskey() {
        val deviceName = deviceInfoProvider.getDeviceModel()
        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true, error = null) }
            addPasskeyUseCase(deviceName)
                .onSuccess {
                    _uiState.update { it.copy(isAdding = false) }
                    loadPasskeys()
                }
                .onFailure { error ->
                    Log.e(TAG, "Error adding passkey", error)
                    _uiState.update { it.copy(isAdding = false, error = error.message) }
                }
        }
    }

    fun removePasskey(id: String) {
        if (_uiState.value.removingIds.contains(id)) return
        viewModelScope.launch {
            _uiState.update { it.copy(removingIds = it.removingIds + id, error = null) }
            removePasskeyUseCase(id)
                .onSuccess {
                    _uiState.update { it.copy(removingIds = it.removingIds - id) }
                    loadPasskeys()
                }
                .onFailure { error ->
                    Log.e(TAG, "Error removing passkey", error)
                    _uiState.update {
                        it.copy(removingIds = it.removingIds - id, error = error.message)
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private companion object {
        const val TAG = "PasskeysViewModel"
    }
}
