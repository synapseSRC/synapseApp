package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceDownloadCache @Inject constructor(
    private val httpClient: HttpClient,
    @ApplicationContext private val context: Context
) {

    private fun isSupabaseUrl(url: String): Boolean {
        val configuredUrl = try { SynapseConfig.SUPABASE_URL } catch (e: Exception) { "" }
        return url.contains("supabase.co") ||
               url.contains("/storage/v1/") ||
               (configuredUrl.isNotBlank() && url.contains(configuredUrl))
    }

    suspend fun getLocalPath(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val urlHash = url.hashCode().toString()
            val cachedFile = File(context.cacheDir, "voice_$urlHash.m4a")

            // If already downloaded and decoded, return it
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return@withContext Result.success(cachedFile.absolutePath)
            }

            val isSupabase = isSupabaseUrl(url)
            val downloadedBytes: ByteArray = if (isSupabase) {
                val sessionToken = try {
                    SupabaseClient.client.auth.currentSessionOrNull()?.accessToken
                } catch (e: Exception) {
                    null
                }
                val anonKey = try { SynapseConfig.SUPABASE_ANON_KEY } catch (e: Exception) { "" }

                val response = httpClient.get(url) {
                    if (!sessionToken.isNullOrBlank()) {
                        header("Authorization", "Bearer $sessionToken")
                    }
                    if (anonKey.isNotBlank()) {
                        header("apikey", anonKey)
                    }
                }
                response.body()
            } else {
                val response = httpClient.get(url)
                response.body()
            }

            // Decode to extract audio if carrier PNG (ImgBB legacy format)
            val audioBytes = VoiceEncoder.decode(downloadedBytes)

            if (audioBytes.isEmpty()) {
                // If decode fails, fallback to writing raw bytes (raw audio URL)
                cachedFile.writeBytes(downloadedBytes)
            } else {
                cachedFile.writeBytes(audioBytes)
            }

            Result.success(cachedFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
