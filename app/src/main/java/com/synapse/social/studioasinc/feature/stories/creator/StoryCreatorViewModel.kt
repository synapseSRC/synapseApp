package com.synapse.social.studioasinc.feature.stories.creator

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.auth.AuthHelper
import com.synapse.social.studioasinc.data.repository.StoryRepository
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import com.synapse.social.studioasinc.domain.usecase.post.GetPostByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FlashMode { OFF, ON, AUTO }

data class TextOverlay(
    val text: String,
    val position: Offset,
    val color: Color = Color.White,
    val scale: Float = 1f
)

data class DrawingPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

data class StickerOverlay(
    val emoji: String,
    val position: Offset,
    val scale: Float = 1f
)

data class StoryCreatorState(
    val capturedMediaUri: Uri? = null,
    val mediaType: StoryMediaType = StoryMediaType.PHOTO,
    val flashMode: FlashMode = FlashMode.OFF,
    val isFrontCamera: Boolean = false,
    val isRecording: Boolean = false,
    val recordingProgress: Float = 0f,
    val textOverlays: List<TextOverlay> = emptyList(),
    val drawings: List<DrawingPath> = emptyList(),
    val stickers: List<StickerOverlay> = emptyList(),
    val selectedPrivacy: StoryPrivacy = StoryPrivacy.ALL_FRIENDS,
    val isPosting: Boolean = false,
    val isPosted: Boolean = false,
    val error: String? = null,
    val sharedPost: Post? = null
)

@HiltViewModel
class StoryCreatorViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val getPostByIdUseCase: GetPostByIdUseCase
) : ViewModel() {

    companion object {
        private const val MAX_VIDEO_DURATION_MS = 15_000L
    }

    private val _state = MutableStateFlow(StoryCreatorState())
    val state: StateFlow<StoryCreatorState> = _state.asStateFlow()

    private var recordingJob: Job? = null

    private val currentUserId: String?
        get() = AuthHelper.getCurrentUserId()

    fun loadSharedPost(postId: String) {
        viewModelScope.launch {
            getPostByIdUseCase(postId).onSuccess { post ->
                _state.update { it.copy(sharedPost = post) }
            }.onFailure { e ->
                _state.update { it.copy(error = "Failed to load post: ${e.message}") }
            }
        }
    }

    fun toggleFlash() {
        _state.update { state ->
            val newMode = when (state.flashMode) {
                FlashMode.OFF -> FlashMode.ON
                FlashMode.ON -> FlashMode.AUTO
                FlashMode.AUTO -> FlashMode.OFF
            }
            state.copy(flashMode = newMode)
        }
    }

    fun flipCamera() {
        _state.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun capturePhoto() {
        _state.update {
            it.copy(
                capturedMediaUri = null,
                mediaType = StoryMediaType.PHOTO
            )
        }
    }

    fun startVideoRecording() {
        _state.update { it.copy(isRecording = true, recordingProgress = 0f) }

        recordingJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            while (_state.value.isRecording) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / MAX_VIDEO_DURATION_MS).coerceIn(0f, 1f)

                _state.update { it.copy(recordingProgress = progress) }

                if (elapsed >= MAX_VIDEO_DURATION_MS) {
                    stopVideoRecording()
                    break
                }

                delay(50)
            }
        }
    }

    fun stopVideoRecording() {
        recordingJob?.cancel()
        recordingJob = null
        _state.update {
            it.copy(
                isRecording = false,
                mediaType = StoryMediaType.VIDEO
            )
        }
    }

    fun setMediaFromGallery(uri: Uri) {
        val isVideo = uri.toString().contains("video")
        _state.update {
            it.copy(
                capturedMediaUri = uri,
                mediaType = if (isVideo) StoryMediaType.VIDEO else StoryMediaType.PHOTO
            )
        }
    }

    fun setCapturedMedia(uri: Uri, type: StoryMediaType) {
        _state.update {
            it.copy(
                capturedMediaUri = uri,
                mediaType = type
            )
        }
    }

    fun clearCapturedMedia() {
        _state.update {
            it.copy(
                capturedMediaUri = null,
                textOverlays = emptyList(),
                drawings = emptyList(),
                stickers = emptyList()
            )
        }
    }

    fun addTextOverlay() {
        _state.update { state ->
            state.copy(
                textOverlays = state.textOverlays + TextOverlay(
                    text = "",
                    position = Offset(100f, 100f)
                )
            )
        }
    }

    fun removeTextOverlay(index: Int) {
        _state.update { state ->
            state.copy(
                textOverlays = state.textOverlays.filterIndexed { i, _ -> i != index }
            )
        }
    }

    fun updateTextPosition(index: Int, position: Offset) {
        _state.update { state ->
            state.copy(
                textOverlays = state.textOverlays.mapIndexed { i, overlay ->
                    if (i == index) overlay.copy(position = position) else overlay
                }
            )
        }
    }

    fun updateTextContent(index: Int, content: String) {
        _state.update { state ->
            state.copy(
                textOverlays = state.textOverlays.mapIndexed { i, overlay ->
                    if (i == index) overlay.copy(text = content) else overlay
                }
            )
        }
    }


    fun addDrawing(path: DrawingPath) {
        _state.update { state ->
            state.copy(drawings = state.drawings + path)
        }
    }

    fun clearDrawings() {
        _state.update { state ->
            state.copy(drawings = emptyList())
        }
    }


    fun addSticker(emoji: String) {
        _state.update { state ->
            state.copy(
                stickers = state.stickers + StickerOverlay(
                    emoji = emoji,
                    position = Offset(200f, 400f)
                )
            )
        }
    }

    fun removeSticker(index: Int) {
        _state.update { state ->
            state.copy(
                stickers = state.stickers.filterIndexed { i, _ -> i != index }
            )
        }
    }

    fun updateStickerPosition(index: Int, position: Offset) {
        _state.update { state ->
            state.copy(
                stickers = state.stickers.mapIndexed { i, sticker ->
                    if (i == index) sticker.copy(position = position) else sticker
                }
            )
        }
    }

    fun setPrivacy(privacy: StoryPrivacy) {
        _state.update { it.copy(selectedPrivacy = privacy) }
    }

    fun postStory() {
        val userId = currentUserId ?: return
        val mediaUri = _state.value.capturedMediaUri ?: return

        viewModelScope.launch {
            _state.update { it.copy(isPosting = true, error = null) }

            val duration = if (_state.value.mediaType == StoryMediaType.VIDEO) {
                (_state.value.recordingProgress * 15).toInt().coerceAtLeast(1)
            } else {
                5
            }

            storyRepository.createStory(
                userId = userId,
                mediaUri = mediaUri,
                mediaType = _state.value.mediaType,
                privacy = _state.value.selectedPrivacy,
                duration = duration
            ).onSuccess {
                _state.update { it.copy(isPosting = false, isPosted = true) }
            }.onFailure { error ->
                _state.update {
                    it.copy(isPosting = false, error = error.message ?: "Failed to post story")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
    }
}
