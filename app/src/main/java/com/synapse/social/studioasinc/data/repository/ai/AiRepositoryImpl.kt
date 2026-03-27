package com.synapse.social.studioasinc.data.repository.ai

import com.google.genai.Client
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.domain.repository.ai.AiRepository
import com.synapse.social.studioasinc.settings.ApiKeySettingsService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_MODEL = "gemini-3.1-flash-lite-preview"

class AiRepositoryImpl(
    private val apiKeySettingsService: ApiKeySettingsService
) : AiRepository {

    private suspend fun resolveModelAndKey(): Pair<String, String> {
        val settings = apiKeySettingsService.getProviderSettings()
        val model = settings.customModel?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        return when (settings.preferredProvider) {
            "platform" -> Pair(BuildConfig.GEMINI_API_KEY, DEFAULT_MODEL)
            "gemini" -> Pair(BuildConfig.GEMINI_API_KEY, model)
            else -> if (settings.fallbackToPlatform) {
                Pair(BuildConfig.GEMINI_API_KEY, DEFAULT_MODEL)
            } else {
                throw UnsupportedOperationException("Provider '${settings.preferredProvider}' is not yet supported")
            }
        }
    }

    override suspend fun generateSmartReplies(recentMessages: List<String>): Result<List<String>> = withContext(Dispatchers.IO) {
        if (recentMessages.isEmpty()) return@withContext Result.success(emptyList())
        val lastMessages = recentMessages.takeLast(5)
        try {
            val (apiKey, model) = resolveModelAndKey()
            val client = Client.builder().apiKey(apiKey).build()
            val prompt = """
                Based on the following recent chat messages, suggest 3 short, context-aware smart replies.
                Provide only the replies, each on a new line, without any numbering or extra text.

                Recent messages:
                ${lastMessages.joinToString("\n")}
            """.trimIndent()
            val response = client.models.generateContent(model, prompt, null)
            val text = response.text() ?: return@withContext Result.failure(Exception("Empty response"))
            Result.success(text.lines().map { it.trim() }.filter { it.isNotBlank() }.take(3))
        } catch (e: Exception) {
            Napier.e("Error generating smart replies", e)
            Result.failure(e)
        }
    }

    override suspend fun summarizeChat(messages: List<String>): Result<String> = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Cannot summarize an empty chat."))
        try {
            val (apiKey, model) = resolveModelAndKey()
            val client = Client.builder().apiKey(apiKey).build()
            val prompt = """
                Summarize the following chat conversation concisely in a few sentences.
                Highlight the main topics discussed and any key decisions or action items.

                Conversation:
                ${messages.joinToString("\n")}
            """.trimIndent()
            val response = client.models.generateContent(model, prompt, null)
            val text = response.text() ?: return@withContext Result.failure(Exception("Empty response"))
            Result.success(text.trim())
        } catch (e: Exception) {
            Napier.e("Error summarizing chat", e)
            Result.failure(e)
        }
    }

    override suspend fun summarizePost(postContent: String, comments: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (apiKey, model) = resolveModelAndKey()
            val client = Client.builder().apiKey(apiKey).build()
            val commentSection = if (comments.isNotEmpty())
                "\n\nTop comments:\n" + comments.take(10).joinToString("\n")
            else ""
            val prompt = """
                Summarize the following social media post concisely in 2-3 sentences.
                Capture the main point and any notable reactions from comments.

                Post:
                $postContent$commentSection
            """.trimIndent()
            val response = client.models.generateContent(model, prompt, null)
            val text = response.text() ?: return@withContext Result.failure(Exception("Empty response"))
            Result.success(text.trim())
        } catch (e: Exception) {
            Napier.e("Error summarizing post", e)
            Result.failure(e)
        }
    }

    override suspend fun summarizeMessage(content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (apiKey, model) = resolveModelAndKey()
            val client = Client.builder().apiKey(apiKey).build()
            val response = client.models.generateContent(model, "Summarize this message:\n\n$content", null)
            val text = response.text() ?: return@withContext Result.failure(Exception("Empty response"))
            Result.success(text.trim())
        } catch (e: Exception) {
            Napier.e("Error summarizing message", e)
            Result.failure(e)
        }
    }

    override suspend fun summarizeThread(posts: List<com.synapse.social.studioasinc.domain.model.Post>): Result<String> = withContext(Dispatchers.IO) {
        if (posts.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Cannot summarize an empty thread."))
        try {
            val (apiKey, model) = resolveModelAndKey()
            val client = Client.builder().apiKey(apiKey).build()

            val threadContent = posts.mapNotNull { it.postText }.joinToString("\n---\n")
            val prompt = """
                Summarize the following thread of posts into exactly 3 bullet points.
                Focus on the main narrative, arguments, or key details discussed.

                Thread:
                $threadContent
            """.trimIndent()

            val response = client.models.generateContent(model, prompt, null)
            val text = response.text() ?: return@withContext Result.failure(Exception("Empty response"))
            Result.success(text.trim())
        } catch (e: Exception) {
            Napier.e("Error summarizing thread: ${e.message}", e)
            Result.failure(e)
        }
    }
}
