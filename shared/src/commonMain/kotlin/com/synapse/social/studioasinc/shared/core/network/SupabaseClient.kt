package com.synapse.social.studioasinc.shared.core.network

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
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

    @OptIn(SupabaseInternal::class)
    val client by lazy {
        try {
            if (!isConfigured()) {
                Napier.e("Supabase credentials not configured!", tag = TAG)
                throw ConfigurationException("Supabase not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY.")
            }

            createSupabaseClient(
                supabaseUrl = SynapseConfig.SUPABASE_URL,
                supabaseKey = SynapseConfig.SUPABASE_ANON_KEY
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
                install(Storage) {
                    if (SynapseConfig.SUPABASE_SYNAPSE_S3_ENDPOINT_URL.isNotBlank()) {
                        customUrl = SynapseConfig.SUPABASE_SYNAPSE_S3_ENDPOINT_URL
                    }
                }

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
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }

        // Remove leading slash if present to avoid double slashes
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path

        return "$validatedSupabaseUrl/storage/v1/object/public/$bucket/$cleanPath"
    }

    fun constructMediaUrl(storagePath: String): String {
        return constructStorageUrl(BUCKET_POST_MEDIA, storagePath)
    }

    fun constructAvatarUrl(storagePath: String): String {
        return constructStorageUrl(BUCKET_USER_AVATARS, storagePath)
    }
}
