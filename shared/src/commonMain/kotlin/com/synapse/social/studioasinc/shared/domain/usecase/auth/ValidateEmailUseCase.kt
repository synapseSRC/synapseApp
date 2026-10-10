package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult

class ValidateEmailUseCase {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$")

    operator fun invoke(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult.Invalid("Email cannot be empty")
        }
        if (!emailRegex.matches(email)) {
            return ValidationResult.Invalid("Invalid email format")
        }
        return ValidationResult.Valid
    }
}
