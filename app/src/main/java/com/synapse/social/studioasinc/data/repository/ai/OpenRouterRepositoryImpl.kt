package com.synapse.social.studioasinc.data.repository.ai

import com.synapse.social.studioasinc.domain.repository.ai.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenRouterRepositoryImpl(private val apiKey: String) : AiRepository {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun chat(systemPrompt: String, userPrompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = buildJsonObject {
                    put("model", "openai/gpt-4o-mini")
                    putJsonArray("messages") {
                        addJsonObject { put("role", "system"); put("content", systemPrompt) }
                        addJsonObject { put("role", "user"); put("content", userPrompt) }
                    }
                }.toString()

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("OpenRouter error ${response.code}: $responseBody"))

                val text = json.parseToJsonElement(responseBody)
                    .jsonObject["choices"]?.jsonArray?.get(0)
                    ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: ""
                Result.success(text.trim())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun generateSmartReplies(recentMessages: List<String>): Result<List<String>> {
        if (recentMessages.isEmpty()) return Result.success(emptyList())
        return chat(
            "You suggest short smart replies. Respond with exactly 3 replies, one per line, no numbering.",
            "Recent messages:\n${recentMessages.joinToString("\n")}"
        ).map { it.lines().map(String::trim).filter(String::isNotBlank).take(3) }
    }

    override suspend fun summarizeChat(messages: List<String>): Result<String> {
        if (messages.isEmpty()) return Result.failure(IllegalArgumentException("Empty chat"))
        return chat(
            "You summarize conversations concisely.",
            "Summarize this conversation:\n${messages.joinToString("\n")}"
        )
    }

    override suspend fun summarizePost(postContent: String, comments: List<String>): Result<String> {
        if (postContent.isBlank()) return Result.failure(IllegalArgumentException("Empty content"))
        return chat(
            "You summarize social media posts in 1-2 sentences.",
            postContent
        )
    }

    override suspend fun summarizeMessage(content: String): Result<String> = summarizePost(content, emptyList())

    override suspend fun summarizeThread(posts: List<com.synapse.social.studioasinc.domain.model.Post>): Result<String> {
        if (posts.isEmpty()) return Result.failure(IllegalArgumentException("Empty thread"))
        val threadContent = posts.mapNotNull { it.postText }.joinToString("\n---\n")
        return chat(
            "You summarize threads into exactly 3 bullet points.",
            "Summarize this thread:\n$threadContent"
        )
    }
}
