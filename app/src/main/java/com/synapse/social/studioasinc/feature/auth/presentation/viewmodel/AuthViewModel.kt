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
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
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
        when (val state = _uiState.value) {
            is AuthUiState.SignIn -> _uiState.value = state.copy(email = email, emailError = null, isEmailValid = isValid)
            is AuthUiState.SignUp -> _uiState.value = state.copy(email = email, emailError = null, isEmailValid = isValid)
            is AuthUiState.ForgotPassword -> _uiState.value = state.copy(email = email, emailError = null, isEmailValid = isValid)
            else -> {}
        }
    }

    fun onPasswordChanged(password: String) {
        val strength = calculatePasswordStrengthUseCase(password)
        when (val state = _uiState.value) {
            is AuthUiState.SignIn -> _uiState.value = state.copy(password = password, passwordError = null)
            is AuthUiState.SignUp -> _uiState.value = state.copy(
                password = password,
                passwordError = null,
                passwordStrength = strength
            )
            is AuthUiState.ResetPassword -> _uiState.value = state.copy(password = password, passwordError = null)
            else -> {}
        }
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

        val emailValidation = validateEmailUseCase(state.email)
        val passwordValidation = validatePasswordUseCase(state.password)

        if (emailValidation is ValidationResult.Invalid || passwordValidation is ValidationResult.Invalid) {
            _uiState.value = state.copy(
                emailError = (emailValidation as? ValidationResult.Invalid)?.errorMessage,
                passwordError = (passwordValidation as? ValidationResult.Invalid)?.errorMessage
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
                    _uiState.value = AuthUiState.EmailVerification(email = state.email)
                    startResendCooldown()
                    checkEmailVerification(state.email)
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
            val currentState = _uiState.value
            if (currentState is AuthUiState.SignIn) {
                _uiState.value = currentState.copy(isLoading = true, generalError = null)
            } else if (currentState is AuthUiState.SignUp) {
                _uiState.value = currentState.copy(isLoading = true, generalError = null)
            }
            
            signInWithGoogleIdTokenUseCase(idToken).fold(
                onSuccess = {
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                },
                onFailure = { error ->
                    when (val state = _uiState.value) {
                        is AuthUiState.SignIn -> {
                            _uiState.value = state.copy(
                                isLoading = false,
                                generalError = error.message ?: "Google Sign-In failed"
                            )
                        }
                        is AuthUiState.SignUp -> {
                            _uiState.value = state.copy(
                                isLoading = false,
                                generalError = error.message ?: "Google Sign-In failed"
                            )
                        }
                        else -> {}
                    }
                }
            )
        }
    }
    
    fun handleGoogleSignInError(errorMessage: String) {
        when (val state = _uiState.value) {
            is AuthUiState.SignIn -> {
                _uiState.value = state.copy(
                    isLoading = false,
                    generalError = errorMessage
                )
            }
            is AuthUiState.SignUp -> {
                _uiState.value = state.copy(
                    isLoading = false,
                    generalError = errorMessage
                )
            }
            else -> {}
        }
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        
        Napier.d("Handling deep link: $uri", tag = "AuthViewModel")

        val code = uri.getQueryParameter("code")
        val fragment = uri.fragment

        var accessToken: String? = null
        var refreshToken: String? = null
        var error: String? = null
        var errorDescription: String? = null

        if (fragment != null) {
            Napier.d("Deep link has fragment: $fragment", tag = "AuthViewModel")
            val params = fragment.split("&").associate {
                val parts = it.split("=")
                if (parts.size == 2) parts[0] to parts[1] else "" to ""
            }
            accessToken = params["access_token"]
            refreshToken = params["refresh_token"]
            error = params["error"]
            errorDescription = params["error_description"]
        }

        if (uri.getQueryParameter("error") != null) {
            error = uri.getQueryParameter("error")
            errorDescription = uri.getQueryParameter("error_description")
            Napier.e("OAuth error in deep link: $error - $errorDescription", tag = "AuthViewModel")
        }

        val deepLink = OAuthDeepLink(
            provider = null,
            code = code,
            accessToken = accessToken,
            refreshToken = refreshToken,
            type = if (code != null) "pkce" else "implicit",
            error = error,
            errorCode = null,
            errorDescription = errorDescription
        )
        
        Napier.d("Processing OAuth callback - code: ${code != null}, accessToken: ${accessToken != null}", tag = "AuthViewModel")

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

    private suspend fun checkEmailVerification(email: String) {
        val pollInterval = 3000L
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

    private inline fun updateSignUpState(block: AuthUiState.SignUp.() -> AuthUiState.SignUp) {
        val state = _uiState.value as? AuthUiState.SignUp ?: return
        _uiState.value = state.block()
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
        usernameCheckJob?.cancel()
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
                    // Show error in state (add generalError to ResetPassword state if needed, or re-use passwordError)
                    _uiState.value = state.copy(isLoading = false, passwordError = error.message)
                }
            )
        }
    }
}
