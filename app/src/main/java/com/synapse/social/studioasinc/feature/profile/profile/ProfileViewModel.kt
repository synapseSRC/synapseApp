package com.synapse.social.studioasinc.feature.profile.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserIdUseCase
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.usecase.post.BookmarkPostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ReactToPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ReportPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.VotePollUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.ArchiveProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.FollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetFollowingUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileContentUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.SearchUsersUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.IsFollowingUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.LockProfileUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.MuteUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.ReportUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.UnfollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.story.HasActiveStoryUseCase
import com.synapse.social.studioasinc.feature.profile.profile.components.FollowingUser
import com.synapse.social.studioasinc.feature.profile.profile.components.ViewAsMode
import com.synapse.social.studioasinc.feature.shared.components.post.PostCardState
import com.synapse.social.studioasinc.feature.shared.components.post.PostEvent
import com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus
import com.synapse.social.studioasinc.feature.shared.components.post.PostUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProfileScreenState(
    val profileState: ProfileUiState = ProfileUiState.Loading,
    val contentFilter: ProfileContentFilter = ProfileContentFilter.POSTS,
    val posts: List<Any> = emptyList(),
    val photos: List<Any> = emptyList(),
    val reels: List<Any> = emptyList(),
    val followingList: List<FollowingUser> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val postsOffset: Int = 0,
    val photosOffset: Int = 0,
    val reelsOffset: Int = 0,
    val currentUserId: String = "",
    val isOwnProfile: Boolean = false,
    val showMoreMenu: Boolean = false,
    val likedPostIds: Set<String> = emptySet(),
    val savedPostIds: Set<String> = emptySet(),
    val showShareSheet: Boolean = false,
    val showViewAsSheet: Boolean = false,
    val showQrCode: Boolean = false,
    val showReportDialog: Boolean = false,
    val viewAsMode: ViewAsMode? = null,
    val viewAsUserName: String? = null,
    val hasStory: Boolean = false,
    val isFollowLoading: Boolean = false,
    val searchResults: List<com.synapse.social.studioasinc.shared.domain.model.User> = emptyList(),
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val blockSuccess: Boolean = false,
    val blockError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfileContentUseCase: GetProfileContentUseCase,
    private val getFollowingUseCase: GetFollowingUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val reactToPostUseCase: ReactToPostUseCase,
    private val votePollUseCase: VotePollUseCase,
    private val bookmarkPostUseCase: BookmarkPostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val reportPostUseCase: ReportPostUseCase,
    private val lockProfileUseCase: LockProfileUseCase,
    private val archiveProfileUseCase: ArchiveProfileUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val reportUserUseCase: ReportUserUseCase,
    private val muteUserUseCase: MuteUserUseCase,
    private val isFollowingUseCase: IsFollowingUseCase,
    private val hasActiveStoryUseCase: HasActiveStoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            PostEventBus.events.collect { event ->
                when (event) {
                    is PostEvent.Updated -> {
                        _state.update { currentState ->
                            val updatedPosts = currentState.posts.map { item ->
                                if (item is Post && item.id == event.post.id) event.post else item
                            }
                            currentState.copy(posts = updatedPosts)
                        }
                    }
                    is PostEvent.Deleted -> {
                         _state.update { currentState ->
                            val updatedPosts = currentState.posts.filterNot { item ->
                                item is Post && item.id == event.postId
                            }
                            currentState.copy(posts = updatedPosts)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            if (!_state.value.isRefreshing) {
                _state.update { it.copy(profileState = ProfileUiState.Loading) }
            }
            
            val currentUserUid = getCurrentUserIdUseCase() ?: ""
            val isOwnProfile = currentUserUid == userId

            _state.update {
                it.copy(
                    currentUserId = currentUserUid,
                    isOwnProfile = isOwnProfile
                )
            }

            getProfileUseCase(userId).collect { result ->
                result.onSuccess { profile ->
                    _state.update { it.copy(profileState = ProfileUiState.Success(profile)) }
                    checkStory(userId)

                    if (!isOwnProfile) {
                         val followResult = isFollowingUseCase(currentUserUid, userId)
                         followResult.onSuccess { isFollowing ->
                             _state.update { it.copy(isFollowing = isFollowing) }
                         }
                    }

                    loadContent(userId, _state.value.contentFilter)
                }.onFailure { error ->
                    _state.update { it.copy(profileState = ProfileUiState.Error(error.message ?: "Unknown error")) }
                }
                
                // Reset refreshing state after profile is loaded (or failed)
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshProfile(userId: String) {
        _state.update { it.copy(isRefreshing = true) }
        loadProfile(userId)
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            val currentUserId = _state.value.currentUserId
            _state.update { it.copy(isFollowLoading = true) }
            followUserUseCase(currentUserId, userId).onSuccess {
                _state.update { it.copy(isFollowing = true, isFollowLoading = false) }
            }.onFailure {
                _state.update { it.copy(isFollowLoading = false) }
            }
        }
    }

    fun unfollowUser(userId: String) {
         viewModelScope.launch {
            val currentUserId = _state.value.currentUserId
            _state.update { it.copy(isFollowLoading = true) }
            unfollowUserUseCase(currentUserId, userId).onSuccess {
                _state.update { it.copy(isFollowing = false, isFollowLoading = false) }
            }.onFailure {
                _state.update { it.copy(isFollowLoading = false) }
            }
        }
    }

    fun reactToPost(post: Post, reactionType: ReactionType) {
        viewModelScope.launch {
            reactToPostUseCase(post, reactionType).collect { result ->
                result.onSuccess { updatedPost ->
                     PostEventBus.emit(PostEvent.Updated(updatedPost))
                }
            }
        }
    }

    fun toggleSave(postId: String) {
        val isSaved = postId in _state.value.savedPostIds
        val currentUserId = _state.value.currentUserId

        _state.update { state ->
            val savedPostIds = state.savedPostIds.toMutableSet()
            if (isSaved) savedPostIds.remove(postId) else savedPostIds.add(postId)
            state.copy(savedPostIds = savedPostIds)
        }

        viewModelScope.launch {
            bookmarkPostUseCase(postId, currentUserId, isSaved).collect { result ->
                result.onFailure {
                    _state.update { state ->
                        val savedPostIds = state.savedPostIds.toMutableSet()
                        if (isSaved) savedPostIds.add(postId) else savedPostIds.remove(postId)
                        state.copy(savedPostIds = savedPostIds)
                    }
                }
            }
        }
    }

    fun votePoll(post: Post, optionIndex: Int) {
        viewModelScope.launch {
            votePollUseCase(post, optionIndex).collect { result ->
                result.onSuccess { updatedPost ->
                    PostEventBus.emit(PostEvent.Updated(updatedPost))
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId).collect { result ->
                result.onSuccess {
                    PostEventBus.emit(PostEvent.Deleted(postId))
                }
            }
        }
    }

    fun reportPost(postId: String, reason: String) {
        viewModelScope.launch {
            reportPostUseCase(postId, reason, null).collect { }
        }
    }

    // UI Helpers
    fun showShareSheet() { _state.update { it.copy(showShareSheet = true) } }
    fun hideShareSheet() { _state.update { it.copy(showShareSheet = false) } }
    fun showViewAsSheet() { _state.update { it.copy(showViewAsSheet = true) } }
    fun hideViewAsSheet() { _state.update { it.copy(showViewAsSheet = false) } }
    fun showQrCode() { _state.update { it.copy(showQrCode = true) } }
    fun hideQrCode() { _state.update { it.copy(showQrCode = false) } }
    fun showReportDialog() { _state.update { it.copy(showReportDialog = true) } }
    fun hideReportDialog() { _state.update { it.copy(showReportDialog = false) } }

    fun setViewAsMode(mode: ViewAsMode, userName: String? = null) {
        _state.update { it.copy(viewAsMode = mode, viewAsUserName = userName) }
    }

    fun exitViewAs() {
        _state.update { it.copy(viewAsMode = null, viewAsUserName = null) }
    }

    fun toggleMoreMenu() {
        _state.update { it.copy(showMoreMenu = !it.showMoreMenu) }
    }

    fun switchContentFilter(filter: ProfileContentFilter) {
        if (_state.value.contentFilter == filter) return
        _state.update { it.copy(contentFilter = filter) }
        val profile = (_state.value.profileState as? ProfileUiState.Success)?.profile
        if (profile != null) {
             loadContent(profile.id, filter)
        }
    }

    fun searchUsers(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (query.isBlank()) {
                _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
                return@launch
            }

            _state.update { it.copy(isSearching = true) }

            searchUsersUseCase(query).onSuccess { results ->
                _state.update { it.copy(searchResults = results, isSearching = false) }
            }.onFailure {
                _state.update { it.copy(isSearching = false) }
            }
        }
    }

    fun clearSearchResults() {
        _state.update { it.copy(searchResults = emptyList()) }
    }

    fun lockProfile(isLocked: Boolean) {
        viewModelScope.launch {
            lockProfileUseCase(_state.value.currentUserId, isLocked).collect {}
        }
    }

    fun archiveProfile(isArchived: Boolean) {
        viewModelScope.launch {
            archiveProfileUseCase(_state.value.currentUserId, isArchived).collect {}
        }
    }

    fun blockUser(blockedUserId: String) {
        viewModelScope.launch {
            _state.update { it.copy(blockSuccess = false, blockError = null) }
            
            blockUserUseCase(blockedUserId)
                .onSuccess {
                    _state.update { it.copy(blockSuccess = true) }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(blockError = error.message ?: "Failed to block user")
                    }
                }
        }
    }
    
    fun clearBlockStatus() {
        _state.update { it.copy(blockSuccess = false, blockError = null) }
    }

    fun reportUser(reportedUserId: String, reason: String) {
        viewModelScope.launch {
            reportUserUseCase(_state.value.currentUserId, reportedUserId, reason).collect {}
        }
    }

    fun muteUser(mutedUserId: String) {
        viewModelScope.launch {
            muteUserUseCase(_state.value.currentUserId, mutedUserId).collect {}
        }
    }

    fun mapPostToState(post: Post): PostCardState {
        val currentProfile = (_state.value.profileState as? ProfileUiState.Success)?.profile
        return PostUiMapper.mapToState(post, currentProfile)
    }

    private fun checkStory(userId: String) {
        viewModelScope.launch {
            hasActiveStoryUseCase(userId).onSuccess { hasStory ->
                _state.update { it.copy(hasStory = hasStory) }
            }
        }
    }

    private fun loadContent(userId: String, filter: ProfileContentFilter) {
        viewModelScope.launch {
            if (filter == ProfileContentFilter.POSTS) {
                getFollowingUseCase(userId).onSuccess { users ->
                    val followingUsers = users.map { user ->
                        FollowingUser(
                            id = user.id,
                            username = user.username,
                            name = user.name ?: user.username,
                            avatarUrl = user.avatar,
                            isMutual = false
                        )
                    }
                    _state.update { it.copy(followingList = followingUsers) }
                }
            }

            when (filter) {
                ProfileContentFilter.POSTS -> {
                    getProfileContentUseCase.getPosts(userId).onSuccess { posts ->
                        _state.update { it.copy(posts = posts, postsOffset = posts.size) }
                    }
                }
                ProfileContentFilter.PHOTOS -> {
                    getProfileContentUseCase.getPhotos(userId).onSuccess { photos ->
                        _state.update { it.copy(photos = photos, photosOffset = photos.size) }
                    }
                }
                ProfileContentFilter.REELS -> {
                    getProfileContentUseCase.getReels(userId).onSuccess { reels ->
                        _state.update { it.copy(reels = reels, reelsOffset = reels.size) }
                    }
                }
            }
        }
    }
}
