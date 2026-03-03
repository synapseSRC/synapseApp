package com.synapse.social.studioasinc.ui.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.usecase.auth.AddPasskeyUseCase
import com.synapse.social.studioasinc.domain.usecase.auth.LoadPasskeysUseCase
import com.synapse.social.studioasinc.domain.usecase.auth.RemovePasskeyUseCase
import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasskeysViewModel @Inject constructor(
    private val loadPasskeysUseCase: LoadPasskeysUseCase,
    private val addPasskeyUseCase: AddPasskeyUseCase,
    private val removePasskeyUseCase: RemovePasskeyUseCase
) : ViewModel() {

    private val _passkeys = MutableStateFlow<List<Passkey>>(emptyList())
    val passkeys: StateFlow<List<Passkey>> = _passkeys.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPasskeys()
    }

    fun loadPasskeys() {
        viewModelScope.launch {
            _isLoading.value = true
            loadPasskeysUseCase()
                .onSuccess {
                    _passkeys.value = it
                }
                .onFailure { e ->
                    Log.e("PasskeysViewModel", "Error loading passkeys", e)
                    _error.value = e.message ?: "Failed to load passkeys"
                }
            _isLoading.value = false
        }
    }

    fun addPasskey(activityContext: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            addPasskeyUseCase(activityContext)
                .onSuccess {
                    Log.d("PasskeysViewModel", "Passkey added successfully")
                    loadPasskeys() // Reload the list
                }
                .onFailure { e ->
                    Log.e("PasskeysViewModel", "Error adding passkey", e)
                    _error.value = e.message ?: "Error adding passkey"
                }

            _isLoading.value = false
        }
    }

    fun removePasskey(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            removePasskeyUseCase(id)
                .onSuccess {
                    Log.d("PasskeysViewModel", "Passkey removed successfully")
                    loadPasskeys()
                }
                .onFailure { e ->
                    Log.e("PasskeysViewModel", "Error removing passkey", e)
                    _error.value = e.message ?: "Failed to remove passkey"
                }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
