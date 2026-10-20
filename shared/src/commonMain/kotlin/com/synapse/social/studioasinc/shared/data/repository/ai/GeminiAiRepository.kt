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
    override suspend fun generateSmartReplies(recentMessages: List<String>, conversationId: String?): Result<List<String>> {
        val prompt = "Suggest three short context-aware replies; output one reply per line without numbering.\n\nRecent messages:\n" + recentMessages.joinToString("\n")
        return traceAiChat(provider = "google", model = MODEL, conversationId = conversationId) {
            try {
                val functionUrl = "${SynapseConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/ai-generate"
                val response: AiGenerateResponse = httpClient.post(functionUrl) {
                    contentType(ContentType.Application.Json)
                    header("apikey", SynapseConfig.SUPABASE_ANON_KEY)
                    header("Authorization", "Bearer ${SynapseConfig.SUPABASE_ANON_KEY}")
                    setBody(AiGenerateRequest(prompt = prompt, model = MODEL))
                }.body()
                val text = response.text
                    ?: return@traceAiChat Result.failure(Exception(response.error ?: "Empty response from Gemini AI service"))
                val replies = text.lines().map { it.replace(Regex("^[\\s\\d.*-]+\\s*"), "").trim() }
                    .filter { it.isNotBlank() }.take(3)
                Result.success(replies)
            } catch (error: Exception) {
                Napier.e("Error generating smart replies from Gemini AI service", error)
                Result.failure(error)
            }
        }
    }
    @Serializable private data class AiGenerateRequest(val prompt: String, val model: String)
    @Serializable private data class AiGenerateResponse(val text: String? = null, val error: String? = null)
    companion object {
        private const val MODEL = "gemini-1.5-flash"
    }
}
