package com.synapse.social.studioasinc.domain.repository.ai

interface AiRepository {
    suspend fun generateSmartReplies(recentMessages: List<String>, conversationId: String?): Result<List<String>>
    suspend fun summarizeChat(messages: List<String>, conversationId: String? = null): Result<String>
    suspend fun summarizePost(postContent: String, comments: List<String>, conversationId: String? = null): Result<String>
    suspend fun summarizeMessage(content: String, conversationId: String? = null): Result<String>
    suspend fun summarizeThread(posts: List<com.synapse.social.studioasinc.domain.model.Post>, conversationId: String? = null): Result<String>
}
