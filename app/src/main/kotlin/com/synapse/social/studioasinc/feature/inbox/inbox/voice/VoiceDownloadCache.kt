package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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

    suspend fun getLocalPath(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val urlHash = url.hashCode().toString()
            val cachedFile = File(context.cacheDir, "voice_$urlHash.m4a")

            // If already downloaded and decoded, return it
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return@withContext Result.success(cachedFile.absolutePath)
            }

            // Download PNG carrier from ImgBB
            val response = httpClient.get(url)
            val downloadedBytes: ByteArray = response.body()

            // Decode to extract audio
            val audioBytes = VoiceEncoder.decode(downloadedBytes)

            if (audioBytes.isEmpty()) {
                // If decode fails, fallback to writing raw bytes (maybe it's a raw audio URL?)
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
