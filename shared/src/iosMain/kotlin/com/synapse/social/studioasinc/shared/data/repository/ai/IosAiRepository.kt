package com.synapse.social.studioasinc.shared.data.repository.ai

import com.synapse.social.studioasinc.shared.domain.repository.ai.AiRepository

class IosAiRepository : AiRepository {
    override suspend fun generateSmartReplies(recentMessages: List<String>): Result<List<String>> {
        return Result.success(emptyList())
    }

    override suspend fun summarizeChat(messages: List<String>): Result<String> {
        return Result.success("Not implemented on iOS")
    }

    override suspend fun isContentToxic(text: String): Result<Boolean> {
        return Result.success(false)
    }

    override suspend fun detectSensitiveContent(mediaPath: String): Result<Boolean> {
        return Result.success(false)
    }
}
