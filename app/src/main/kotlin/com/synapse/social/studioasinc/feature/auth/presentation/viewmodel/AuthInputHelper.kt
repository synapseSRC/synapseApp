package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import com.synapse.social.studioasinc.feature.auth.ui.models.AuthField
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength

object AuthInputHelper {
    fun handleEmailChanged(state: AuthUiState, email: String, isValid: Boolean): AuthUiState {
        return when (state) {
            is AuthUiState.SignIn -> state.copy(
                email = email,
                isEmailValid = isValid,
                validationErrors = state.validationErrors - AuthField.EMAIL
            )
            is AuthUiState.SignUp -> state.copy(
                email = email,
                isEmailValid = isValid,
                validationErrors = state.validationErrors - AuthField.EMAIL
            )
            is AuthUiState.ForgotPassword -> state.copy(
                email = email,
                isEmailValid = isValid,
                validationErrors = state.validationErrors - AuthField.EMAIL
            )
            else -> state
        }
    }

    fun handlePasswordChanged(state: AuthUiState, password: String, strength: PasswordStrength): AuthUiState {
        return when (state) {
            is AuthUiState.SignIn -> state.copy(
                password = password,
                validationErrors = state.validationErrors - AuthField.PASSWORD
            )
            is AuthUiState.SignUp -> state.copy(
                password = password,
                passwordStrength = strength,
                validationErrors = state.validationErrors - AuthField.PASSWORD
            )
            is AuthUiState.ResetPassword -> state.copy(
                password = password,
                validationErrors = state.validationErrors - AuthField.PASSWORD
            )
            else -> state
        }
    }

    fun handleGoogleSignInError(state: AuthUiState, errorMessage: String): AuthUiState {
        return when (state) {
            is AuthUiState.SignIn -> state.copy(
                isLoading = false,
                generalError = errorMessage,
                isErrorDismissed = false
            )
            is AuthUiState.SignUp -> state.copy(
                isLoading = false,
                generalError = errorMessage,
                isErrorDismissed = false
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
                generalError = errorMessage,
                isErrorDismissed = false
            )
            is AuthUiState.SignUp -> state.copy(
                isLoading = false,
                generalError = errorMessage,
                isErrorDismissed = false
            )
            else -> state
        }
    }
}
