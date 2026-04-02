package com.synapse.social.studioasinc.feature.auth.ui.models

import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength

enum class AuthField {
    EMAIL, PASSWORD, USERNAME, CONFIRM_PASSWORD
}

sealed class AuthUiState {

    object Initial : AuthUiState()

    object Loading : AuthUiState()

    data class SignIn(
        val email: String = "",
        val password: String = "",
        val validationErrors: Map<AuthField, String?> = emptyMap(),
        val generalError: String? = null,
        val isErrorDismissed: Boolean = false,
        val isEmailValid: Boolean = false,
        val isLoading: Boolean = false
    ) : AuthUiState()

    data class SignUp(
        val email: String = "",
        val password: String = "",
        val username: String = "",
        val validationErrors: Map<AuthField, String?> = emptyMap(),
        val generalError: String? = null,
        val isErrorDismissed: Boolean = false,
        val isEmailValid: Boolean = false,
        val passwordStrength: PasswordStrength = PasswordStrength.Weak,
        val isCheckingUsername: Boolean = false,
        val isLoading: Boolean = false,
        val showSuccessDialog: Boolean = false
    ) : AuthUiState()

    data class EmailVerification(
        val email: String,
        val canResend: Boolean = true,
        val resendCooldownSeconds: Int = 0,
        val isResent: Boolean = false,
        val isLoading: Boolean = false
    ) : AuthUiState()

    data class ForgotPassword(
        val email: String = "",
        val validationErrors: Map<AuthField, String?> = emptyMap(),
        val isEmailValid: Boolean = false,
        val isEmailSent: Boolean = false,
        val isLoading: Boolean = false
    ) : AuthUiState()

    data class ResetPassword(
        val password: String = "",
        val confirmPassword: String = "",
        val validationErrors: Map<AuthField, String?> = emptyMap(),
        val passwordStrength: PasswordStrength = PasswordStrength.Weak,
        val isLoading: Boolean = false
    ) : AuthUiState()

    data class Success(val message: String) : AuthUiState()

    data class Error(val message: String) : AuthUiState()
}
