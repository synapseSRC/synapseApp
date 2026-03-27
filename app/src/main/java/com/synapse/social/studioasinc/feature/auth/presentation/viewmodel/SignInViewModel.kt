package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.config.Constants
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.shared.core.util.UiEvent
import com.synapse.social.studioasinc.shared.core.util.UiEventManager
import com.synapse.social.studioasinc.shared.domain.model.OAuthDeepLink
import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
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
        val state = _uiState.value as? AuthUiState.SignIn ?: return
        _uiState.value = state.copy(email = email, emailError = null, isEmailValid = isValid)
    }

    fun onPasswordChanged(password: String) {
        val state = _uiState.value as? AuthUiState.SignIn ?: return
        _uiState.value = state.copy(password = password, passwordError = null)
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
            val currentState = _uiState.value as? AuthUiState.SignIn
            if (currentState != null) {
                _uiState.value = currentState.copy(isLoading = true, generalError = null)
            }

            signInWithGoogleIdTokenUseCase(idToken).fold(
                onSuccess = {
                    val state = _uiState.value as? AuthUiState.SignIn
                    if (state != null) {
                        _uiState.value = state.copy(isLoading = false)
                    }
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                },
                onFailure = { error ->
                    val state = _uiState.value as? AuthUiState.SignIn ?: return@fold
                    _uiState.value = state.copy(
                        isLoading = false,
                        generalError = error.message ?: "Google Sign-In failed"
                    )
                }
            )
        }
    }

    fun handleGoogleSignInError(errorMessage: String) {
        val state = _uiState.value as? AuthUiState.SignIn ?: return
        _uiState.value = state.copy(
            isLoading = false,
            generalError = errorMessage
        )
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return

        Napier.d("Handling deep link: $uri", tag = "SignInViewModel")

        val code = uri.getQueryParameter("code")
        val fragment = uri.fragment

        var accessToken: String? = null
        var refreshToken: String? = null
        var error: String? = null
        var errorDescription: String? = null

        if (fragment != null) {
            Napier.d("Deep link has fragment: $fragment", tag = "SignInViewModel")
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
            Napier.e("OAuth error in deep link: $error - $errorDescription", tag = "SignInViewModel")
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

        Napier.d("Processing OAuth callback - code: ${code != null}, accessToken: ${accessToken != null}", tag = "SignInViewModel")

        viewModelScope.launch {
            handleOAuthCallbackUseCase(deepLink).fold(
                onSuccess = {
                    Napier.d("OAuth callback successful, navigating to main", tag = "SignInViewModel")
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                },
                onFailure = { e ->
                    Napier.e("OAuth callback failed: ${e.message}", e, tag = "SignInViewModel")
                    _uiState.value = AuthUiState.SignIn()
                    viewModelScope.launch { UiEventManager.emit(UiEvent.Error(e.message ?: "Error")) }
                }
            )
        }
    }
}
