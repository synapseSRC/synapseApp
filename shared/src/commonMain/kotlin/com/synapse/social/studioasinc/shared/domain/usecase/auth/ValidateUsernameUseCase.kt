package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult

class ValidateUsernameUseCase {
    private val usernameRegex = Regex("^[a-zA-Z0-9_]+$")

    operator fun invoke(username: String): ValidationResult {
        if (username.isBlank()) {
            return ValidationResult.Invalid("Username cannot be empty")
        }
        if (username.length < 3) {
            return ValidationResult.Invalid("Username must be at least 3 characters")
        }
        if (username.length > 20) {
            return ValidationResult.Invalid("Username must be at most 20 characters")
        }
        if (!usernameRegex.matches(username)) {
            return ValidationResult.Invalid("Username can only contain letters, numbers, and underscores")
        }
        return ValidationResult.Valid
    }
}
