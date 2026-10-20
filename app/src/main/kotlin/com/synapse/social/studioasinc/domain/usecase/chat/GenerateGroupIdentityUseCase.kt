package com.synapse.social.studioasinc.domain.usecase.chat

import com.synapse.social.studioasinc.domain.repository.ai.AiRepository
import javax.inject.Inject

/** Requests and validates an AI-authored group identity without exposing provider details to UI. */
class GenerateGroupIdentityUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(purpose: String, memberCount: Int): Result<GroupIdentitySuggestion> {
        if (purpose.isBlank() || memberCount < 1) {
            return Result.failure(IllegalArgumentException("A purpose and at least one participant are required."))
        }
        val prompt = """
            Propose a concise identity for a private social group.
            Purpose: ${purpose.trim()}
            Selected participants: ${memberCount}
            Return exactly three plain-text lines with these labels:
            Name: a short group title (maximum 60 characters)
            Description: a concise description and one respectful group guideline (maximum 240 characters)
            Icon concept: one simple symbol concept (maximum 50 characters)
            Do not include markdown, numbering, personal details, or extra lines.
        """.trimIndent()
        return aiRepository.summarizeMessage(prompt).mapCatching { response -> GroupIdentityParser.parse(response) }
    }
}

data class GroupIdentitySuggestion(
    val name: String,
    val description: String,
    val iconConcept: String
)

/** Strictly parses the bounded response format so model output is never used as unchecked UI state. */
object GroupIdentityParser {
    fun parse(response: String): GroupIdentitySuggestion {
        val values = response.lineSequence().mapNotNull { line ->
            val split = line.trim().split(":", limit = 2)
            if (split.size != 2) null else split[0].trim().lowercase() to split[1].trim()
        }.toMap()
        val name = values["name"].orEmpty().take(60).trim()
        val description = values["description"].orEmpty().take(240).trim()
        val icon = values["icon concept"].orEmpty().take(50).trim()
        require(name.isNotBlank() && description.isNotBlank() && icon.isNotBlank()) {
            "The AI response did not contain a complete group identity."
        }
        return GroupIdentitySuggestion(name, description, icon)
    }
}
