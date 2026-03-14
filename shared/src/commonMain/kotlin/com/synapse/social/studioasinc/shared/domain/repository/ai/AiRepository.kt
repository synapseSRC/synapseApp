package com.synapse.social.studioasinc.shared.domain.repository.ai

interface AiRepository {
    suspend fun generateSmartReplies(recentMessages: List<String>): Result<List<String>>
    suspend fun summarizeChat(messages: List<String>): Result<String>
    suspend fun isContentToxic(text: String): Result<Boolean>
    suspend fun detectSensitiveContent(mediaPath: String): Result<Boolean>
}
