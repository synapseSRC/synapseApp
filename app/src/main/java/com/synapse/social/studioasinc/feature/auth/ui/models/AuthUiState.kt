package com.synapse.social.studioasinc.feature.auth.ui.models

import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength

sealed class AuthUiState {

    object Initial : AuthUiState()

    object Loading : AuthUiState()

    data class SignIn(
        val email: String = "",
        val password: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val generalError: String? = null,
        val isEmailValid: Boolean = false,
        val isLoading: Boolean = false // Added isLoading field to support inline loading state
    ) : AuthUiState()

    data class SignUp(
        val email: String = "",
        val password: String = "",
        val username: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val usernameError: String? = null,
        val generalError: String? = null,
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
        val isLoading: Boolean = false // Added isLoading field
    ) : AuthUiState()

    data class ForgotPassword(
        val email: String = "",
        val emailError: String? = null,
        val isEmailValid: Boolean = false,
        val isEmailSent: Boolean = false,
        val isLoading: Boolean = false // Added isLoading field
    ) : AuthUiState()

    data class ResetPassword(
        val password: String = "",
        val confirmPassword: String = "",
        val passwordError: String? = null,
        val confirmPasswordError: String? = null,
        val passwordStrength: PasswordStrength = PasswordStrength.Weak,
        val isLoading: Boolean = false // Added isLoading field
    ) : AuthUiState()

    data class Success(val message: String) : AuthUiState()

    data class Error(val message: String) : AuthUiState()
}
