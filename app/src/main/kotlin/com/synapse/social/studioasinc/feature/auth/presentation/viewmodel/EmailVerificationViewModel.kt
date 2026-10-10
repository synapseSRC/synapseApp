package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.shared.core.util.UiEvent
import com.synapse.social.studioasinc.shared.core.util.UiEventManager
import com.synapse.social.studioasinc.shared.domain.usecase.auth.IsEmailVerifiedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.RefreshSessionUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ResendVerificationEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val isEmailVerifiedUseCase: IsEmailVerifiedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.EmailVerification(email = ""))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()

    private var cooldownJob: Job? = null
    private val RESEND_COOLDOWN_SECONDS = 60

    fun initEmail(email: String) {
        if (_uiState.value is AuthUiState.EmailVerification) {
            val currentState = _uiState.value as AuthUiState.EmailVerification
            if (currentState.email != email) {
                _uiState.value = currentState.copy(email = email)
                startResendCooldown()
                checkEmailVerification(email)
            }
        }
    }

    fun onResendVerificationEmail() {
        val state = _uiState.value as? AuthUiState.EmailVerification ?: return
        if (!state.canResend) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            resendVerificationEmailUseCase(state.email).fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false, isResent = true)
                    startResendCooldown()
                },
                onFailure = { error ->
                    _uiState.value = state.copy(isLoading = false)
                    viewModelScope.launch { UiEventManager.emit(UiEvent.Error(error.message ?: "Error")) }
                }
            )
        }
    }

    fun onBackToSignInClick() {
        viewModelScope.launch { _navigationEvent.emit(AuthNavigationEvent.NavigateToSignIn) }
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()
        val state = _uiState.value as? AuthUiState.EmailVerification ?: return

        cooldownJob = viewModelScope.launch {
            for (seconds in RESEND_COOLDOWN_SECONDS downTo 1) {
                val current = _uiState.value as? AuthUiState.EmailVerification ?: break
                _uiState.value = current.copy(canResend = false, resendCooldownSeconds = seconds)
                delay(1000)
            }
            val finalState = _uiState.value as? AuthUiState.EmailVerification
            if (finalState != null) {
                _uiState.value = finalState.copy(canResend = true, resendCooldownSeconds = 0, isResent = false)
            }
        }
    }

    private fun checkEmailVerification(email: String) {
        val pollInterval = 3000L
        viewModelScope.launch {
            while (_uiState.value is AuthUiState.EmailVerification) {
                delay(pollInterval)
                if (_uiState.value !is AuthUiState.EmailVerification) break

                refreshSessionUseCase().onSuccess {
                    if (isEmailVerifiedUseCase()) {
                        _uiState.value = AuthUiState.Success("Email verified successfully")
                        delay(1000)
                        _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
    }
}
