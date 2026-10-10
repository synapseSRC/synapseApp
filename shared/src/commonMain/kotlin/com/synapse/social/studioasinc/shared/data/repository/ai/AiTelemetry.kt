package com.synapse.social.studioasinc.shared.data.repository.ai

/** Platform-owned trace hook; commonMain stays free of platform SDK imports. */
internal expect suspend fun <T> traceAiChat(provider: String, model: String, conversationId: String?, request: suspend () -> Result<T>): Result<T>
