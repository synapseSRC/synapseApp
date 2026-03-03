package com.synapse.social.studioasinc.shared.domain.model

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errorMessage: String) : ValidationResult()
}
