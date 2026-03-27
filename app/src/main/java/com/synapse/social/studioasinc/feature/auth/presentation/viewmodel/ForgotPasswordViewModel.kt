package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import com.synapse.social.studioasinc.shared.domain.usecase.auth.SendPasswordResetUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateEmailUseCase
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
class ForgotPasswordViewModel @Inject constructor(
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.ForgotPassword())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()

    fun onEmailChanged(email: String) {
        val isValid = validateEmailUseCase(email) is ValidationResult.Valid
        val state = _uiState.value as? AuthUiState.ForgotPassword ?: return
        _uiState.value = state.copy(email = email, emailError = null, isEmailValid = isValid)
    }

    fun onSubmitNewPassword() {
         val state = _uiState.value as? AuthUiState.ForgotPassword ?: return
         val emailValidation = validateEmailUseCase(state.email)
         if (emailValidation is ValidationResult.Invalid) {
             _uiState.value = state.copy(emailError = emailValidation.errorMessage)
             return
         }

         viewModelScope.launch {
             _uiState.value = state.copy(isLoading = true)
             sendPasswordResetUseCase(state.email).fold(
                 onSuccess = {
                     _uiState.value = state.copy(isLoading = false, isEmailSent = true)
                 },
                 onFailure = { error ->
                     _uiState.value = state.copy(isLoading = false, )
                 }
             )
         }
    }

    fun onBackToSignInClick() {
        viewModelScope.launch { _navigationEvent.emit(AuthNavigationEvent.NavigateToSignIn) }
    }
}
