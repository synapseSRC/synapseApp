package com.synapse.social.studioasinc.data.call

import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.data.call.dto.TurnCredentialErrorDto
import com.synapse.social.studioasinc.data.call.dto.TurnCredentialsDto
import com.synapse.social.studioasinc.data.call.mapper.toDomain
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendException
import com.synapse.social.studioasinc.shared.domain.model.call.CallErrorKind
import com.synapse.social.studioasinc.shared.domain.model.call.TurnCredentials
import com.synapse.social.studioasinc.shared.domain.repository.TurnCredentialRepository
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Fetches ICE servers from the `turn-credentials` Edge Function.
 *
 * The app holds no TURN secret and never will: the function derives short-lived
 * `use-auth-secret` credentials from a secret that stays in Supabase, so a decompiled APK is
 * worth nothing to someone trying to relay other people's traffic. What is cached here is a
 * credential that expires on its own, which is why keeping it in memory — and only until
 * half its lifetime — is the right trade instead of fetching one per ICE server list.
 *
 * Failures are reported, never papered over. A function that is not deployed, or a project
 * with no coturn behind it, produces `turnAvailable = false`; this class does not fall back
 * to a STUN-only list and call it a success, because the honest outcome for two peers behind
 * symmetric NATs is a visible failure before dialing, not a call that dies during
 * negotiation.
 */
class TurnCredentialRepositoryImpl(
    private val client: SupabaseClientLib
) : TurnCredentialRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mintMutex = Mutex()

    @Volatile
    private var cache: Cached? = null

    private class Cached(
        val credentials: TurnCredentials,
        val validUntilMillis: Long,
        val ttlMillis: Long
    ) {
        fun remaining(nowMillis: Long): Long = validUntilMillis - nowMillis
    }

    override suspend fun credentials(minValidityMillis: Long): Result<TurnCredentials> {
        reuse(minValidityMillis)?.let { return Result.success(it) }
        return mint(minValidityMillis)
    }

    override fun invalidate() {
        cache = null
    }

    /**
     * Re-uses a cached list only while it still has both the requested headroom *and* half
     * its own lifetime left, so a long call rotates credentials instead of running out
     * mid-negotiation.
     */
    private fun reuse(minValidityMillis: Long): TurnCredentials? {
        val cached = cache ?: return null
        val required = maxOf(minValidityMillis, cached.ttlMillis / REFRESH_DIVISOR)
        if (cached.remaining(System.currentTimeMillis()) < required) {
            cache = null
            return null
        }
        return cached.credentials
    }

    private suspend fun mint(minValidityMillis: Long): Result<TurnCredentials> =
        withContext(AppDispatchers.IO) {
            mintMutex.withLock {
                reuse(minValidityMillis)?.let { return@withLock Result.success(it) }
                try {
                    val response = client.functions.invoke(BuildConfig.TURN_FUNCTION_NAME)
                    val text = response.bodyAsText()
                    val status = response.status.value
                    if (status !in 200..299) {
                        return@withLock Result.failure(rejection(status, text))
                    }
                    val dto = json.decodeFromString(TurnCredentialsDto.serializer(), text)
                    val credentials = dto.toDomain()
                    remember(dto, credentials)
                    // Never log the list itself: [IceServerEndpoint] hides its credential, but
                    // the useful facts here are counts and availability.
                    Napier.i(
                        "ICE configuration: ${credentials.servers.size} server(s), turn=${credentials.turnAvailable}",
                        tag = TAG
                    )
                    Result.success(credentials)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.w("turn-credentials request failed: ${e.message}", tag = TAG)
                    Result.failure(
                        if (e is CallBackendException) e else CallBackendException(
                            if (e is IOException) CallErrorKind.BACKEND_UNREACHABLE else CallErrorKind.UNKNOWN,
                            "Could not obtain traversal credentials: ${e.message ?: e.javaClass.simpleName}"
                        )
                    )
                }
            }
        }

    private fun remember(dto: TurnCredentialsDto, credentials: TurnCredentials) {
        val now = System.currentTimeMillis()
        val ttlMillis = dto.ttlSeconds?.takeIf { it > 0 }?.times(1000L)
            ?: dto.expiresAtMillis?.minus(now)?.takeIf { it > 0 }
        // With no stated lifetime there is nothing honest to cache against, so each call mints
        // again rather than keeping a credential of unknown age.
        if (ttlMillis == null) {
            cache = null
            return
        }
        cache = Cached(
            credentials = credentials,
            validUntilMillis = dto.expiresAtMillis ?: (now + ttlMillis),
            ttlMillis = ttlMillis
        )
    }

    /**
     * Turns a non-2xx response into the failure that best explains it to a caller.
     *
     * A 404 means the function is not deployed under the configured name — which, from the
     * app's side, is the same problem as a project with no TURN server: relay is not
     * available, and the call should say so rather than "network error".
     */
    private fun rejection(status: Int, text: String): CallBackendException {
        val body = try {
            json.decodeFromString(TurnCredentialErrorDto.serializer(), text)
        } catch (e: Exception) {
            // Not a JSON error object (a proxy page, an empty body). The status plus a
            // truncated body is still the most useful thing we can report.
            null
        }
        val code = body?.code
        val detail = body?.error ?: text.take(MAX_ERROR_BODY_CHARS)
        return when {
            code == "TURN_NOT_CONFIGURED" || status == 404 -> CallBackendException(
                CallErrorKind.TURN_UNAVAILABLE,
                "Traversal credentials are not available (HTTP $status${code?.let { ", $it" } ?: ""}): $detail"
            )

            status == 401 || status == 403 -> CallBackendException(
                CallErrorKind.NOT_AUTHENTICATED,
                "The call service refused to mint traversal credentials (HTTP $status): $detail"
            )

            else -> CallBackendException(
                CallErrorKind.BACKEND_UNREACHABLE,
                "The credential service returned HTTP $status: $detail"
            )
        }
    }

    private companion object {
        const val TAG = "Call"

        /** Refresh at half the credential's lifetime, matching the model's documented policy. */
        const val REFRESH_DIVISOR = 2L

        const val MAX_ERROR_BODY_CHARS = 200
    }
}
