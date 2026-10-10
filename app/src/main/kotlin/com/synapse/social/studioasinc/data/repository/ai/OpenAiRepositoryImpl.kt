package com.synapse.social.studioasinc.data.repository.ai

import com.synapse.social.studioasinc.domain.repository.ai.AiRepository
import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAiRepositoryImpl(private val apiKey: String) : AiRepository {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun chat(systemPrompt: String, userPrompt: String, conversationId: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            SentryAgentTracing.traceChat(provider = "openai", model = "gpt-4o-mini", conversationId = conversationId) {
                val body = buildJsonObject { put("model", "gpt-4o-mini"); putJsonArray("messages") { addJsonObject { put("role", "system"); put("content", systemPrompt) }; addJsonObject { put("role", "user"); put("content", userPrompt) } } }.toString()
                val request = Request.Builder()
                    .url(SynapseConfig.OPENAI_API_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                    ?: return@traceChat Result.failure(Exception("Empty response from OpenAI"))
                if (!response.isSuccessful) return@traceChat Result.failure(Exception("OpenAI error " + response.code + ": " + responseBody.take(512)))
                val text = json.parseToJsonElement(responseBody).jsonObject["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                if (text.isNullOrBlank()) Result.failure(Exception("Empty response from OpenAI"))
                else Result.success(text.trim())
            }
        }

    override suspend fun generateSmartReplies(recentMessages: List<String>, conversationId: String?): Result<List<String>> {
        if (recentMessages.isEmpty()) return Result.success(emptyList())
        val prompt = "Recent messages:\n" + recentMessages.takeLast(5).joinToString("\n")
        return chat("You suggest short smart replies. Respond with exactly 3 replies, one per line, no numbering.", prompt, conversationId)
            .map { it.lines().map(String::trim).filter(String::isNotBlank).take(3) }
    }

    override suspend fun summarizeChat(messages: List<String>, conversationId: String?): Result<String> {
        if (messages.isEmpty()) return Result.failure(IllegalArgumentException("Empty chat"))
        return chat("You summarize conversations concisely.", "Summarize this conversation:\n" + messages.joinToString("\n"), conversationId)
    }

    override suspend fun summarizePost(postContent: String, comments: List<String>, conversationId: String?): Result<String> {
        if (postContent.isBlank()) return Result.failure(IllegalArgumentException("Empty content"))
        return chat("You summarize social media posts in 1-2 sentences.", postContent, conversationId)
    }

    override suspend fun summarizeMessage(content: String, conversationId: String?): Result<String> =
        chat("You summarize social media posts in 1-2 sentences.", content, conversationId)

    override suspend fun summarizeThread(posts: List<com.synapse.social.studioasinc.domain.model.Post>, conversationId: String?): Result<String> {
        if (posts.isEmpty()) return Result.failure(IllegalArgumentException("Empty thread"))
        val threadContent = posts.mapNotNull { it.postText }.joinToString("\n---\n")
        return chat("You summarize threads into exactly 3 bullet points.", "Summarize this thread:\n" + threadContent, conversationId)
    }
}
