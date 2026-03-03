package com.synapse.social.studioasinc.feature.stories.tray

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.StoryRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.model.StoryTrayState
import com.synapse.social.studioasinc.domain.model.StoryWithUser
import com.synapse.social.studioasinc.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StoryTrayViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _storyTrayState = MutableStateFlow(StoryTrayState())
    val storyTrayState: StateFlow<StoryTrayState> = _storyTrayState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val currentUserId: String?
        get() = authRepository.getCurrentUserId()

    init {
        loadCurrentUser()
        loadStories()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            try {
                val result = withContext(Dispatchers.IO) {
                    userRepository.getUserById(userId)
                }
                if (result.isSuccess) {
                    _currentUser.value = result.getOrNull()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadStories() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _storyTrayState.update { it.copy(isLoading = true, error = null) }

            storyRepository.getActiveStories(userId)
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    _storyTrayState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
                .collect { stories ->
                    val myStory = stories.find { it.user.uid == userId }
                    val friendStories = stories.filter { it.user.uid != userId }

                    _storyTrayState.update {
                        StoryTrayState(
                            myStory = myStory,
                            friendStories = friendStories,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun refresh() {
        loadStories()
    }

    fun markStoryAsSeen(storyId: String) {
        val viewerId = currentUserId ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                storyRepository.markAsSeen(storyId, viewerId)
            }
        }
    }

    fun markStoriesAsSeen(storyWithUser: StoryWithUser) {
        _storyTrayState.update { state ->
            val updatedFriends = state.friendStories.map { story ->
                if (story.user.uid == storyWithUser.user.uid) {
                    story.copy(hasUnseenStories = false)
                } else {
                    story
                }
            }
            state.copy(friendStories = updatedFriends)
        }
    }
}
