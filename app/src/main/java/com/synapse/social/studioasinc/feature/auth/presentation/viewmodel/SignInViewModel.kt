package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.config.Constants
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthField
import com.synapse.social.studioasinc.shared.core.util.UiEvent
import com.synapse.social.studioasinc.shared.core.util.UiEventManager
import com.synapse.social.studioasinc.shared.domain.model.OAuthDeepLink
import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength
import com.synapse.social.studioasinc.shared.domain.usecase.auth.*
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateSignInCredentialsUseCase: ValidateSignInCredentialsUseCase,
    private val signInUseCase: SignInUseCase,
    private val getOAuthUrlUseCase: GetOAuthUrlUseCase,
    private val handleOAuthCallbackUseCase: HandleOAuthCallbackUseCase,
    private val signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.SignIn())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()

    fun onEmailChanged(email: String) {
        val isValid = validateEmailUseCase(email) is ValidationResult.Valid
        _uiState.value = AuthInputHelper.handleEmailChanged(_uiState.value, email, isValid)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = AuthInputHelper.handlePasswordChanged(_uiState.value, password, PasswordStrength.Weak)
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

    fun onDismissError() {
        val state = _uiState.value as? AuthUiState.SignIn ?: return
        _uiState.value = state.copy(isErrorDismissed = true)
    }

    fun onForgotPasswordClick() {
        viewModelScope.launch { _navigationEvent.emit(AuthNavigationEvent.NavigateToForgotPassword) }
    }

    fun onToggleModeClick() {
        viewModelScope.launch {
            _navigationEvent.emit(AuthNavigationEvent.NavigateToSignUp)
        }
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

        Napier.d("Processing OAuth callback", tag = "SignInViewModel")

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
}
