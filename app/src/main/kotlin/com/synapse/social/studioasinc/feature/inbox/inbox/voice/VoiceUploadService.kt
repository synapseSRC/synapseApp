package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.data.source.remote.ImgBBUploadService
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class VoiceUploadService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imgBBUploadService: ImgBBUploadService
) {

    suspend fun upload(audioFile: File, config: StorageConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            val audioBytes = audioFile.readBytes()
            val carrierStream = context.resources.openRawResource(R.raw.carrier_97b)
            val carrierBytes = carrierStream.readBytes()
            carrierStream.close()

            val encodedBytes = VoiceEncoder.encode(audioBytes, carrierBytes)

            val fileName = "voice_${System.currentTimeMillis()}.png"

            val url = imgBBUploadService.upload(
                fileProvider = { _ -> ByteReadChannel(encodedBytes) },
                fileSize = encodedBytes.size.toLong(),
                fileName = fileName,
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
