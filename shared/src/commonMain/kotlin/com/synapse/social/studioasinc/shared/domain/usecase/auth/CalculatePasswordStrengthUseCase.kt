package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength

class CalculatePasswordStrengthUseCase {
    operator fun invoke(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.Weak

        var score = 0
        if (password.length >= 10) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score >= 5 -> PasswordStrength.Strong
            score >= 3 -> PasswordStrength.Fair
            else -> PasswordStrength.Weak
        }
    }
}
