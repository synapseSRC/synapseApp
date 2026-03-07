package com.synapse.social.studioasinc.feature.createpost.createpost.handlers

import android.app.Application
import android.net.Uri
import com.synapse.social.studioasinc.core.util.FileUtils
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.domain.model.LocationData
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.shared.data.repository.ReelRepository
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ReelSubmissionHandler @Inject constructor(
    private val application: Application,
    private val reelRepository: ReelRepository
) {

    /**
     * Data class to hold validated file info without holding open streams.
     * Streams/channels are created at upload time to avoid scope cancellation issues.
     */
    private data class ValidatedVideoFile(
        val cleanPath: String,
        val isContentUri: Boolean,
        val size: Long,
        val fileName: String
    )

    suspend fun submitReel(
        videoItem: MediaItem,
        postText: String,
        location: LocationData?,
        taggedPeople: List<User>,
        feeling: FeelingActivity?,
        textBackgroundColor: Long?,
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        val videoPath = videoItem.url

        // Step 1: Validate file and gather metadata on IO thread
        // Do NOT create InputStream or ByteReadChannel here — they must be
        // created in the same coroutine scope that consumes them.
        val validationResult = withContext(Dispatchers.IO) {
            val cleanPath = FileUtils.validateAndCleanPath(application, videoPath)
            if (cleanPath == null) {
                return@withContext Result.failure(Exception("Invalid video file or file not found"))
            }

            val isContentUri = videoPath.startsWith("content://")
            val fileName = "reel_${System.currentTimeMillis()}.mp4"

            try {
                val size: Long = if (isContentUri) {
                    val uri = Uri.parse(videoPath)
                    application.contentResolver.openFileDescriptor(uri, "r")?.use {
                        it.statSize
                    } ?: 0L
                } else {
                    java.io.File(cleanPath).length()
                }

                Result.success(ValidatedVideoFile(cleanPath, isContentUri, size, fileName))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val videoFile = validationResult.getOrElse { return Result.failure(it) }

        val metadataMap = mutableMapOf<String, Any?>()
        feeling?.let { metadataMap["feeling"] = mapOf("emoji" to it.emoji, "text" to it.text, "type" to it.type.name) }
        if (taggedPeople.isNotEmpty()) {
            metadataMap["tagged_people"] = taggedPeople.map { mapOf("uid" to it.uid, "username" to it.username) }
        }
        metadataMap["layout_type"] = DEFAULT_LAYOUT_TYPE
        textBackgroundColor?.let { metadataMap["background_color"] = it }

        // Step 2: Create the ByteReadChannel and upload in the same scope
        // so the internal coroutine launched by toByteReadChannel() stays alive
        // throughout the upload.
        return withContext(Dispatchers.IO) {
            try {
                val channel = if (videoFile.isContentUri) {
                    val uri = Uri.parse(videoPath)
                    val inputStream = application.contentResolver.openInputStream(uri)
                        ?: throw java.io.IOException("Failed to open input stream for video")
                    inputStream.toByteReadChannel()
                } else {
                    java.io.File(videoFile.cleanPath).inputStream().toByteReadChannel()
                }

                reelRepository.uploadReel(
                    dataChannel = channel,
                    size = videoFile.size,
                    fileName = videoFile.fileName,
                    caption = postText,
                    musicTrack = "Original Audio",
                    locationName = location?.name,
                    locationAddress = location?.address,
                    locationLatitude = location?.latitude,
                    locationLongitude = location?.longitude,
                    metadata = metadataMap,
                    onProgress = onProgress
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
