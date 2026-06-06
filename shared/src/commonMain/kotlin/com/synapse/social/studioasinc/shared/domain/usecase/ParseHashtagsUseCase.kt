package com.synapse.social.studioasinc.shared.domain.usecase

class ParseHashtagsUseCase {
    private val HASHTAG_REGEX = Regex("#([\\w]+)")

    operator fun invoke(text: String): List<String> {
        return HASHTAG_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .filter { tag ->
                // Ensure it's not just numbers
                tag.any { !it.isDigit() }
            }
            .distinct()
            .toList()
    }
}
