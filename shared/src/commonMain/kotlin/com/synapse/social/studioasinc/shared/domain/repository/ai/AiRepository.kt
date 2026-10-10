package com.synapse.social.studioasinc.shared.domain.repository.ai

interface AiRepository {
    suspend fun generateSmartReplies(recentMessages: List<String>, conversationId: String? = null): Result<List<String>>
}
