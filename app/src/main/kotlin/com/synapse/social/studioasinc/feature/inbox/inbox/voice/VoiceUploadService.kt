package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.data.source.remote.ImgBBUploadService
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.usecase.chat.UploadMediaUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class VoiceUploadService @Inject constructor(
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val imgBBUploadService: ImgBBUploadService,
    @ApplicationContext private val context: Context
) {
    suspend fun upload(
        audioFile: File,
        chatId: String,
        config: StorageConfig,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val primaryResult = uploadMediaUseCase(
            chatId = chatId,
            filePath = audioFile.absolutePath,
            fileName = audioFile.name.ifBlank { "voice_${System.currentTimeMillis()}.m4a" },
            contentType = "audio/m4a",
            onProgress = onProgress
        )

        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        if (config.isProviderConfigured(StorageProvider.IMGBB)) {
            try {
                val audioBytes = audioFile.readBytes()
                val carrierBytes = context.resources.openRawResource(R.raw.carrier_97b).use { it.readBytes() }
                val encodedBytes = VoiceEncoder.encode(audioBytes, carrierBytes)

                val uploadResult = imgBBUploadService.upload(
                    fileProvider = { offset -> ByteReadChannel(encodedBytes, offset.toInt(), encodedBytes.size - offset.toInt()) },
                    fileSize = encodedBytes.size.toLong(),
                    fileName = "voice_${System.currentTimeMillis()}.png",
                    config = config,
                    bucketName = null,
                    onProgress = {}
                )
                return@withContext Result.success(uploadResult.url)
            } catch (_: Exception) {
                // Fallback to primary result below
            }
        }

        primaryResult
    }
}
