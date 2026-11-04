package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoFunctionErrorDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoMediaCredentialsDto
import com.synapse.social.studioasinc.shared.data.mapper.echo.EchoMapper
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoBackendException
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoMediaCredentials
import com.synapse.social.studioasinc.shared.domain.repository.EchoTokenRepository
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Fetches room-scoped LiveKit credentials from the `livekit-token` Edge Function.
 *
 * The app never holds a LiveKit secret: the function derives the grant from the caller's JWT and
 * the membership row Postgres already has. Tokens are short-lived, so nothing is cached here —
 * every (re)connect asks for a fresh one.
 */
class SupabaseEchoTokenRepository(
    private val client: SupabaseClientLib = SupabaseClient.client
) : EchoTokenRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun mint(roomId: String): Result<EchoMediaCredentials> =
        try {
            withContext(AppDispatchers.IO) {
                val response = client.functions.invoke(FUNCTION_NAME) {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject { put("room_id", roomId) }.toString())
                }
                val text = response.bodyAsText()
                val status = response.status.value
                if (status !in 200..299) {
                    Result.failure(rejection(status, text))
                } else {
                    val dto = json.decodeFromString(EchoMediaCredentialsDto.serializer(), text)
                    Result.success(EchoMapper.toDomain(dto))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("livekit-token request failed: ${e.message}", tag = TAG)
            Result.failure(
                EchoBackendException(
                    EchoErrorKind.AUDIO_FAILED,
                    "Could not obtain Echo audio credentials: ${e.message ?: e::class.simpleName}"
                )
            )
        }

    private fun rejection(status: Int, text: String): EchoBackendException {
        val code = try {
            json.decodeFromString(EchoFunctionErrorDto.serializer(), text).error
        } catch (e: Exception) {
            null
        }
        val detail = text.take(MAX_ERROR_BODY_CHARS)
        val kind = when {
            status == 401 -> EchoErrorKind.NOT_AUTHENTICATED
            status == 403 -> EchoErrorKind.FORBIDDEN
            else -> EchoErrorKind.AUDIO_FAILED
        }
        // The body is only read on a non-2xx response, where it cannot contain a token.
        return EchoBackendException(
            kind,
            "Echo audio is unavailable (HTTP $status${code?.let { ", $it" } ?: ""}): $detail"
        )
    }

    private companion object {
        const val TAG = "EchoToken"
        const val FUNCTION_NAME = "livekit-token"
        const val MAX_ERROR_BODY_CHARS = 200
    }
}
