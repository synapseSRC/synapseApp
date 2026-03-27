package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.config.Constants
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
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
        val state = _uiState.value as? AuthUiState.SignUp ?: return
        _uiState.value = state.copy(email = email, emailError = null, isEmailValid = isValid)
    }

    fun onPasswordChanged(password: String) {
        val strength = calculatePasswordStrengthUseCase(password)
        val state = _uiState.value as? AuthUiState.SignUp ?: return
        _uiState.value = state.copy(
            password = password,
            passwordError = null,
            passwordStrength = strength
        )
    }

    fun onUsernameChanged(username: String) {
        val state = _uiState.value as? AuthUiState.SignUp ?: return
        _uiState.value = state.copy(username = username, usernameError = null, isCheckingUsername = true)
        checkUsernameAvailability(username)
    }

    private fun checkUsernameAvailability(username: String) {
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            delay(500) // Debounce
            val validation = validateUsernameUseCase(username)
            if (validation is ValidationResult.Invalid) {
                updateSignUpState { copy(usernameError = validation.errorMessage, isCheckingUsername = false) }
                return@launch
            }

            checkUsernameAvailabilityUseCase(username).fold(
                onSuccess = { isAvailable ->
                    updateSignUpState {
                        copy(
                            usernameError = if (isAvailable) null else "Username is already taken",
                            isCheckingUsername = false
                        )
                    }
                },
                onFailure = {
                    updateSignUpState { copy(isCheckingUsername = false) }
                }
            )
        }
    }

    fun onSignUpClick() {
        val state = _uiState.value as? AuthUiState.SignUp ?: return

        val emailValidation = validateEmailUseCase(state.email)
        val passwordValidation = validatePasswordUseCase(state.password)
        val usernameValidation = validateUsernameUseCase(state.username)

        if (emailValidation is ValidationResult.Invalid ||
            passwordValidation is ValidationResult.Invalid ||
            usernameValidation is ValidationResult.Invalid) {

            _uiState.value = state.copy(
                emailError = (emailValidation as? ValidationResult.Invalid)?.errorMessage,
                passwordError = (passwordValidation as? ValidationResult.Invalid)?.errorMessage,
                usernameError = (usernameValidation as? ValidationResult.Invalid)?.errorMessage
            )
            return
        }

        if (state.usernameError != null) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, generalError = null)
            signUpUseCase(state.email, state.password, state.username).fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false, showSuccessDialog = true)
                },
                onFailure = { error ->
                    _uiState.value = state.copy(isLoading = false, generalError = error.message ?: "Registration failed")
                }
            )
        }
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

    override fun onCleared() {
        super.onCleared()
        usernameCheckJob?.cancel()
    }
}
