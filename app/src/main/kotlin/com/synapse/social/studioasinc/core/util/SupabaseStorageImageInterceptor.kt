package com.synapse.social.studioasinc.core.util

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.URI

/**
 * Authenticates Coil requests to private Supabase Storage objects.
 *
 * Supabase's public-object endpoint remains the first choice so public buckets keep their
 * existing behavior. If that endpoint rejects an object (for example, because its bucket is
 * private), retry the same object through the authenticated endpoint using the current session.
 */
internal class SupabaseStorageImageInterceptor(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val accessTokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return when (request.url.supabaseStorageObjectEndpoint(supabaseUrl)) {
            "authenticated" -> {
                val authenticatedRequest = request.withSupabaseStorageAuthHeaders(
                    supabaseUrl = supabaseUrl,
                    accessToken = currentAccessToken(),
                    anonKey = anonKey
                ) ?: request
                chain.proceed(authenticatedRequest)
            }
            "public" -> retryPrivateObjectWithAuthentication(chain, request)
            else -> chain.proceed(request)
        }
    }

    private fun retryPrivateObjectWithAuthentication(
        chain: Interceptor.Chain,
        request: Request
    ): Response {
        val publicResponse = chain.proceed(request)
        if (publicResponse.code !in AUTH_FALLBACK_STATUS_CODES) return publicResponse

        val authenticatedUrl = request.url.authenticatedSupabaseStorageUrl(supabaseUrl)
            ?: return publicResponse
        val authenticatedRequest = request.newBuilder()
            .url(authenticatedUrl)
            .build()
            .withSupabaseStorageAuthHeaders(
                supabaseUrl = supabaseUrl,
                accessToken = currentAccessToken(),
                anonKey = anonKey
            ) ?: return publicResponse

        publicResponse.close()
        return chain.proceed(authenticatedRequest)
    }

    private fun currentAccessToken(): String? = try {
        accessTokenProvider()
    } catch (_: Exception) {
        null
    }

    private companion object {
        val AUTH_FALLBACK_STATUS_CODES = setOf(400, 401, 403, 404)
    }
}

/** Returns the object endpoint only for HTTPS requests to this project's Storage API. */
internal fun HttpUrl.supabaseStorageObjectEndpoint(supabaseUrl: String): String? {
    if (!isHttps) return null

    val configuredHost = try {
        URI(supabaseUrl.trim()).host
    } catch (_: Exception) {
        null
    } ?: return null

    if (configuredHost.isBlank() || !host.equals(configuredHost, ignoreCase = true)) return null

    val segments = encodedPathSegments
    for (index in 0 until (segments.size - 3)) {
        if (
            segments[index] == "storage" &&
            segments[index + 1] == "v1" &&
            segments[index + 2] == "object"
        ) {
            return segments[index + 3]
        }
    }
    return null
}

/** Rewrites only a Supabase public-object URL to its authenticated-object equivalent. */
internal fun HttpUrl.authenticatedSupabaseStorageUrl(supabaseUrl: String): HttpUrl? {
    if (supabaseStorageObjectEndpoint(supabaseUrl) != "public") return null

    val publicObjectPath = "/storage/v1/object/public/"
    if (!encodedPath.contains(publicObjectPath)) return null

    val authenticatedPath = encodedPath.replaceFirst(
        publicObjectPath,
        "/storage/v1/object/authenticated/"
    )
    return newBuilder().encodedPath(authenticatedPath).build()
}

/** Adds the project API key and current user bearer token to authenticated object requests. */
internal fun Request.withSupabaseStorageAuthHeaders(
    supabaseUrl: String,
    accessToken: String?,
    anonKey: String
): Request? {
    if (url.supabaseStorageObjectEndpoint(supabaseUrl) != "authenticated") return null

    val apiKey = anonKey.takeIf { it.isNotBlank() } ?: return null
    val bearerToken = accessToken?.takeIf { it.isNotBlank() } ?: apiKey

    return newBuilder()
        .header("Authorization", "Bearer $bearerToken")
        .header("apikey", apiKey)
        .build()
}
