package com.synapse.social.studioasinc.domain.repository.ai

interface AiRepository {
    suspend fun generateSmartReplies(recentMessages: List<String>): Result<List<String>>
    suspend fun summarizeChat(messages: List<String>): Result<String>
    suspend fun summarizePost(postContent: String, comments: List<String>): Result<String>
    suspend fun summarizeMessage(content: String): Result<String>
    suspend fun summarizeThread(posts: List<com.synapse.social.studioasinc.domain.model.Post>): Result<String>
}
