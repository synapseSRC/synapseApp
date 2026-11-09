package com.synapse.social.studioasinc.feature.inbox.inbox.models

import androidx.annotation.StringRes

/**
 * Immutable UI state for the poll creation screen.
 *
 * The authoritative validation rules (question length, 2..10 unique options, per-option length)
 * live in `CreatePollUseCase`; [canSend] only mirrors them so the send button's enabled state and
 * the inline character counters stay honest without duplicating business logic in composables.
 */
data class CreatePollUiState(
    val question: String = "",
    val options: List<String> = listOf("", ""),
    val allowMultipleAnswers: Boolean = false,
    val isSending: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val sendSuccess: Boolean = false
) {
    /** Non-blank, trimmed options the user has actually filled in. */
    val filledOptions: List<String>
        get() = options.map { it.trim() }.filter { it.isNotEmpty() }

    /** True when the form satisfies the same rules the use case enforces. */
    val canSend: Boolean
        get() {
            if (isSending) return false
            if (question.isBlank()) return false
            val filled = filledOptions
            if (filled.size < MIN_OPTIONS) return false
            // Mirror the use case's case-insensitive uniqueness rule.
            return filled.distinctBy { it.lowercase() }.size == filled.size
        }

    companion object {
        const val MAX_QUESTION_LENGTH = 255
        const val MAX_OPTION_LENGTH = 80
        const val MIN_OPTIONS = 2
        const val MAX_OPTIONS = 10
    }
}
