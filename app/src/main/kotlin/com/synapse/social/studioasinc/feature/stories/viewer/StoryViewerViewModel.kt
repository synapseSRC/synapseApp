package com.synapse.social.studioasinc.feature.stories.viewer

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.model.StoryReaction
import com.synapse.social.studioasinc.data.repository.StoryRepository
import com.synapse.social.studioasinc.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.domain.model.Story
import androidx.compose.ui.layout.ContentScale
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryViewWithUser
import com.synapse.social.studioasinc.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val isFinished: Boolean = false,
    val contentScale: ContentScale = ContentScale.Crop,
    val viewers: List<StoryViewWithUser> = emptyList(),
    val reactions: List<StoryReaction> = emptyList(),
    val isLoadingViewers: Boolean = false,
    val showViewersSheet: Boolean = false,
    val showReactionsSheet: Boolean = false,
    val isOwnStory: Boolean = false,
    val userReaction: String? = null,
    val reactionsCount: Int = 0,
    val replyText: String = "",
    val isReplying: Boolean = false,
    val showOptionsSheet: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)

@HiltViewModel
class StoryViewerViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val userRepository: UserRepositoryImpl,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
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
            val (user, stories) = coroutineScope {
                val userResultDeferred = async { userRepository.getUserById(userId) }
                val storiesResultDeferred = async { storyRepository.getUserStories(userId) }

                val userResult = userResultDeferred.await()
                val storiesResult = storiesResultDeferred.await()

                Pair(userResult.getOrNull(), storiesResult.getOrNull() ?: emptyList())
            }

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
                    val currentUserId = authRepository.getCurrentUserId()
                    val isOwnStory = stories.isNotEmpty() && stories[0].userId == currentUserId
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            stories = stories,
                            user = user,
                            currentStoryIndex = 0,
                            progress = 0f,
                            isOwnStory = isOwnStory
                        )
                    }
                    loadStoryData(stories[0].id)
                    // Removed automatic startProgress and markAsSeen - handled by pager visibility
                }
            }
        }
    }

    private fun loadStoryData(storyId: String?) {
        if (storyId == null) return
        val currentUserId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val reactionsResult = storyRepository.getReactions(storyId)
            val reactions = reactionsResult.getOrNull() ?: emptyList()
            val userReaction = reactions.find { it.userId == currentUserId }?.emoji

            _uiState.update {
                it.copy(
                    reactionsCount = reactions.size,
                    userReaction = userReaction
                )
            }
        }
    }

    fun startProgress(durationOverride: Long? = null) {
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

    fun stopProgress() {
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
                    progress = 0f,
                    replyText = "",
                    isReplying = false
                )
            }
            val nextStory = currentState.stories[nextIndex]
            if (nextStory.mediaType != StoryMediaType.VIDEO) {
                startProgress()
            }
            markAsSeen(nextStory.id)
            loadStoryData(nextStory.id)
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
                    progress = 0f,
                    replyText = "",
                    isReplying = false
                )
            }
            val prevStory = currentState.stories[prevIndex]
            if (prevStory.mediaType != StoryMediaType.VIDEO) {
                startProgress()
            }
            markAsSeen(prevStory.id)
            loadStoryData(prevStory.id)
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

    fun loadViewers(storyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingViewers = true) }
            val result = storyRepository.getStoryViewers(storyId)
            _uiState.update {
                it.copy(
                    isLoadingViewers = false,
                    viewers = result.getOrNull() ?: emptyList()
                )
            }
        }
    }

    fun showViewersSheet() {
        _uiState.update { it.copy(showViewersSheet = true) }
    }

    fun hideViewersSheet() {
        _uiState.update { it.copy(showViewersSheet = false) }
    }

    fun showReactionsSheet() {
        _uiState.update { it.copy(showReactionsSheet = true) }
    }

    fun hideReactionsSheet() {
        _uiState.update { it.copy(showReactionsSheet = false) }
    }

    fun showOptionsSheet() {
        pause()
        _uiState.update { it.copy(showOptionsSheet = true) }
    }

    fun hideOptionsSheet() {
        _uiState.update { it.copy(showOptionsSheet = false) }
        resume()
    }

    fun onVideoReady(durationMs: Long) {

        if (_uiState.value.isPaused || _uiState.value.isFinished) return


        startProgress(durationOverride = durationMs)
    }

    fun cycleContentScale() {
        _uiState.update { state ->
            val nextScale = when (state.contentScale) {
                ContentScale.Crop -> ContentScale.Fit
                ContentScale.Fit -> ContentScale.FillBounds
                ContentScale.FillBounds -> ContentScale.Inside
                else -> ContentScale.Crop
            }
            state.copy(contentScale = nextScale)
        }
    }

    fun markAsSeen(storyId: String?) {
        if (storyId == null) return
        val currentUserId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            storyRepository.markAsSeen(storyId, currentUserId)
        }
    }

    fun reactToStory(emoji: String) {
        val currentStory = _uiState.value.stories.getOrNull(_uiState.value.currentStoryIndex) ?: return
        val storyId = currentStory.id ?: return
        val currentUserId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            val result = storyRepository.reactToStory(storyId, currentUserId, emoji)
            if (result.isSuccess) {
                loadStoryData(storyId)
            }
        }
    }

    fun removeReaction() {
        val currentStory = _uiState.value.stories.getOrNull(_uiState.value.currentStoryIndex) ?: return
        val storyId = currentStory.id ?: return
        val currentUserId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            val result = storyRepository.removeReaction(storyId, currentUserId)
            if (result.isSuccess) {
                loadStoryData(storyId)
            }
        }
    }

    fun updateReplyText(text: String) {
        _uiState.update { it.copy(replyText = text, isReplying = text.isNotEmpty()) }
    }

    fun sendReply() {
        val currentState = _uiState.value
        val currentStory = currentState.stories.getOrNull(currentState.currentStoryIndex) ?: return
        val storyOwnerId = currentStory.userId
        val replyText = currentState.replyText
        if (replyText.isBlank()) return

        viewModelScope.launch {
            val chatResult = chatRepository.getOrCreateChat(storyOwnerId)
            val chatId = chatResult.getOrNull()
            if (chatId != null) {
                val message = "[Story reply] ${currentStory.mediaUrl}\n$replyText"
                chatRepository.sendMessage(chatId, message)
                _uiState.update { it.copy(replyText = "", isReplying = false) }
                resume()
            }
        }
    }

    fun showDeleteConfirmation() {
        pause()
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
        resume()
    }

    fun confirmDeleteStory(storyId: String) {
        viewModelScope.launch {
            val result = storyRepository.deleteStory(storyId)
            if (result.isSuccess) {
                _uiState.update { it.copy(showDeleteConfirmation = false) }
                nextStory()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgress()
    }
}
