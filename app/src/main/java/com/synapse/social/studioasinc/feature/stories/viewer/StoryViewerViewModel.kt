package com.synapse.social.studioasinc.feature.stories.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.StoryRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.domain.model.Story
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryViewerState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val stories: List<Story> = emptyList(),
    val user: User? = null,
    val currentStoryIndex: Int = 0,
    val progress: Float = 0f,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false
)

@HiltViewModel
class StoryViewerViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryViewerState())
    val uiState: StateFlow<StoryViewerState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private val defaultStoryDuration = 5000L
    private val progressUpdateInterval = 50L

    fun loadStories(userId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            android.util.Log.d("StoryViewerViewModel", "Loading stories for user: $userId")
            
            // Fetch user and stories in parallel
            val userResult = userRepository.getUserById(userId)
            val storiesResult = storyRepository.getUserStories(userId)

            val user = userResult.getOrNull()
            val stories = storiesResult.getOrNull() ?: emptyList()

            android.util.Log.d("StoryViewerViewModel", "User fetched: ${user != null}, Stories count: ${stories.size}")

            when {
                user == null -> {
                    android.util.Log.e("StoryViewerViewModel", "User not found for userId: $userId")
                    _uiState.update { it.copy(isLoading = false, error = "User not found") }
                }
                stories.isEmpty() -> {
                    android.util.Log.w("StoryViewerViewModel", "No stories found for userId: $userId")
                    _uiState.update { it.copy(isLoading = false, error = "No stories found", user = user) }
                }
                else -> {
                    android.util.Log.d("StoryViewerViewModel", "Successfully loaded ${stories.size} stories for user: ${user.displayName ?: user.username}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            stories = stories,
                            user = user,
                            currentStoryIndex = 0,
                            progress = 0f
                        )
                    }
                    val firstStory = stories[0]
                    if (firstStory.mediaType != StoryMediaType.VIDEO) {
                        startProgress()
                    }
                    markAsSeen(firstStory.id)
                }
            }
        }
    }

    private fun startProgress(durationOverride: Long? = null) {
        if (_uiState.value.isPaused || _uiState.value.isFinished) return

        stopProgress()
        progressJob = viewModelScope.launch {
            val stories = _uiState.value.stories
            val currentIndex = _uiState.value.currentStoryIndex
            if (currentIndex !in stories.indices) return@launch

            val currentStory = stories[currentIndex]
            val duration = durationOverride
                ?: currentStory.mediaDurationSeconds?.times(1000L)
                ?: defaultStoryDuration

            val steps = duration / progressUpdateInterval
            val stepSize = 1.0f / steps


            var currentProgress = _uiState.value.progress

            while (currentProgress < 1.0f) {
                delay(progressUpdateInterval)
                if (_uiState.value.isPaused) return@launch

                currentProgress += stepSize
                _uiState.update { it.copy(progress = currentProgress.coerceAtMost(1.0f)) }
            }


            nextStory()
        }
    }

    private fun stopProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    fun nextStory() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentStoryIndex + 1

        if (nextIndex < currentState.stories.size) {
            _uiState.update {
                it.copy(
                    currentStoryIndex = nextIndex,
                    progress = 0f
                )
            }
            val nextStory = currentState.stories[nextIndex]
            if (nextStory.mediaType != StoryMediaType.VIDEO) {
                startProgress()
            }
            markAsSeen(nextStory.id)
        } else {
            _uiState.update { it.copy(isFinished = true) }
            stopProgress()
        }
    }

    fun previousStory() {
        val currentState = _uiState.value
        val prevIndex = currentState.currentStoryIndex - 1

        if (prevIndex >= 0) {
            _uiState.update {
                it.copy(
                    currentStoryIndex = prevIndex,
                    progress = 0f
                )
            }
            val prevStory = currentState.stories[prevIndex]
            if (prevStory.mediaType != StoryMediaType.VIDEO) {
                startProgress()
            }
            markAsSeen(prevStory.id)
        } else {

             _uiState.update { it.copy(progress = 0f) }
             val currentStory = currentState.stories.getOrNull(0)
             if (currentStory?.mediaType != StoryMediaType.VIDEO) {
                 startProgress()
             }
        }
    }

    fun pause() {
        _uiState.update { it.copy(isPaused = true) }
        stopProgress()
    }

    fun resume() {
        _uiState.update { it.copy(isPaused = false) }





















        startProgress()
    }

    fun onVideoReady(durationMs: Long) {

        if (_uiState.value.isPaused || _uiState.value.isFinished) return


        startProgress(durationOverride = durationMs)
    }

    private fun markAsSeen(storyId: String?) {
        if (storyId == null) return
        val currentUserId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            storyRepository.markAsSeen(storyId, currentUserId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgress()
    }
}
