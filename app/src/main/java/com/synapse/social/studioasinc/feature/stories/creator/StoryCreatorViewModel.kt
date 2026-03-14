package com.synapse.social.studioasinc.feature.stories.creator

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.auth.AuthHelper
import com.synapse.social.studioasinc.data.repository.StoryRepository
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import com.synapse.social.studioasinc.domain.usecase.post.GetPostByIdUseCase
import com.synapse.social.studioasinc.domain.usecase.post.GetDraftUseCase
import com.synapse.social.studioasinc.domain.usecase.post.SaveDraftUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ClearDraftUseCase
import com.synapse.social.studioasinc.domain.usecase.user.GetUserByIdUseCase
import com.synapse.social.studioasinc.shared.domain.model.DraftType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject

enum class FlashMode { OFF, ON, AUTO }

@Serializable
data class TextOverlayDraft(
    val text: String,
    val positionX: Float,
    val positionY: Float,
    val colorArgb: Int,
    val scale: Float
)

@Serializable
data class DrawingPathDraft(
    val points: List<Pair<Float, Float>>,
    val colorArgb: Int,
    val strokeWidth: Float
)

@Serializable
data class StickerOverlayDraft(
    val emoji: String,
    val positionX: Float,
    val positionY: Float,
    val scale: Float
)

@Serializable
data class StoryCreatorDraft(
    val capturedMediaUri: String? = null,
    val mediaType: StoryMediaType = StoryMediaType.PHOTO,
    val textOverlays: List<TextOverlayDraft> = emptyList(),
    val drawings: List<DrawingPathDraft> = emptyList(),
    val stickers: List<StickerOverlayDraft> = emptyList(),
    val selectedPrivacy: StoryPrivacy = StoryPrivacy.ALL_FRIENDS,
    val sharedPostId: String? = null
)

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
    val sharedPost: Post? = null,
    val checkDraft: Boolean = true
)

@HiltViewModel
class StoryCreatorViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val getPostByIdUseCase: GetPostByIdUseCase,
    private val getUserByIdUseCase: com.synapse.social.studioasinc.domain.usecase.user.GetUserByIdUseCase,
    private val getDraftUseCase: GetDraftUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val clearDraftUseCase: ClearDraftUseCase
) : ViewModel() {

    companion object {
        private const val MAX_VIDEO_DURATION_MS = 15_000L
    }

    private val _state = MutableStateFlow(StoryCreatorState())
    val state: StateFlow<StoryCreatorState> = _state.asStateFlow()

    private val _userProfile = MutableStateFlow<com.synapse.social.studioasinc.domain.model.User?>(null)
    val userProfile: StateFlow<com.synapse.social.studioasinc.domain.model.User?> = _userProfile.asStateFlow()

    private var recordingJob: Job? = null

    private val currentUserId: String?
        get() = AuthHelper.getCurrentUserId()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            currentUserId?.let { uid ->
                getUserByIdUseCase(uid)
                    .onSuccess { user ->
                        _userProfile.value = user
                    }
                    .onFailure { error ->
                        android.util.Log.e("StoryCreatorViewModel", "Failed to load current user: ${error.message}", error)
                    }
            }
        }
    }

    fun loadDraft() {
        if (!_state.value.checkDraft) return

        viewModelScope.launch {
            getDraftUseCase(DraftType.STORY)?.let { draft ->
                try {
                    val draftContent = Json.decodeFromString<StoryCreatorDraft>(draft.content)

                    // Load shared post if any
                    val sharedPost = draftContent.sharedPostId?.let {
                        getPostByIdUseCase(it).getOrNull()
                    }

                    _state.update { state ->
                        state.copy(
                            capturedMediaUri = draftContent.capturedMediaUri?.let { Uri.parse(it) },
                            mediaType = draftContent.mediaType,
                            textOverlays = draftContent.textOverlays.map {
                                TextOverlay(it.text, Offset(it.positionX, it.positionY), Color(it.colorArgb), it.scale)
                            },
                            drawings = draftContent.drawings.map {
                                DrawingPath(it.points.map { p -> Offset(p.first, p.second) }, Color(it.colorArgb), it.strokeWidth)
                            },
                            stickers = draftContent.stickers.map {
                                StickerOverlay(it.emoji, Offset(it.positionX, it.positionY), it.scale)
                            },
                            selectedPrivacy = draftContent.selectedPrivacy,
                            sharedPost = sharedPost,
                            checkDraft = false
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("StoryCreatorViewModel", "Failed to decode draft", e)
                    _state.update { it.copy(checkDraft = false) }
                }
            } ?: run {
                _state.update { it.copy(checkDraft = false) }
            }
        }
    }

    fun saveDraft() {
        if (_state.value.isPosted) return

        val state = _state.value
        if (state.capturedMediaUri != null || state.textOverlays.isNotEmpty() || state.drawings.isNotEmpty() || state.stickers.isNotEmpty()) {
            val draftContent = StoryCreatorDraft(
                capturedMediaUri = state.capturedMediaUri?.toString(),
                mediaType = state.mediaType,
                textOverlays = state.textOverlays.map {
                    TextOverlayDraft(it.text, it.position.x, it.position.y, it.color.toArgb(), it.scale)
                },
                drawings = state.drawings.map {
                    DrawingPathDraft(it.points.map { p -> p.x to p.y }, it.color.toArgb(), it.strokeWidth)
                },
                stickers = state.stickers.map {
                    StickerOverlayDraft(it.emoji, it.position.x, it.position.y, it.scale)
                },
                selectedPrivacy = state.selectedPrivacy,
                sharedPostId = state.sharedPost?.id
            )
            viewModelScope.launch {
                saveDraftUseCase(
                    id = "story_creator_draft",
                    type = DraftType.STORY,
                    content = Json.encodeToString(draftContent)
                )
            }
        }
    }

    fun clearDraft() {
        viewModelScope.launch {
            clearDraftUseCase(DraftType.STORY)
        }
    }

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
                clearDraft()
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
