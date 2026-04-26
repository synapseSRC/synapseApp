package com.synapse.social.studioasinc.desktop.ui

import com.synapse.social.studioasinc.shared.domain.usecase.auth.SignInUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.aakira.napier.Napier

class DesktopAuthViewModel(
    private val signInUseCase: SignInUseCase
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            signInUseCase(email, password)
                .onSuccess {
                    Napier.d("Successfully signed in")
                }
                .onFailure { e ->
                    Napier.e("Failed to sign in", e)
                    _error.value = e.message ?: "An unknown error occurred"
                }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
