package com.synapse.social.studioasinc.feature.shared.reels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.synapse.social.studioasinc.shared.data.repository.ReelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReelUploadManager @Inject constructor(
    private val reelRepository: ReelRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress = _uploadProgress.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    fun uploadReel(videoUri: Uri, caption: String, musicTrack: String) {
        val fileName = "reel_${System.currentTimeMillis()}.mp4"
        scope.launch {
            _uploadError.value = null
            _uploadProgress.value = 0f

            try {
                val size = context.contentResolver.query(videoUri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    cursor.getLong(sizeIndex)
                } ?: -1L

                val inputStream = context.contentResolver.openInputStream(videoUri)
                    ?: throw Exception("Failed to open input stream for URI: $videoUri")

                val channel = inputStream.toByteReadChannel()

                reelRepository.uploadReel(
                    dataChannel = channel,
                    size = size,
                    fileName = fileName,
                    caption = caption,
                    musicTrack = musicTrack,
                    onProgress = { progress ->
                        _uploadProgress.value = progress
                    }
                ).onSuccess {
                    _uploadProgress.value = null
                }.onFailure { e ->
                    _uploadProgress.value = null
                    _uploadError.value = e.message
                }
            } catch (e: Exception) {
                _uploadProgress.value = null
                _uploadError.value = e.message
            }
        }
    }

    fun clearError() {
        _uploadError.value = null
    }
}
