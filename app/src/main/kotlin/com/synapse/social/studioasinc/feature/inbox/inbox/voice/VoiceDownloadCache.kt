package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceDownloadCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient
) {
    suspend fun getLocalPath(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val urlHash = url.hashCode()
            val fileName = "voice_$urlHash.m4a"
            val file = File(context.cacheDir, fileName)

            if (file.exists() && file.length() > 0) {
                return@withContext Result.success(file.absolutePath)
            }

            val response = httpClient.get(url)
            val encodedBytes = response.readBytes()

            val audioBytes = VoiceEncoder.decode(encodedBytes)
            file.writeBytes(audioBytes)

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
