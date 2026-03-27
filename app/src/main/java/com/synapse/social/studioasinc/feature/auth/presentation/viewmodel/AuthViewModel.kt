package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import com.synapse.social.studioasinc.shared.core.util.UiEvent
import com.synapse.social.studioasinc.shared.core.util.UiEventManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.shared.domain.model.OAuthDeepLink
import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength
import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import com.synapse.social.studioasinc.core.config.Constants
import com.synapse.social.studioasinc.shared.domain.usecase.auth.*
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateSignInCredentialsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateSignUpCredentialsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateResetPasswordEmailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateNewPasswordUseCase
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
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

    fun onEvent(event: Any) {
        // Placeholder for future event handling
    }

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
                _uiState.value = state.copy(username = username, usernameError = null, isCheckingUsername = true)
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

    fun onConfirmPasswordChanged(confirmPassword: String) {
         when (val state = _uiState.value) {
            is AuthUiState.ResetPassword -> {
                _uiState.value = state.copy(confirmPassword = confirmPassword, confirmPasswordError = null)
            }
            else -> {}
        }
    }

    fun onSignInClick() {
        val state = _uiState.value as? AuthUiState.SignIn ?: return

        val validation = validateSignInCredentialsUseCase(state.email, state.password)

        if (!validation.isValid) {
            _uiState.value = state.copy(
                emailError = validation.emailError,
                passwordError = validation.passwordError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, generalError = null)
            signInUseCase(state.email, state.password).fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false)
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                },
                onFailure = { error ->
                    _uiState.value = state.copy(isLoading = false, generalError = error.message ?: "Login failed")
                }
            )
        }
    }

    fun onSignUpClick() {
        val state = _uiState.value as? AuthUiState.SignUp ?: return

        val validation = validateSignUpCredentialsUseCase(state.email, state.password, state.username)

        if (!validation.isValid) {
            _uiState.value = state.copy(
                emailError = validation.emailError,
                passwordError = validation.passwordError,
                usernameError = validation.usernameError
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

    fun onForgotPasswordClick() {
        _uiState.value = AuthUiState.ForgotPassword()
        viewModelScope.launch { _navigationEvent.emit(AuthNavigationEvent.NavigateToForgotPassword) }
    }

    fun onResetPasswordClick() {
         val state = _uiState.value as? AuthUiState.ForgotPassword ?: return
         val validation = validateResetPasswordEmailUseCase(state.email)

         if (!validation.isValid) {
             _uiState.value = state.copy(emailError = validation.emailError)
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
                 // Set loading state before starting the native flow
                 when (val state = _uiState.value) {
                     is AuthUiState.SignIn -> _uiState.value = state.copy(isLoading = true, generalError = null)
                     is AuthUiState.SignUp -> _uiState.value = state.copy(isLoading = true, generalError = null)
                     else -> {}
                 }
                 // Google Sign-In is handled natively via GoogleAuthHelper in AuthActivity
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
        
        Napier.d("Processing OAuth callback - code: ${deepLink.code != null}, accessToken: ${deepLink.accessToken != null}", tag = "AuthViewModel")

        viewModelScope.launch {
            handleOAuthCallbackUseCase(deepLink).fold(
                onSuccess = {
                    Napier.d("OAuth callback successful, navigating to main", tag = "AuthViewModel")
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                },
                onFailure = { e ->
                    Napier.e("OAuth callback failed: ${e.message}", e, tag = "AuthViewModel")
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
            _uiState.value = state.copy(
                passwordError = validation.passwordError,
                confirmPasswordError = validation.confirmPasswordError
            )
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
                    // Show error in state (add generalError to ResetPassword state if needed, or re-use passwordError)
                    _uiState.value = state.copy(isLoading = false, passwordError = error.message)
                }
            )
        }
    }
}
