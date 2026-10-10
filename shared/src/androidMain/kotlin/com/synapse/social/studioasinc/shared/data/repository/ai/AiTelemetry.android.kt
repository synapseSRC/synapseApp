package com.synapse.social.studioasinc.shared.data.repository.ai

import com.synapse.social.studioasinc.shared.BuildConfig
import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.TracesSamplingDecision
import io.sentry.TransactionContext
import kotlinx.coroutines.CancellationException
import java.security.MessageDigest
import kotlin.random.Random

internal actual suspend fun <T> traceAiChat(provider: String, model: String, conversationId: String?, request: suspend () -> Result<T>): Result<T> {
    val rate = BuildConfig.SENTRY_AGENT_TRACE_SAMPLE_RATE.coerceIn(0.0, 1.0)
    val transaction = Sentry.startTransaction(TransactionContext("invoke_agent Synapse AI Assistant", "gen_ai.invoke_agent", TracesSamplingDecision(Random.nextDouble() < rate)))
    val span = transaction.startChild("gen_ai.chat", "chat " + model)
    transaction.setData("gen_ai.operation.name", "invoke_agent")
    transaction.setData("gen_ai.agent.name", "Synapse AI Assistant")
    span.setData("gen_ai.operation.name", "chat")
    span.setData("gen_ai.agent.name", "Synapse AI Assistant")
    span.setData("gen_ai.provider.name", provider)
    span.setData("gen_ai.request.model", model)
    conversationIdForSentry(conversationId)?.let { id -> transaction.setData("gen_ai.conversation.id", id); span.setData("gen_ai.conversation.id", id) }
    try {
        val result = request()
        val error = result.exceptionOrNull()
        if (error == null) { transaction.setStatus(SpanStatus.OK); span.setStatus(SpanStatus.OK) }
        else { captureSanitizedException(error); markFailure(transaction, error); markFailure(span, error) }
        return result
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        captureSanitizedException(error); markFailure(transaction, error); markFailure(span, error)
        return Result.failure(error)
    } finally {
        span.finish(); transaction.finish()
    }
}
private fun conversationIdForSentry(raw: String?): String? {
    val value = raw?.takeIf { Regex("[A-Za-z0-9_-]{1,200}").matches(it) } ?: return null
    val digest = MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray())
    val hex = "0123456789abcdef"
    return buildString(digest.size * 2) { digest.forEach { b -> val v = b.toInt() and 0xff; append(hex[v ushr 4]); append(hex[v and 15]) } }
}
private fun captureSanitizedException(error: Throwable) {
    if (error is CancellationException) return
    val safe = RuntimeException("AI request failed (" + error.javaClass.simpleName.ifBlank { "Exception" } + ")")
    safe.stackTrace = error.stackTrace
    Sentry.captureException(safe)
}
private fun markFailure(span: ISpan, error: Throwable) {
    span.setStatus(SpanStatus.INTERNAL_ERROR)
    span.setData("error.type", error.javaClass.simpleName.ifBlank { "Exception" })
}
