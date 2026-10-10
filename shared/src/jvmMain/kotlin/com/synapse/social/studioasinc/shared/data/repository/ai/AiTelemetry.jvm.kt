package com.synapse.social.studioasinc.shared.data.repository.ai

internal actual suspend fun <T> traceAiChat(provider: String, model: String, conversationId: String?, request: suspend () -> Result<T>): Result<T> = request()
