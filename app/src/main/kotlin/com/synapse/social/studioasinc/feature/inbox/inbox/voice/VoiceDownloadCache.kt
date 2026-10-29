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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class VoiceDownloadCache @Inject constructor(
    private val httpClient: HttpClient,
    @ApplicationContext private val context: Context
) {

    private fun isSupabaseUrl(url: String): Boolean {
        val configuredUrl = try { SynapseConfig.SUPABASE_URL } catch (e: Exception) { "" }
        return url.contains("supabase.co") ||
               url.contains("/storage/v1/") ||
               (configuredUrl.isNotBlank() && url.contains(configuredUrl))
    }

    private fun getCacheFilename(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(url.toByteArray(Charsets.UTF_8))
        val hex = hashBytes.joinToString("") { "%02x".format(it) }
        return "voice_$hex.m4a"
    }

    private fun extractCarrierPayload(bytes: ByteArray): ByteArray {
        val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val isPngCarrier = bytes.size >= pngSignature.size &&
            bytes.copyOfRange(0, pngSignature.size).contentEquals(pngSignature)
        if (!isPngCarrier) return bytes
        return VoiceEncoder.decode(bytes).also {
            require(it.isNotEmpty()) { "Voice media carrier does not contain an audio payload." }
        }
    }

    private suspend fun download(url: String): ByteArray {
        val isSupabase = isSupabaseUrl(url)
        return if (isSupabase) {
            val sessionToken = try {
                SupabaseClient.client.auth.currentSessionOrNull()?.accessToken
            } catch (e: Exception) {
                null
            }
            val anonKey = try { SynapseConfig.SUPABASE_ANON_KEY } catch (e: Exception) { "" }
            val response = httpClient.get(url) {
                if (!sessionToken.isNullOrBlank()) header("Authorization", "Bearer $sessionToken")
                if (anonKey.isNotBlank()) header("apikey", anonKey)
            }
            response.body()
        } else {
            httpClient.get(url).body()
        }
    }

    open suspend fun getLocalPath(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if local file path or uri directly
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                val cleanPath = url.removePrefix("file://")
                val localFile = File(cleanPath)
                if (localFile.exists() && localFile.length() > 0) {
                    return@withContext Result.success(localFile.absolutePath)
                }
            }

            val encryptedReference = VoiceMediaCrypto.parseReference(url)
            val filename = getCacheFilename(url)
            val cachedFile = File(context.cacheDir, filename)
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return@withContext Result.success(cachedFile.absolutePath)
            }

            val downloadUrl = encryptedReference?.remoteUrl ?: url
            val downloadedBytes = download(downloadUrl)
            val extractedBytes = extractCarrierPayload(downloadedBytes)
            val playbackBytes = if (encryptedReference != null) {
                VoiceMediaCrypto.decrypt(extractedBytes, encryptedReference)
            } else {
                extractedBytes
            }
            cachedFile.writeBytes(playbackBytes)
            Result.success(cachedFile.absolutePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
