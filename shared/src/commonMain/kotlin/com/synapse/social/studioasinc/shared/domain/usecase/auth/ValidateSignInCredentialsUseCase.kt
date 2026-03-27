package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult

class ValidateSignInCredentialsUseCase(
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    data class ValidationResultGroup(
        val isValid: Boolean,
        val emailError: String? = null,
        val passwordError: String? = null
    )

    operator fun invoke(email: String, password: String): ValidationResultGroup {
        val emailValidation = validateEmailUseCase(email)
        val passwordValidation = validatePasswordUseCase(password)

        val emailError = (emailValidation as? ValidationResult.Invalid)?.errorMessage
        val passwordError = (passwordValidation as? ValidationResult.Invalid)?.errorMessage

        return ValidationResultGroup(
            isValid = emailError == null && passwordError == null,
            emailError = emailError,
            passwordError = passwordError
        )
    }
}
