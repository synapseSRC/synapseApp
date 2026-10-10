package com.synapse.social.studioasinc.shared.data.repository.ai

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import com.synapse.social.studioasinc.shared.domain.repository.ai.AiRepository
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class GeminiAiRepository(private val httpClient: HttpClient) : AiRepository {
    private val apiKey = SynapseConfig.GEMINI_API_KEY
    override suspend fun generateSmartReplies(recentMessages: List<String>, conversationId: String?): Result<List<String>> {
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("Gemini API Key is not configured"))
        val prompt = "Suggest three short context-aware replies; output one reply per line without numbering.\n\nRecent messages:\n" + recentMessages.joinToString("\n")
        return traceAiChat(provider = "google", model = MODEL, conversationId = conversationId) {
            try {
                val response: GeminiResponse = httpClient.post(BASE_URL) {
                    contentType(ContentType.Application.Json)
                    header("x-goog-api-key", apiKey)
                    setBody(GeminiRequest(contents = listOf(Content(parts = listOf(Part(text = prompt))))))
                }.body()
                val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: return@traceAiChat Result.failure(Exception("Empty response from Gemini"))
                val replies = text.lines().map { it.replace(Regex("^[\\s\\d.*-]+\\s*"), "").trim() }
                    .filter { it.isNotBlank() }.take(3)
                Result.success(replies)
            } catch (error: Exception) {
                Napier.e("Error generating smart replies from Gemini", error)
                Result.failure(error)
            }
        }
    }
    @Serializable private data class GeminiRequest(val contents: List<Content>)
    @Serializable private data class Content(val parts: List<Part>)
    @Serializable private data class Part(val text: String)
    @Serializable private data class GeminiResponse(val candidates: List<Candidate>)
    @Serializable private data class Candidate(val content: Content)
    companion object {
        private val BASE_URL = SynapseConfig.GEMINI_API_ENDPOINT
        private val MODEL = BASE_URL.substringAfter("/models/").substringBefore(":")
    }
}
