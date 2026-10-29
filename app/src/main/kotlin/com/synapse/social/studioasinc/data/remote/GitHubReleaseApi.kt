package com.synapse.social.studioasinc.data.remote

import com.synapse.social.studioasinc.domain.model.release.ReleaseRateLimitedException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Transport for the public GitHub Releases list.
 *
 * Requests are anonymous — the endpoint is public read-only, so no token, key or backend is
 * involved. `per_page` asks for exactly the releases the What's New screen shows, which fits one
 * page and needs no link-header walking.
 */
class GitHubReleaseApi @Inject constructor(
    private val httpClient: HttpClient
) {

    /**
     * Returns the raw list payload.
     *
     * @throws ReleaseRateLimitedException when GitHub's anonymous quota is spent.
     * @throws IOException on transport failures, timeouts and any other non-success status.
     */
    suspend fun fetchReleasePayload(): String = try {
        withTimeout(REQUEST_TIMEOUT_MILLIS) {
            val response = httpClient.get(ENDPOINT) {
                header("Accept", "application/vnd.github+json")
                header("User-Agent", USER_AGENT)
            }
            response.bodyAsText()
        }
    } catch (timeout: TimeoutCancellationException) {
        throw IOException("GitHub releases request timed out", timeout)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: ResponseException) {
        if (isRateLimited(failure.response)) {
            throw ReleaseRateLimitedException()
        }
        throw IOException("GitHub releases request failed with HTTP ${failure.response.status.value}", failure)
    }

    private fun isRateLimited(response: HttpResponse): Boolean {
        if (response.status == HttpStatusCode.TooManyRequests) return true
        if (response.status != HttpStatusCode.Forbidden) return false
        val remaining = response.headers["X-RateLimit-Remaining"]
            ?: response.headers["RateLimit"]?.substringAfter("remaining=", "")?.takeWhile { it.isDigit() }
        return remaining?.trim() == "0"
    }

    private companion object {
        const val ENDPOINT = "https://api.github.com/repos/synapseSRC/SynapseApp/releases?per_page=10"
        const val USER_AGENT = "Synapse-Android"
        const val REQUEST_TIMEOUT_MILLIS = 10_000L
    }
}
