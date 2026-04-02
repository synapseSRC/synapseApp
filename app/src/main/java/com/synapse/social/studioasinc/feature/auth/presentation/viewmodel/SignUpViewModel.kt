package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.config.Constants
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthField
import com.synapse.social.studioasinc.shared.core.util.UiEvent
import com.synapse.social.studioasinc.shared.core.util.UiEventManager
import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import com.synapse.social.studioasinc.shared.domain.usecase.auth.*
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
class SignUpViewModel @Inject constructor(
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateUsernameUseCase: ValidateUsernameUseCase,
    private val validateSignUpCredentialsUseCase: ValidateSignUpCredentialsUseCase,
    private val calculatePasswordStrengthUseCase: CalculatePasswordStrengthUseCase,
    private val checkUsernameAvailabilityUseCase: CheckUsernameAvailabilityUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val getOAuthUrlUseCase: GetOAuthUrlUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.SignUp())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()

    private var usernameCheckJob: Job? = null

    fun onEmailChanged(email: String) {
        val isValid = validateEmailUseCase(email) is ValidationResult.Valid
        _uiState.value = AuthInputHelper.handleEmailChanged(_uiState.value, email, isValid)
    }

    fun onPasswordChanged(password: String) {
        val strength = calculatePasswordStrengthUseCase(password)
        _uiState.value = AuthInputHelper.handlePasswordChanged(_uiState.value, password, strength)
    }

    fun onUsernameChanged(username: String) {
        when (val state = _uiState.value) {
            is AuthUiState.SignUp -> {
                _uiState.value = state.copy(
                    username = username,
                    validationErrors = state.validationErrors - AuthField.USERNAME,
                    isCheckingUsername = true
                )
                checkUsernameAvailability(username)
            }
            else -> {}
        }
    }

    private fun checkUsernameAvailability(username: String) {
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            delay(500) // Debounce
            val validation = validateUsernameUseCase(username)
            if (validation is ValidationResult.Invalid) {
                updateSignUpValidationErrors(AuthField.USERNAME, validation.errorMessage)
                updateSignUpState { copy(isCheckingUsername = false) }
                return@launch
            }

            checkUsernameAvailabilityUseCase(username).fold(
                onSuccess = { isAvailable ->
                    updateSignUpValidationErrors(
                        AuthField.USERNAME,
                        if (isAvailable) null else "Username is already taken"
                    )
                    updateSignUpState { copy(isCheckingUsername = false) }
                },
                onFailure = {
                    updateSignUpState { copy(isCheckingUsername = false) }
                }
            )
        }
    }

    fun onSignUpClick() {
        val state = _uiState.value as? AuthUiState.SignUp ?: return

        val validation = validateSignUpCredentialsUseCase(state.email, state.password, state.username)

        if (!validation.isValid) {
            val errors = mutableMapOf<AuthField, String?>()
            validation.emailError?.let { errors[AuthField.EMAIL] = it }
            validation.passwordError?.let { errors[AuthField.PASSWORD] = it }
            validation.usernameError?.let { errors[AuthField.USERNAME] = it }

            _uiState.value = state.copy(validationErrors = errors)
            viewModelScope.launch { UiEventManager.emit(UiEvent.Error("Validation failed")) }
            return
        }

        if (state.validationErrors[AuthField.USERNAME] != null) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, generalError = null, isErrorDismissed = false)
            signUpUseCase(state.email, state.password, state.username).fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false, showSuccessDialog = true)
                },
                onFailure = { error ->
                    _uiState.value = state.copy(
                        isLoading = false,
                        generalError = error.message ?: "Registration failed",
                        isErrorDismissed = false
                    )
                }
            )
        }
    }

    fun onDismissError() {
        val state = _uiState.value as? AuthUiState.SignUp ?: return
        _uiState.value = state.copy(isErrorDismissed = true)
    }

    fun onToggleModeClick() {
        viewModelScope.launch { _navigationEvent.emit(AuthNavigationEvent.NavigateToSignIn) }
    }

    fun onOAuthClick(provider: String) {
        viewModelScope.launch {
            if (provider.equals("Google", ignoreCase = true)) {
                _navigationEvent.emit(AuthNavigationEvent.InitiateGoogleSignIn)
            } else {
                getOAuthUrlUseCase(provider, Constants.AUTH_REDIRECT_URL).fold(
                    onSuccess = { url ->
                        _navigationEvent.emit(AuthNavigationEvent.OpenUrl(url))
                    },
                    onFailure = { e ->
                    }
                )
            }
        }
    }

    fun onDismissSuccessDialog() {
        val state = _uiState.value as? AuthUiState.SignUp ?: return
        viewModelScope.launch {
            _navigationEvent.emit(AuthNavigationEvent.NavigateToEmailVerification(state.email))
        }
    }

    private inline fun updateSignUpState(block: AuthUiState.SignUp.() -> AuthUiState.SignUp) {
        val state = _uiState.value as? AuthUiState.SignUp ?: return
        _uiState.value = state.block()
    }

    private fun updateSignUpValidationErrors(field: AuthField, error: String?) {
        val state = _uiState.value as? AuthUiState.SignUp ?: return
        val newErrors = if (error == null) state.validationErrors - field else state.validationErrors + (field to error)
        _uiState.value = state.copy(validationErrors = newErrors)
    }

    override fun onCleared() {
        super.onCleared()
        usernameCheckJob?.cancel()
    }
}
