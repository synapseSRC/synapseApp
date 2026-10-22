package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.data.source.remote.ImgBBUploadService
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.usecase.chat.UploadMediaUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class EncryptedVoiceUpload(
    val mediaReference: String
)

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
    ): Result<EncryptedVoiceUpload> = withContext(Dispatchers.IO) {
        val encryptedFile = File(context.cacheDir, "voice_" + UUID.randomUUID() + ".enc")
        try {
            val material = VoiceMediaCrypto.encryptFile(audioFile, encryptedFile)
            val primaryResult = uploadMediaUseCase(
                chatId = chatId,
                filePath = encryptedFile.absolutePath,
                fileName = "voice_" + UUID.randomUUID() + ".m4a.enc",
                contentType = "application/octet-stream",
                onProgress = onProgress
            )

            var attachmentUrl = primaryResult.getOrNull()
            if (attachmentUrl == null && config.isProviderConfigured(StorageProvider.IMGBB)) {
                try {
                    val encryptedBytes = encryptedFile.readBytes()
                    val carrierBytes = context.resources.openRawResource(R.raw.carrier_97b).use { it.readBytes() }
                    val encodedBytes = VoiceEncoder.encode(encryptedBytes, carrierBytes)
                    attachmentUrl = imgBBUploadService.upload(
                        fileProvider = { offset ->
                            ByteReadChannel(encodedBytes, offset.toInt(), encodedBytes.size - offset.toInt())
                        },
                        fileSize = encodedBytes.size.toLong(),
                        fileName = "voice_" + UUID.randomUUID() + ".png",
                        config = config,
                        bucketName = null,
                        onProgress = {}
                    ).url
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Keep the primary provider failure if the optional fallback also fails.
                }
            }

            if (attachmentUrl == null) {
                return@withContext Result.failure(
                    primaryResult.exceptionOrNull() ?: IllegalStateException("Voice upload failed.")
                )
            }

            Result.success(
                EncryptedVoiceUpload(
                    mediaReference = VoiceMediaCrypto.createReference(attachmentUrl, material)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            encryptedFile.delete()
        }
    }
}
