package com.synapse.social.studioasinc.data.repository.ai

import com.google.genai.Client
import com.synapse.social.studioasinc.domain.repository.ai.AiRepository
import com.synapse.social.studioasinc.settings.ApiKeySettingsService
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient as SynapseSupabaseClient
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private const val DEFAULT_MODEL = "gemini-3.1-flash-lite-preview"

@Serializable
private data class AiGenerateRequest(
    val prompt: String,
    val model: String? = null
)

@Serializable
private data class AiGenerateResponse(
    val text: String? = null,
    val error: String? = null
)

class AiRepositoryImpl(
    private val apiKeySettingsService: ApiKeySettingsService,
    private val supabaseClient: SupabaseClient = SynapseSupabaseClient.client
) : AiRepository {
    private suspend fun resolveModelAndCustomKey(): Pair<String, String?> {
        val settings = apiKeySettingsService.getProviderSettings()
        val model = settings.customModel?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        val customKey = when (settings.preferredProvider) {
            "platform" -> null
            "gemini" -> {
                val keyInfo = apiKeySettingsService.apiKeys.value
                    .firstOrNull { it.provider == "gemini" && it.isActive }
                keyInfo?.let { apiKeySettingsService.getRawApiKey(it.id) }?.takeIf { it.isNotBlank() }
            }
            else -> if (settings.fallbackToPlatform) null
            else throw UnsupportedOperationException("Provider '" + settings.preferredProvider + "' is not yet supported")
        }
        return Pair(model, customKey)
    }

    private suspend fun generateContent(prompt: String, conversationId: String? = null): Result<String> {
        val (model, customKey) = try { resolveModelAndCustomKey() } catch (error: Exception) {
            Napier.e("Unable to resolve Gemini model configuration", error)
            SentryAgentTracing.captureSanitizedException(error)
            return Result.failure(error)
        }
        return withContext(Dispatchers.IO) {
            SentryAgentTracing.traceChat(provider = "google", model = model, conversationId = conversationId) {
                if (!customKey.isNullOrBlank()) {
                    // User provided custom Gemini API key (BYOK)
                    val client = Client.builder().apiKey(customKey).build()
                    val response = client.models.generateContent(model, prompt, null)
                    val text = response.text()
                    if (text.isNullOrBlank()) Result.failure(Exception("Empty response from Gemini"))
                    else Result.success(text.trim())
                } else {
                    // Platform managed: route through secure Supabase Edge Function without exposing key in APK
                    try {
                        val response = supabaseClient.functions.invoke(
                            function = "ai-generate",
                            body = AiGenerateRequest(prompt = prompt, model = model)
                        )
                        val responseBody = response.body<AiGenerateResponse>()
                        val text = responseBody.text
                        if (text.isNullOrBlank()) {
                            val errorMsg = responseBody.error ?: "Empty response from Gemini Edge Function"
                            Result.failure(Exception(errorMsg))
                        } else {
                            Result.success(text.trim())
                        }
                    } catch (e: Exception) {
                        Napier.e("Error invoking ai-generate Edge Function", e)
                        Result.failure(e)
                    }
                }
            }
        }
    }

    override suspend fun generateSmartReplies(recentMessages: List<String>, conversationId: String?): Result<List<String>> {
        if (recentMessages.isEmpty()) return Result.success(emptyList())
        val prompt = "Suggest three short context-aware replies; output one reply per line without numbering.\n\nRecent messages:\n" + recentMessages.takeLast(5).joinToString("\n")
        return generateContent(prompt, conversationId).map { it.lines().map { line -> line.trim() }.filter { it.isNotBlank() }.take(3) }
    }

    override suspend fun summarizeChat(messages: List<String>, conversationId: String?): Result<String> {
        if (messages.isEmpty()) return Result.failure(IllegalArgumentException("Cannot summarize an empty chat."))
        val prompt = "Summarize this chat concisely. Highlight main topics, decisions, and action items.\n\nConversation:\n" + messages.joinToString("\n")
        return generateContent(prompt, conversationId)
    }

    override suspend fun summarizePost(postContent: String, comments: List<String>, conversationId: String?): Result<String> {
        val commentSection = if (comments.isNotEmpty()) "\n\nTop comments:\n" + comments.take(10).joinToString("\n") else ""
        return generateContent("Summarize this social media post in 2-3 sentences, including notable reactions.\n\nPost:\n" + postContent + commentSection, conversationId)
    }

    override suspend fun summarizeMessage(content: String, conversationId: String?): Result<String> =
        generateContent("Summarize this message:\n\n" + content, conversationId)

    override suspend fun summarizeThread(posts: List<com.synapse.social.studioasinc.domain.model.Post>, conversationId: String?): Result<String> {
        if (posts.isEmpty()) return Result.failure(IllegalArgumentException("Cannot summarize an empty thread."))
        val thread = posts.mapNotNull { it.postText }.joinToString("\n---\n")
        return generateContent("Summarize this thread into exactly three bullet points.\n\nThread:\n" + thread, conversationId)
    }
}
