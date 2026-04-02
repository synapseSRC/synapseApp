package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import com.synapse.social.studioasinc.shared.core.util.UiEvent
import com.synapse.social.studioasinc.shared.core.util.UiEventManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthField
import com.synapse.social.studioasinc.shared.domain.model.OAuthDeepLink
import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength
import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import com.synapse.social.studioasinc.core.config.Constants
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
import android.net.Uri
import io.github.aakira.napier.Napier

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val validateSignInCredentialsUseCase: ValidateSignInCredentialsUseCase,
    private val validateSignUpCredentialsUseCase: ValidateSignUpCredentialsUseCase,
    private val validateResetPasswordEmailUseCase: ValidateResetPasswordEmailUseCase,
    private val validateNewPasswordUseCase: ValidateNewPasswordUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validateUsernameUseCase: ValidateUsernameUseCase,
    private val calculatePasswordStrengthUseCase: CalculatePasswordStrengthUseCase,
    private val checkUsernameAvailabilityUseCase: CheckUsernameAvailabilityUseCase,
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
    private val handleOAuthCallbackUseCase: HandleOAuthCallbackUseCase,
    private val getOAuthUrlUseCase: GetOAuthUrlUseCase,
    private val signInWithOAuthUseCase: SignInWithOAuthUseCase,
    private val signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val isEmailVerifiedUseCase: IsEmailVerifiedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.SignIn())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()

    private var cooldownJob: Job? = null
    private val RESEND_COOLDOWN_SECONDS = 60
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

    fun onConfirmPasswordChanged(confirmPassword: String) {
         when (val state = _uiState.value) {
            is AuthUiState.ResetPassword -> {
                _uiState.value = state.copy(
                    confirmPassword = confirmPassword,
                    validationErrors = state.validationErrors - AuthField.CONFIRM_PASSWORD
                )
            }
            else -> {}
        }
    }

    fun onSignInClick() {
        val state = _uiState.value as? AuthUiState.SignIn ?: return

        val validation = validateSignInCredentialsUseCase(state.email, state.password)

        if (!validation.isValid) {
            val errors = mutableMapOf<AuthField, String?>()
            validation.emailError?.let { errors[AuthField.EMAIL] = it }
            validation.passwordError?.let { errors[AuthField.PASSWORD] = it }

            _uiState.value = state.copy(validationErrors = errors)
            viewModelScope.launch { UiEventManager.emit(UiEvent.Error("Validation failed")) }
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, generalError = null, isErrorDismissed = false)
            signInUseCase(state.email, state.password).fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false)
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                },
                onFailure = { error ->
                    _uiState.value = state.copy(
                        isLoading = false,
                        generalError = error.message ?: "Login failed",
                        isErrorDismissed = false
                    )
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

    fun onDismissGeneralError() {
        when (val state = _uiState.value) {
            is AuthUiState.SignIn -> _uiState.value = state.copy(isErrorDismissed = true)
            is AuthUiState.SignUp -> _uiState.value = state.copy(isErrorDismissed = true)
            else -> {}
        }
    }

    fun onForgotPasswordClick() {
        _uiState.value = AuthUiState.ForgotPassword()
        viewModelScope.launch { _navigationEvent.emit(AuthNavigationEvent.NavigateToForgotPassword) }
    }

    fun onResetPasswordClick() {
         val state = _uiState.value as? AuthUiState.ForgotPassword ?: return
         val validation = validateResetPasswordEmailUseCase(state.email)

         if (!validation.isValid) {
             _uiState.value = state.copy(validationErrors = mapOf(AuthField.EMAIL to validation.emailError))
             return
         }

         viewModelScope.launch {
             _uiState.value = state.copy(isLoading = true)
             sendPasswordResetUseCase(state.email).fold(
                 onSuccess = {
                     _uiState.value = state.copy(isLoading = false, isEmailSent = true)
                 },
                 onFailure = { error ->
                     _uiState.value = state.copy(isLoading = false)
                 }
             )
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

    fun onOAuthClick(provider: String) {
        viewModelScope.launch {
             if (provider.equals("Google", ignoreCase = true)) {
                 when (val state = _uiState.value) {
                     is AuthUiState.SignIn -> _uiState.value = state.copy(isLoading = true, generalError = null)
                     is AuthUiState.SignUp -> _uiState.value = state.copy(isLoading = true, generalError = null)
                     else -> {}
                 }
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

    fun handleGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthInputHelper.handleGoogleIdTokenLoading(_uiState.value)
            
            signInWithGoogleIdTokenUseCase(idToken).fold(
                onSuccess = {
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                },
                onFailure = { error ->
                    _uiState.value = AuthInputHelper.handleGoogleIdTokenFailure(
                        _uiState.value,
                        error.message ?: "Google Sign-In failed"
                    )
                }
            )
        }
    }
    
    fun handleGoogleSignInError(errorMessage: String) {
        _uiState.value = AuthInputHelper.handleGoogleSignInError(_uiState.value, errorMessage)
    }

    fun handleDeepLink(uri: Uri?) {
        val deepLink = AuthOAuthHelper.parseDeepLink(uri) ?: return
        
        Napier.d("Processing OAuth callback", tag = "AuthViewModel")

        viewModelScope.launch {
            handleOAuthCallbackUseCase(deepLink).fold(
                onSuccess = {
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                },
                onFailure = { e ->
                     _uiState.value = AuthUiState.SignIn()
                     viewModelScope.launch { UiEventManager.emit(UiEvent.Error(e.message ?: "Error")) }
                }
            )
        }
    }

    fun onBackToSignInClick() {
        _uiState.value = AuthUiState.SignIn()
        viewModelScope.launch { _navigationEvent.emit(AuthNavigationEvent.NavigateToSignIn) }
    }

    fun onToggleModeClick() {
        viewModelScope.launch {
            when (_uiState.value) {
                is AuthUiState.SignIn -> {
                    _uiState.value = AuthUiState.SignUp()
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToSignUp)
                }
                is AuthUiState.SignUp -> {
                    _uiState.value = AuthUiState.SignIn()
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToSignIn)
                }
                else -> {}
            }
        }
    }

    private fun startResendCooldown() {
        viewModelScope.startResendCooldownExt(
            _uiState,
            RESEND_COOLDOWN_SECONDS,
            cooldownJob
        ) { job ->
            cooldownJob = job
        }
    }

    private suspend fun checkEmailVerification(email: String) {
        viewModelScope.checkEmailVerificationExt(
            _uiState,
            refreshSessionUseCase,
            isEmailVerifiedUseCase,
            _navigationEvent
        )
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

    fun onDismissSuccessDialog() {
        val state = _uiState.value as? AuthUiState.SignUp ?: return
        viewModelScope.launch {
            _uiState.value = AuthUiState.EmailVerification(email = state.email)
            startResendCooldown()
            checkEmailVerification(state.email)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
        usernameCheckJob?.cancel()
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
