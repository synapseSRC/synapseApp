package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.data.source.remote.ImgBBUploadService
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceUploadService @Inject constructor(
    private val imgBBUploadService: ImgBBUploadService,
    private val storageRepository: StorageRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun upload(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val config = storageRepository.getStorageConfig().first()
            val audioBytes = audioFile.readBytes()

            val carrierBytes = context.resources.openRawResource(R.raw.carrier_97b).use { it.readBytes() }

            // Encode: Carrier + Audio
            val encodedBytes = VoiceEncoder.encode(audioBytes, carrierBytes)

            // Upload using ImgBBUploadService directly
            val url = imgBBUploadService.upload(
                fileProvider = { offset -> ByteReadChannel(encodedBytes, offset.toInt(), encodedBytes.size) },
                fileSize = encodedBytes.size.toLong(),
                fileName = "voice_${System.currentTimeMillis()}.png", // Treat it as PNG so ImgBB accepts it
                config = config,
                bucketName = null,
                onProgress = {}
            )

            Result.success(url)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
