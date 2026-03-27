package com.synapse.social.studioasinc.feature.createpost.createpost.handlers

import android.app.Application
import com.synapse.social.studioasinc.domain.model.CreatePostRequest
import com.synapse.social.studioasinc.domain.model.LocationData
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.shared.data.repository.ReelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ReelSubmissionHandler @Inject constructor(
    private val application: Application,
    private val reelRepository: ReelRepository,
    private val uploadMediaUseCase: com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
) : com.synapse.social.studioasinc.domain.repository.ReelSubmissionHandler {

    override suspend fun submitReel(request: CreatePostRequest, onProgress: (Float) -> Unit): Result<Unit> {
        val videoItem = request.mediaItems.firstOrNull { it.type == com.synapse.social.studioasinc.domain.model.MediaType.VIDEO }
            ?: return Result.failure(Exception("No video found for reel"))
        val metadataMap = mutableMapOf<String, Any?>()
        metadataMap["layout_type"] = DEFAULT_LAYOUT_TYPE
        request.textBackgroundColor?.let { metadataMap["background_color"] = it }

        return withContext(Dispatchers.IO) {
            try {
                val videoUrl = uploadMediaUseCase(
                    filePath = videoItem.url,
                    mediaType = com.synapse.social.studioasinc.shared.domain.model.MediaType.VIDEO,
                    bucketName = "reels",
                    onProgress = onProgress
                ).getOrThrow()

                reelRepository.createReel(
                    videoUrl = videoUrl,
                    caption = request.postText,
                    musicTrack = "Original Audio",
                    locationName = request.location?.name,
                    locationAddress = request.location?.address,
                    locationLatitude = request.location?.latitude,
                    locationLongitude = request.location?.longitude,
                    metadata = metadataMap
                )
            } catch (e: Exception) {
                android.util.Log.e("ReelSubmissionHandler", "Reel submission failed: ${e.message}", e)
                Result.failure(Exception("Reel submission failed: ${e.message ?: e::class.simpleName}", e))
            }
        }
    }

    companion object {
        private const val DEFAULT_LAYOUT_TYPE = "COLUMNS"
    }
}

