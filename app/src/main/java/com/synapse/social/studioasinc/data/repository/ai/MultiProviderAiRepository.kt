package com.synapse.social.studioasinc.data.repository.ai

import com.synapse.social.studioasinc.domain.repository.ai.AiRepository
import com.synapse.social.studioasinc.settings.ApiKeySettingsService

class MultiProviderAiRepository(
    private val platformRepository: AiRepository,
    private val apiKeySettingsService: ApiKeySettingsService
) : AiRepository {

    private fun resolveRepository(): AiRepository {
        val settings = apiKeySettingsService.providerSettings.value
        val provider = settings.preferredProvider

        if (provider == "platform") return platformRepository

        val keyInfo = apiKeySettingsService.apiKeys.value
            .firstOrNull { it.provider == provider && it.isActive }

        if (keyInfo != null) {
            val rawKey = apiKeySettingsService.getRawApiKey(keyInfo.id)
            if (!rawKey.isNullOrBlank()) {
                return when (provider) {
                    "openai"     -> OpenAiRepositoryImpl(rawKey)
                    "anthropic"  -> AnthropicRepositoryImpl(rawKey)
                    "openrouter" -> OpenRouterRepositoryImpl(rawKey)
                    "gemini"     -> platformRepository
                    else         -> platformRepository
                }
            }
        }

        return if (settings.fallbackToPlatform) platformRepository
        else throw IllegalStateException("No active API key for provider '$provider' and fallback is disabled.")
    }

    override suspend fun generateSmartReplies(recentMessages: List<String>) =
        resolveRepository().generateSmartReplies(recentMessages)

    override suspend fun summarizeChat(messages: List<String>) =
        resolveRepository().summarizeChat(messages)

    override suspend fun summarizePost(postContent: String, comments: List<String>) =
        resolveRepository().summarizePost(postContent, comments)

    override suspend fun summarizeMessage(content: String) =
        resolveRepository().summarizeMessage(content)

    override suspend fun summarizeThread(posts: List<com.synapse.social.studioasinc.domain.model.Post>) =
        resolveRepository().summarizeThread(posts)
}
