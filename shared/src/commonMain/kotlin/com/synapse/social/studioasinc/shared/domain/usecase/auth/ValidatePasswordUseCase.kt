package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult

class ValidatePasswordUseCase {
    operator fun invoke(password: String): ValidationResult {
        if (password.length < 8) {
            return ValidationResult.Invalid("Password must be at least 8 characters")
        }
        return ValidationResult.Valid
    }
}
