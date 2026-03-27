package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult

class ValidateNewPasswordUseCase(
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    data class ValidationResultGroup(
        val isValid: Boolean,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null
    )

    operator fun invoke(password: String, confirmPassword: String): ValidationResultGroup {
        val passwordValidation = validatePasswordUseCase(password)
        val passwordError = (passwordValidation as? ValidationResult.Invalid)?.errorMessage

        val confirmPasswordError = if (password != confirmPassword) {
            "Passwords do not match"
        } else {
            null
        }

        return ValidationResultGroup(
            isValid = passwordError == null && confirmPasswordError == null,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError
        )
    }
}
