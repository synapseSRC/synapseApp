package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength
import com.synapse.social.studioasinc.shared.domain.model.ValidationResult

object AuthInputHelper {
    fun handleEmailChanged(state: AuthUiState, email: String, isValid: Boolean): AuthUiState {
        return when (state) {
            is AuthUiState.SignIn -> state.copy(email = email, emailError = null, isEmailValid = isValid)
            is AuthUiState.SignUp -> state.copy(email = email, emailError = null, isEmailValid = isValid)
            is AuthUiState.ForgotPassword -> state.copy(email = email, emailError = null, isEmailValid = isValid)
            else -> state
        }
    }

    fun handlePasswordChanged(state: AuthUiState, password: String, strength: PasswordStrength): AuthUiState {
        return when (state) {
            is AuthUiState.SignIn -> state.copy(password = password, passwordError = null)
            is AuthUiState.SignUp -> state.copy(
                password = password,
                passwordError = null,
                passwordStrength = strength
            )
            is AuthUiState.ResetPassword -> state.copy(password = password, passwordError = null)
            else -> state
        }
    }

    fun handleGoogleSignInError(state: AuthUiState, errorMessage: String): AuthUiState {
        return when (state) {
            is AuthUiState.SignIn -> state.copy(
                isLoading = false,
                generalError = errorMessage
            )
            is AuthUiState.SignUp -> state.copy(
                isLoading = false,
                generalError = errorMessage
            )
            else -> state
        }
    }

    fun handleGoogleIdTokenLoading(state: AuthUiState): AuthUiState {
        return when (state) {
            is AuthUiState.SignIn -> state.copy(isLoading = true, generalError = null)
            is AuthUiState.SignUp -> state.copy(isLoading = true, generalError = null)
            else -> state
        }
    }

    fun handleGoogleIdTokenFailure(state: AuthUiState, errorMessage: String): AuthUiState {
        return when (state) {
            is AuthUiState.SignIn -> state.copy(
                isLoading = false,
                generalError = errorMessage
            )
            is AuthUiState.SignUp -> state.copy(
                isLoading = false,
                generalError = errorMessage
            )
            else -> state
        }
    }
}
