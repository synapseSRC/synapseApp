package com.synapse.social.studioasinc.shared.core.network

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlin.time.Duration.Companion.minutes
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.Url

import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

class ConfigurationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    const val TABLE_USERS = "users"
    const val BUCKET_POST_MEDIA = "posts"
    const val BUCKET_USER_AVATARS = "avatars"
    const val BUCKET_USER_COVERS = "covers"

    private val protectedMediaBuckets = setOf("posts", "covers", "story-media", "story-thumbnails", "reels")

    @OptIn(SupabaseInternal::class)
    val client by lazy {
        try {
            val configured = isConfigured()
            if (!configured) {
                Napier.w("Supabase credentials not configured. Using placeholder configuration.", tag = TAG)
            }

            val supabaseUrl = if (configured) SynapseConfig.SUPABASE_URL else "https://placeholder.supabase.co"
            val supabaseKey = if (configured) SynapseConfig.SUPABASE_ANON_KEY else "placeholder-anon-key"

            createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey
            ) {
                defaultSerializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
                install(Auth)
                install(Postgrest)
                install(Realtime)
                install(Functions)
                // Use Supabase's native Storage REST endpoint.
                // SUPABASE_SYNAPSE_S3_ENDPOINT_URL is an S3-compatible endpoint
                // configuration and must not replace the Storage REST base URL.
                install(Storage)

                httpConfig {
                    install(HttpTimeout) {
                        requestTimeoutMillis = 300_000
                        connectTimeoutMillis = 60_000
                        socketTimeoutMillis = 300_000
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("Failed to initialize Supabase client: ${e.message}", e, tag = TAG)
            throw e
        }
    }

    fun isConfigured(): Boolean {
        return SynapseConfig.SUPABASE_URL.isNotBlank() &&
               SynapseConfig.SUPABASE_URL != "https://your-project.supabase.co" &&
               SynapseConfig.SUPABASE_ANON_KEY.isNotBlank() &&
               SynapseConfig.SUPABASE_ANON_KEY != "your-anon-key-here"
    }

    fun getUrl(): String = SynapseConfig.SUPABASE_URL

    /**
     * Validated base URL for Supabase. Initialized lazily to avoid parsing overhead on every call.
     * Throws [ConfigurationException] if the configured URL is invalid.
     */
    private val validatedSupabaseUrl by lazy {
        val supabaseUrl = SynapseConfig.SUPABASE_URL
        try {
            Url(supabaseUrl).toString()
        } catch (e: Exception) {
            throw ConfigurationException("Invalid Supabase URL configured: $supabaseUrl", e)
        }
        supabaseUrl
    }

    fun constructStorageUrl(bucket: String, path: String): String {
        if (path.isBlank()) return ""
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("private-media://")) {
            return path
        }

        // Remove leading slash if present to avoid double slashes
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        val cleanBaseUrl = if (validatedSupabaseUrl.endsWith("/")) validatedSupabaseUrl.dropLast(1) else validatedSupabaseUrl

        return "$cleanBaseUrl/storage/v1/object/public/$bucket/$cleanPath"
    }

    fun constructMediaUrl(storagePath: String): String {
        return constructStorageUrl(BUCKET_POST_MEDIA, storagePath)
    }

    fun constructAvatarUrl(storagePath: String): String {
        return constructStorageUrl(BUCKET_USER_AVATARS, storagePath)
    }

    fun constructCoverUrl(storagePath: String): String {
        return constructStorageUrl(BUCKET_USER_COVERS, storagePath)
    }

    /** Signs a protected Storage object only after Storage RLS authorizes the current session. */
    suspend fun createSignedStorageUrl(defaultBucket: String, pathOrUrl: String): String {
        val reference = resolveProtectedStorageObject(defaultBucket, pathOrUrl) ?: return pathOrUrl
        return client.storage.from(reference.first).createSignedUrl(
            path = reference.second,
            expiresIn = 15.minutes
        )
    }

    private fun resolveProtectedStorageObject(defaultBucket: String, pathOrUrl: String): Pair<String, String>? {
        if (pathOrUrl.startsWith("private-media://")) {
            val route = pathOrUrl.removePrefix("private-media://")
                .substringBefore("?")
                .substringBefore("#")
            val segments = route.split('/', limit = 2)
            if (segments.size < 2) return null
            val bucket = segments[0]
            val objectPath = segments[1]
            return if (bucket in protectedMediaBuckets && objectPath.isNotBlank()) bucket to objectPath else null
        }

        val marker = "/storage/v1/object/"
        val markerIndex = pathOrUrl.indexOf(marker)
        if (markerIndex >= 0) {
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                val sourceHost = runCatching { Url(pathOrUrl).host }.getOrNull() ?: return null
                val configuredHost = runCatching { Url(validatedSupabaseUrl).host }.getOrNull() ?: return null
                if (!sourceHost.equals(configuredHost, ignoreCase = true)) return null
            }
            val route = pathOrUrl.substring(markerIndex + marker.length)
                .substringBefore("?")
                .substringBefore("#")
            val segments = route.split('/', limit = 3)
            if (segments.size < 3) return null
            val bucket = segments[1]
            val objectPath = segments[2]
            return if (bucket in protectedMediaBuckets && objectPath.isNotBlank()) bucket to objectPath else null
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return null
        val objectPath = pathOrUrl.substringBefore("?").substringBefore("#").trimStart('/')
        return if (defaultBucket in protectedMediaBuckets && objectPath.isNotBlank()) defaultBucket to objectPath else null
    }
}
