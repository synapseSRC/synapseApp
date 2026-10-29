package com.synapse.social.studioasinc.domain.usecase.about

import javax.inject.Inject

class ValidateFeedbackUseCase @Inject constructor() {
    operator fun invoke(email: String, description: String): Result<Unit> {
        if (description.isBlank()) {
            return Result.failure(FeedbackValidationException(FeedbackValidationIssue.DescriptionRequired))
        }
        if (email.isNotBlank() && !isValidEmail(email.trim())) {
            return Result.failure(FeedbackValidationException(FeedbackValidationIssue.InvalidEmail))
        }
        return Result.success(Unit)
    }

    private fun isValidEmail(email: String): Boolean {
        val parts = email.split('@')
        if (parts.size != 2) return false
        val local = parts[0]
        val domain = parts[1]
        return local.isNotBlank() && domain.contains('.') &&
            !local.any { it.isWhitespace() } && !domain.any { it.isWhitespace() } &&
            !domain.startsWith('.') && !domain.endsWith('.')
    }
}

enum class FeedbackValidationIssue { DescriptionRequired, InvalidEmail }

class FeedbackValidationException(val issue: FeedbackValidationIssue) : IllegalArgumentException(issue.name)
