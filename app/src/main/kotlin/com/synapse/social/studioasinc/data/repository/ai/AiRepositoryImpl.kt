package com.synapse.social.studioasinc.data.repository.ai

import com.google.genai.Client
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.domain.repository.ai.AiRepository
import com.synapse.social.studioasinc.settings.ApiKeySettingsService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_MODEL = "gemini-3.1-flash-lite-preview"

class AiRepositoryImpl(private val apiKeySettingsService: ApiKeySettingsService) : AiRepository {
    private suspend fun resolveModelAndKey(): Pair<String, String> {
        val settings = apiKeySettingsService.getProviderSettings()
        val model = settings.customModel?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        return when (settings.preferredProvider) {
            "platform" -> Pair(BuildConfig.GEMINI_API_KEY, DEFAULT_MODEL)
            "gemini" -> Pair(BuildConfig.GEMINI_API_KEY, model)
            else -> if (settings.fallbackToPlatform) Pair(BuildConfig.GEMINI_API_KEY, DEFAULT_MODEL)
            else throw UnsupportedOperationException("Provider '" + settings.preferredProvider + "' is not yet supported")
        }
    }

    private suspend fun generateContent(prompt: String, conversationId: String? = null): Result<String> {
        val (apiKey, model) = try { resolveModelAndKey() } catch (error: Exception) {
            Napier.e("Unable to resolve Gemini model configuration", error)
            SentryAgentTracing.captureSanitizedException(error)
            return Result.failure(error)
        }
        return withContext(Dispatchers.IO) {
            SentryAgentTracing.traceChat(provider = "google", model = model, conversationId = conversationId) {
                val client = Client.builder().apiKey(apiKey).build()
                val response = client.models.generateContent(model, prompt, null)
                val text = response.text()
                if (text.isNullOrBlank()) Result.failure(Exception("Empty response from Gemini"))
                else Result.success(text.trim())
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
