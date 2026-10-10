package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult

class ValidateSignUpCredentialsUseCase(
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateUsernameUseCase: ValidateUsernameUseCase
) {
    data class ValidationResultGroup(
        val isValid: Boolean,
        val emailError: String? = null,
        val passwordError: String? = null,
        val usernameError: String? = null
    )

    operator fun invoke(email: String, password: String, username: String): ValidationResultGroup {
        val emailValidation = validateEmailUseCase(email)
        val passwordValidation = validatePasswordUseCase(password)
        val usernameValidation = validateUsernameUseCase(username)

        val emailError = (emailValidation as? ValidationResult.Invalid)?.errorMessage
        val passwordError = (passwordValidation as? ValidationResult.Invalid)?.errorMessage
        val usernameError = (usernameValidation as? ValidationResult.Invalid)?.errorMessage

        return ValidationResultGroup(
            isValid = emailError == null && passwordError == null && usernameError == null,
            emailError = emailError,
            passwordError = passwordError,
            usernameError = usernameError
        )
    }
}
