package com.synapse.social.studioasinc.data.repository.ai

import com.synapse.social.studioasinc.BuildConfig
import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.protocol.SentryTransaction
import io.sentry.SpanStatus
import io.sentry.TracesSamplingDecision
import io.sentry.TransactionContext
import kotlinx.coroutines.CancellationException
import java.security.MessageDigest
import kotlin.random.Random

/** Metadata-only GenAI spans; prompt and response bodies are never stored in Sentry. */
internal object SentryAgentTracing {
    private const val AGENT_NAME = "Synapse AI Assistant"
    private val safeConversationId = Regex("[A-Za-z0-9_-]{1,200}")
    private val contentAttributeKeys = setOf(
        "gen_ai.input.messages", "gen_ai.output.messages", "gen_ai.system_instructions",
        "gen_ai.response.text", "gen_ai.response.object", "gen_ai.request.messages",
        "gen_ai.tool.definitions", "gen_ai.tool.call.arguments", "gen_ai.tool.call.result"
    )

    suspend fun traceChat(provider: String, model: String, conversationId: String? = null, request: suspend () -> Result<String>): Result<String> {
        val transaction = Sentry.startTransaction(TransactionContext("invoke_agent " + AGENT_NAME, "gen_ai.invoke_agent", TracesSamplingDecision(Random.nextDouble() < BuildConfig.SENTRY_AGENT_TRACE_SAMPLE_RATE.coerceIn(0.0, 1.0))))
        val modelSpan = transaction.startChild("gen_ai.chat", "chat " + model)
        transaction.setData("gen_ai.operation.name", "invoke_agent")
        transaction.setData("gen_ai.agent.name", AGENT_NAME)
        modelSpan.setData("gen_ai.operation.name", "chat")
        modelSpan.setData("gen_ai.agent.name", AGENT_NAME)
        modelSpan.setData("gen_ai.provider.name", provider)
        modelSpan.setData("gen_ai.request.model", model)
        conversationIdForSentry(conversationId)?.let { id -> transaction.setData("gen_ai.conversation.id", id); modelSpan.setData("gen_ai.conversation.id", id) }
        try {
            val result = request()
            val error = result.exceptionOrNull()
            if (error == null) { transaction.setStatus(SpanStatus.OK); modelSpan.setStatus(SpanStatus.OK) }
            else { captureSanitizedException(error); markFailure(transaction, error); markFailure(modelSpan, error) }
            return result
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            captureSanitizedException(error)
            markFailure(transaction, error)
            markFailure(modelSpan, error)
            return Result.failure(error)
        } finally {
            modelSpan.finish()
            transaction.finish()
        }
    }

    internal fun conversationIdForSentry(conversationId: String?): String? {
        val value = conversationId?.takeIf { safeConversationId.matches(it) } ?: return null
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        val hex = "0123456789abcdef"
        return buildString(digest.size * 2) { digest.forEach { b -> val n = b.toInt() and 0xff; append(hex[n ushr 4]); append(hex[n and 15]) } }
    }

    internal fun sanitizedException(error: Throwable): RuntimeException {
        val type = error.javaClass.simpleName.ifBlank { "Exception" }
        return RuntimeException("AI request failed (" + type + ")").apply { stackTrace = error.stackTrace }
    }

    internal fun captureSanitizedException(error: Throwable) {
        if (error !is CancellationException) Sentry.captureException(sanitizedException(error))
    }

    internal fun hasUnsafeGenAiContent(transaction: SentryTransaction): Boolean {
        val rootData = transaction.contexts.trace?.data
        if (containsContent(rootData)) return true
        return transaction.spans.orEmpty().any { containsContent(it.data) }
    }

    private fun containsContent(data: Map<String, Any?>?): Boolean = data?.keys?.any { it in contentAttributeKeys } == true

    private fun markFailure(span: ISpan, error: Throwable) {
        span.setStatus(SpanStatus.INTERNAL_ERROR)
        span.setData("error.type", error.javaClass.simpleName.ifBlank { "Exception" })
    }
}
