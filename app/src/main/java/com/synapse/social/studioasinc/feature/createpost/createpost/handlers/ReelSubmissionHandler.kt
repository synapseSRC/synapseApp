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

        // Move all I/O and validation to background
        val filePrepResult = withContext(Dispatchers.IO) {
            val cleanPath = FileUtils.validateAndCleanPath(application, videoPath)
            if (cleanPath == null) {
                return@withContext Result.failure(Exception("Invalid video file or file not found"))
            }

            val isContentUri = videoPath.startsWith("content://")
            val file = java.io.File(cleanPath)
            val fileName = "reel_${System.currentTimeMillis()}.mp4"

            try {
                val channel: io.ktor.utils.io.ByteReadChannel
                val size: Long

                if (isContentUri) {
                    val uri = Uri.parse(videoPath)
                    val resolver = application.contentResolver
                    val inputStream = resolver.openInputStream(uri)
                        ?: throw java.io.IOException("Failed to open input stream")
                    channel = inputStream.toByteReadChannel()
                    size = resolver.openFileDescriptor(uri, "r")?.statSize ?: 0L
                } else {
                    channel = file.inputStream().toByteReadChannel()
                    size = file.length()
                }

                Result.success(Triple(channel, size, fileName))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val (channel, size, fileName) = filePrepResult.getOrElse { return Result.failure(it) }

        val metadataMap = mutableMapOf<String, Any?>()
        feeling?.let { metadataMap["feeling"] = mapOf("emoji" to it.emoji, "text" to it.text, "type" to it.type.name) }
        if (taggedPeople.isNotEmpty()) {
            metadataMap["tagged_people"] = taggedPeople.map { mapOf("uid" to it.uid, "username" to it.username) }
        }
        metadataMap["layout_type"] = DEFAULT_LAYOUT_TYPE
        textBackgroundColor?.let { metadataMap["background_color"] = it }

        return reelRepository.uploadReel(
            dataChannel = channel,
            size = size,
            fileName = fileName,
            caption = postText,
            musicTrack = "Original Audio",
            locationName = location?.name,
            locationAddress = location?.address,
            locationLatitude = location?.latitude,
            locationLongitude = location?.longitude,
            metadata = metadataMap,
            onProgress = onProgress
        )
    }

    companion object {
        private const val DEFAULT_LAYOUT_TYPE = "COLUMNS"
    }
}
