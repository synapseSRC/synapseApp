package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import com.synapse.social.studioasinc.shared.domain.usecase.auth.CalculatePasswordStrengthUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ResetPasswordUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidatePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val calculatePasswordStrengthUseCase: CalculatePasswordStrengthUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.ResetPassword())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()

    fun onPasswordChanged(password: String) {
        val strength = calculatePasswordStrengthUseCase(password)
        val state = _uiState.value as? AuthUiState.ResetPassword ?: return
        _uiState.value = state.copy(password = password, passwordError = null)
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        val state = _uiState.value as? AuthUiState.ResetPassword ?: return
        _uiState.value = state.copy(confirmPassword = confirmPassword, confirmPasswordError = null)
    }

    fun onSubmitNewPassword() {
        val state = _uiState.value as? AuthUiState.ResetPassword ?: return
        val passwordValidation = validatePasswordUseCase(state.password)
        if (passwordValidation is ValidationResult.Invalid) {
            _uiState.value = state.copy(passwordError = passwordValidation.errorMessage)
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(confirmPasswordError = "Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            resetPasswordUseCase(state.password).fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false)
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToSignIn)
                },
                onFailure = { error ->
                    _uiState.value = state.copy(isLoading = false, passwordError = error.message)
                }
            )
        }
    }
}
