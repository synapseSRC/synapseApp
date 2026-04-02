package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthField
import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength
import com.synapse.social.studioasinc.shared.domain.usecase.auth.*
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
    private val validateNewPasswordUseCase: ValidateNewPasswordUseCase,
    private val calculatePasswordStrengthUseCase: CalculatePasswordStrengthUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.ResetPassword())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()

    fun onPasswordChanged(password: String) {
        val strength = calculatePasswordStrengthUseCase(password)
        _uiState.value = AuthInputHelper.handlePasswordChanged(_uiState.value, password, strength)
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
         val state = _uiState.value as? AuthUiState.ResetPassword ?: return
         _uiState.value = state.copy(
             confirmPassword = confirmPassword,
             validationErrors = state.validationErrors - AuthField.CONFIRM_PASSWORD
         )
    }

    fun onSubmitNewPassword() {
        val state = _uiState.value as? AuthUiState.ResetPassword ?: return

        val validation = validateNewPasswordUseCase(state.password, state.confirmPassword)

        if (!validation.isValid) {
            val errors = mutableMapOf<AuthField, String?>()
            validation.passwordError?.let { errors[AuthField.PASSWORD] = it }
            validation.confirmPasswordError?.let { errors[AuthField.CONFIRM_PASSWORD] = it }
            _uiState.value = state.copy(validationErrors = errors)
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
                    _uiState.value = state.copy(
                        isLoading = false,
                        validationErrors = mapOf(AuthField.PASSWORD to error.message)
                    )
                }
            )
        }
    }
}
