package com.synapse.social.studioasinc.feature.createpost.createpost.handlers

import android.app.Application
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.core.util.FileUtils
import com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import java.util.UUID

class MediaUploadHandler @Inject constructor(
    private val application: Application,
    private val uploadMediaUseCase: UploadMediaUseCase
) : com.synapse.social.studioasinc.domain.repository.MediaUploadHandler {

    override suspend fun uploadMedia(
        newMedia: List<MediaItem>,
        onProgress: (Float) -> Unit
    ): List<MediaItem> {
        val totalItems = newMedia.size
        val progressMap = ConcurrentHashMap<Int, Float>()

        // Initialize progress map
        for (i in 0 until totalItems) {
            progressMap[i] = 0f
        }

        val uploadedResults = coroutineScope {
            newMedia.mapIndexed { index, mediaItem ->
                async(Dispatchers.IO) {
                    try {
                        val filePath = mediaItem.url
                        val cleanPath = FileUtils.validateAndCleanPath(application, filePath)
                        if (cleanPath == null) {
                            // Set progress to 100% for skipped item
                            progressMap[index] = 1.0f
                            updateTotalProgress(progressMap, totalItems, onProgress)
                            return@async null
                        }

                        val sharedMediaType = when (mediaItem.type) {
                            MediaType.IMAGE -> com.synapse.social.studioasinc.shared.domain.model.MediaType.PHOTO
                            MediaType.VIDEO -> com.synapse.social.studioasinc.shared.domain.model.MediaType.VIDEO
                            else -> com.synapse.social.studioasinc.shared.domain.model.MediaType.OTHER
                        }

                        val result = uploadMediaUseCase(
                            filePath = filePath,
                            mediaType = sharedMediaType,
                            onProgress = { progress ->
                                progressMap[index] = progress
                                updateTotalProgress(progressMap, totalItems, onProgress)
                            }
                        )

                        // If upload succeeds, return the new MediaItem
                        val uploadedUrl = result.getOrThrow()

                        // Set progress to 100%
                        progressMap[index] = 1.0f
                        updateTotalProgress(progressMap, totalItems, onProgress)

                        android.util.Log.d("MediaUploadHandler", "Uploaded ${mediaItem.type}: $uploadedUrl")

                        mediaItem.copy(
                            id = UUID.randomUUID().toString(),
                            url = uploadedUrl,
                            mimeType = application.contentResolver.getType(android.net.Uri.parse(filePath))
                        )
                    } catch (e: Exception) {
                        val errorDetails = "Media upload failed for ${mediaItem.type} at index $index: ${e.message ?: e.toString()}"
                        android.util.Log.e("MediaUploadHandler", errorDetails, e)
                        throw Exception(errorDetails, e)
                    }
                }
            }.awaitAll()
        }

        return uploadedResults.filterNotNull()
    }

    private fun updateTotalProgress(
        progressMap: ConcurrentHashMap<Int, Float>,
        totalItems: Int,
        onProgress: (Float) -> Unit
    ) {
        val totalProgress = progressMap.values.sum() / totalItems
        onProgress(totalProgress)
    }
}
