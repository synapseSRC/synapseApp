package com.synapse.social.studioasinc.feature.profile.editprofile.featured

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.data.repository.PostRepositoryImpl
import com.synapse.social.studioasinc.domain.model.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeaturedContentViewModel @Inject constructor(
    private val editProfileRepository: EditProfileRepositoryImpl,
    private val postRepository: PostRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeaturedContentUiState())
    val uiState: StateFlow<FeaturedContentUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<FeaturedContentNavigation>()
    val navigationEvents: SharedFlow<FeaturedContentNavigation> = _navigationEvents.asSharedFlow()

    init {
        loadUserContent()
    }

    fun loadUserContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = editProfileRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            val postsResult = postRepository.getUserPosts(userId)
            postsResult.fold(
                onSuccess = { posts ->
                    val eligiblePosts = posts.filter { post ->
                        post.authorUid == userId && post.isDeleted != true
                    }

                    val mediaItems = mutableListOf<FeaturedMediaItem>()
                    eligiblePosts.forEach { post ->
                        post.mediaItems?.forEach { media ->
                            if (media.url.isNotBlank()) {
                                mediaItems.add(
                                    FeaturedMediaItem(
                                        url = media.url,
                                        sourcePostId = post.id,
                                        isVideo = media.type == MediaType.VIDEO,
                                        caption = post.postText
                                    )
                                )
                            }
                        }
                        if (mediaItems.none { it.sourcePostId == post.id } && !post.postImage.isNullOrBlank()) {
                            mediaItems.add(
                                FeaturedMediaItem(
                                    url = post.postImage!!,
                                    sourcePostId = post.id,
                                    isVideo = false,
                                    caption = post.postText
                                )
                            )
                        }
                    }

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            currentUserId = userId,
                            candidatePosts = eligiblePosts,
                            candidateMedia = mediaItems.distinctBy { it.url },
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to load candidate content")
                    }
                }
            )
        }
    }

    fun onEvent(event: FeaturedContentEvent) {
        when (event) {
            is FeaturedContentEvent.TabSelected -> {
                _uiState.update { it.copy(selectedTab = event.tab) }
            }
            is FeaturedContentEvent.TogglePostSelected -> {
                _uiState.update { state ->
                    val currentSelected = state.selectedPostIds.toMutableList()
                    if (currentSelected.contains(event.postId)) {
                        currentSelected.remove(event.postId)
                    } else {
                        currentSelected.add(event.postId)
                    }
                    state.copy(selectedPostIds = currentSelected, hasPendingChanges = true)
                }
            }
            is FeaturedContentEvent.ToggleMediaSelected -> {
                _uiState.update { state ->
                    val currentSelected = state.selectedMediaUrls.toMutableList()
                    if (currentSelected.contains(event.mediaUrl)) {
                        currentSelected.remove(event.mediaUrl)
                    } else {
                        currentSelected.add(event.mediaUrl)
                    }
                    state.copy(selectedMediaUrls = currentSelected, hasPendingChanges = true)
                }
            }
            is FeaturedContentEvent.MovePostOrderUp -> {
                _uiState.update { state ->
                    val currentSelected = state.selectedPostIds.toMutableList()
                    val index = currentSelected.indexOf(event.postId)
                    if (index > 0) {
                        val item = currentSelected.removeAt(index)
                        currentSelected.add(index - 1, item)
                    }
                    state.copy(selectedPostIds = currentSelected, hasPendingChanges = true)
                }
            }
            is FeaturedContentEvent.MovePostOrderDown -> {
                _uiState.update { state ->
                    val currentSelected = state.selectedPostIds.toMutableList()
                    val index = currentSelected.indexOf(event.postId)
                    if (index in 0 until currentSelected.lastIndex) {
                        val item = currentSelected.removeAt(index)
                        currentSelected.add(index + 1, item)
                    }
                    state.copy(selectedPostIds = currentSelected, hasPendingChanges = true)
                }
            }
            is FeaturedContentEvent.MoveMediaOrderUp -> {
                _uiState.update { state ->
                    val currentSelected = state.selectedMediaUrls.toMutableList()
                    val index = currentSelected.indexOf(event.mediaUrl)
                    if (index > 0) {
                        val item = currentSelected.removeAt(index)
                        currentSelected.add(index - 1, item)
                    }
                    state.copy(selectedMediaUrls = currentSelected, hasPendingChanges = true)
                }
            }
            is FeaturedContentEvent.MoveMediaOrderDown -> {
                _uiState.update { state ->
                    val currentSelected = state.selectedMediaUrls.toMutableList()
                    val index = currentSelected.indexOf(event.mediaUrl)
                    if (index in 0 until currentSelected.lastIndex) {
                        val item = currentSelected.removeAt(index)
                        currentSelected.add(index + 1, item)
                    }
                    state.copy(selectedMediaUrls = currentSelected, hasPendingChanges = true)
                }
            }
            FeaturedContentEvent.ClearAllSelected -> {
                _uiState.update { state ->
                    state.copy(
                        selectedPostIds = emptyList(),
                        selectedMediaUrls = emptyList(),
                        hasPendingChanges = true
                    )
                }
            }
            FeaturedContentEvent.ApplyChanges -> {
                _uiState.update { state ->
                    state.copy(hasPendingChanges = false)
                }
            }
            FeaturedContentEvent.BackClicked -> {
                viewModelScope.launch { _navigationEvents.emit(FeaturedContentNavigation.NavigateBack) }
            }
            FeaturedContentEvent.DismissError -> {
                _uiState.update { it.copy(error = null) }
            }
            FeaturedContentEvent.DismissCapabilityNotice -> {
                _uiState.update { it.copy(showBackendCapabilityNotice = false) }
            }
        }
    }
}
