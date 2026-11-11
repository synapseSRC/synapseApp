package com.synapse.social.studioasinc.feature.profile.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.data.repository.toData
import com.synapse.social.studioasinc.domain.model.UserProfile as DomainUserProfile
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
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfilePostsUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfilePhotosUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileReelsUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileRepliesUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.SearchUsersUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileFollowStatusUseCase
import com.synapse.social.studioasinc.domain.model.ProfileFollowStatus
import com.synapse.social.studioasinc.domain.usecase.profile.LockProfileUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.MuteUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.ReportUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.UnfollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.story.HasActiveStoryUseCase
import com.synapse.social.studioasinc.data.repository.PostRepositoryImpl
import com.synapse.social.studioasinc.feature.profile.profile.components.FollowingUser
import com.synapse.social.studioasinc.feature.profile.profile.components.ViewAsMode
import com.synapse.social.studioasinc.feature.shared.components.post.PostCardState
import com.synapse.social.studioasinc.feature.shared.components.post.PostEvent
import com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus
import com.synapse.social.studioasinc.feature.shared.components.post.PostUiMapper
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizePostUseCase
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
import java.util.concurrent.atomic.AtomicBoolean
import com.synapse.social.studioasinc.core.util.NotificationHelper

data class ProfileScreenState(
    val profileState: ProfileUiState = ProfileUiState.Loading,
    val contentFilter: ProfileContentFilter = ProfileContentFilter.POSTS,
    val posts: List<Any> = emptyList(),
    val photos: List<Any> = emptyList(),
    val reels: List<Any> = emptyList(),
    val followingList: List<FollowingUser> = emptyList(),
    val isFollowing: Boolean = false,
    val isFollowRequested: Boolean = false,
    val followStatusError: String? = null,
    val isLoadingMore: Boolean = false,
    val postsOffset: Int = 0,
    val photosOffset: Int = 0,
    val reelsOffset: Int = 0,
    val repliesOffset: Int = 0,
    val replies: List<Any> = emptyList(),
    val currentUserId: String = "",
    val isOwnProfile: Boolean = false,
    val showMoreMenu: Boolean = false,
    val likedPostIds: Set<String> = emptySet(),
    val savedPostIds: Set<String> = emptySet(),
    val showViewAsSheet: Boolean = false,
    val showReportDialog: Boolean = false,
    val viewAsMode: ViewAsMode? = null,
    val viewAsUserName: String? = null,
    val hasStory: Boolean = false,
    val isFollowLoading: Boolean = false,
    val searchResults: List<com.synapse.social.studioasinc.shared.domain.model.User> = emptyList(),
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val blockSuccess: Boolean = false,
    val blockError: String? = null,
    val isSummarizing: Boolean = false,
    val postSummary: String? = null,
    val summaryError: String? = null,
    val navigateToLockProfile: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfilePostsUseCase: GetProfilePostsUseCase,
    private val getProfilePhotosUseCase: GetProfilePhotosUseCase,
    private val getProfileReelsUseCase: GetProfileReelsUseCase,
    private val getProfileRepliesUseCase: GetProfileRepliesUseCase,
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
    private val getProfileFollowStatusUseCase: GetProfileFollowStatusUseCase,
    private val hasActiveStoryUseCase: HasActiveStoryUseCase,
    private val postRepository: PostRepositoryImpl,
    private val summarizePostUseCase: SummarizePostUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var contentJob: Job? = null
    private var storyJob: Job? = null
    private val isFollowOperationInProgress = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            PostEventBus.events.collect { event ->
                when (event) {
                    is PostEvent.Updated -> {
                        _state.update { currentState ->
                            val updatedPosts = currentState.posts.map { item ->
                                if (item is Post && item.id == event.post.id) event.post else item
                            }
                            val updatedReplies = currentState.replies.map { item ->
                                if (item is com.synapse.social.studioasinc.domain.model.CommentWithUser && item.id == event.post.id) {
                                    item.copy(
                                        likesCount = event.post.likesCount,
                                        userReaction = event.post.userReaction
                                    )
                                } else item
                            }
                            currentState.copy(posts = updatedPosts, replies = updatedReplies)
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

    fun loadProfile(userId: String, refresh: Boolean = false) {
        contentJob?.cancel()
        storyJob?.cancel()
        viewModelScope.launch {
            val currentUserUid = getCurrentUserIdUseCase().orEmpty()
            val isOwnProfile = currentUserUid == userId
            val followOpInProgress = isFollowOperationInProgress.get()
            _state.update {
                it.copy(
                    profileState = ProfileUiState.Loading,
                    currentUserId = currentUserUid,
                    isOwnProfile = isOwnProfile,
                    isFollowing = if (followOpInProgress) it.isFollowing else false,
                    isFollowRequested = if (followOpInProgress) it.isFollowRequested else false,
                    followStatusError = null,
                    hasStory = false,
                    posts = emptyList(),
                    photos = emptyList(),
                    reels = emptyList(),
                    replies = emptyList(),
                    followingList = emptyList(),
                    postsOffset = 0,
                    photosOffset = 0,
                    reelsOffset = 0,
                    repliesOffset = 0
                )
            }

            getProfileUseCase(userId, refresh).collect { result ->
                result.onSuccess { profile ->
                    val profileUid = profile.uid
                    val followStatusResult: Result<ProfileFollowStatus> = if (isOwnProfile) {
                        Result.success(ProfileFollowStatus.NONE)
                    } else {
                        getProfileFollowStatusUseCase(profileUid)
                    }
                    val followStatus = followStatusResult.getOrElse { ProfileFollowStatus.NONE }
                    val canViewProtectedContent = canViewProfileContent(
                        isPrivate = profile.isPrivate,
                        isOwnProfile = isOwnProfile,
                        isFollowing = followStatus == ProfileFollowStatus.FOLLOWING
                    )
                    _state.update {
                        it.copy(
                            profileState = ProfileUiState.Success(profile.toData()),
                            isFollowing = followStatus == ProfileFollowStatus.FOLLOWING,
                            isFollowRequested = followStatus == ProfileFollowStatus.REQUESTED,
                            followStatusError = followStatusResult.exceptionOrNull()?.message
                        )
                    }
                    if (canViewProtectedContent) {
                        checkStory(profileUid)
                        loadContent(profileUid, _state.value.contentFilter)
                    }
                }.onFailure { error ->
                    _state.update { it.copy(profileState = ProfileUiState.Error(error.message ?: "Unknown error")) }
                }
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshProfile(userId: String) {
        _state.update { it.copy(isRefreshing = true) }
        loadProfile(userId, refresh = true)
    }

    fun followUser(userId: String) {
        val currentUserId = _state.value.currentUserId
        if (currentUserId.isBlank() || currentUserId == userId) return
        if (!isFollowOperationInProgress.compareAndSet(false, true)) return

        val previousState = _state.value
        android.util.Log.d("ProfileViewModel", "followUser: userId=$userId currentUserId=$currentUserId previousIsFollowing=${previousState.isFollowing}")
        val isPrivate = (previousState.profileState as? ProfileUiState.Success)?.profile?.isPrivate == true
        val targetFollowing = !isPrivate
        val targetRequested = isPrivate
        val delta = if (targetFollowing && !previousState.isFollowing) 1 else 0

        // Optimistic update: immediately update following state and follower count
        _state.update { state ->
            state.copy(
                isFollowing = targetFollowing,
                isFollowRequested = targetRequested,
                isFollowLoading = false,
                followStatusError = null,
                profileState = (state.profileState as? ProfileUiState.Success)?.let { success ->
                    success.copy(profile = success.profile.copy(followerCount = maxOf(0, success.profile.followerCount + delta)))
                } ?: state.profileState
            )
        }

        viewModelScope.launch {
            try {
                followUserUseCase(currentUserId, userId).fold(
                    onSuccess = {
                        android.util.Log.d("ProfileViewModel", "followUser: success userId=$userId")
                        // Verify follow status with server to ensure UI reflects actual backend state
                        getProfileFollowStatusUseCase(userId).fold(
                            onSuccess = { followStatus ->
                                _state.update { state ->
                                    val ps = state.profileState
                                    state.copy(
                                        isFollowing = followStatus == ProfileFollowStatus.FOLLOWING,
                                        isFollowRequested = followStatus == ProfileFollowStatus.REQUESTED,
                                        isFollowLoading = false,
                                        followStatusError = null,
                                        // If content became accessible (public profile followed), load it
                                        profileState = if (followStatus == ProfileFollowStatus.FOLLOWING && ps is ProfileUiState.Success) {
                                            val currentProfile = ps.profile
                                            val canView = canViewProfileContent(
                                                isPrivate = currentProfile.isPrivate,
                                                isOwnProfile = state.isOwnProfile,
                                                isFollowing = true
                                            )
                                            if (canView) {
                                                // Trigger content load via update - contentJob will be handled by observer
                                                ps
                                            } else {
                                                ps
                                            }
                                        } else ps
                                    )
                                }
                                // For public profiles that are now following, load content
                                if (followStatus == ProfileFollowStatus.FOLLOWING) {
                                    checkStory(userId)
                                    loadContent(userId, _state.value.contentFilter)
                                }
                            },
                            onFailure = { error ->
                                android.util.Log.w("ProfileViewModel", "Failed to verify follow status: ${error.message}")
                                // Keep optimistic state but clear loading
                                _state.update { it.copy(isFollowLoading = false) }
                            }
                        )
                    },
                    onFailure = { error ->
                        android.util.Log.e("ProfileViewModel", "followUser: failure userId=$userId error=${error.message}", error)
                        // Revert optimistic update on failure
                        _state.value = previousState.copy(
                            isFollowLoading = false,
                            followStatusError = error.message
                        )
                    }
                )
            } finally {
                isFollowOperationInProgress.set(false)
            }
        }
    }

    fun unfollowUser(userId: String) {
        val currentUserId = _state.value.currentUserId
        if (currentUserId.isBlank() || currentUserId == userId) return
        if (!isFollowOperationInProgress.compareAndSet(false, true)) return

        val previousState = _state.value
        val wasFollowing = previousState.isFollowing

        // Optimistic update: immediately clear following/requested and decrement count
        _state.update { state ->
            state.copy(
                isFollowing = false,
                isFollowRequested = false,
                isFollowLoading = false,
                followStatusError = null,
                profileState = (state.profileState as? ProfileUiState.Success)?.let { success ->
                    success.copy(profile = success.profile.copy(followerCount = if (wasFollowing) maxOf(0, success.profile.followerCount - 1) else success.profile.followerCount))
                } ?: state.profileState
            )
        }

        viewModelScope.launch {
            try {
                unfollowUserUseCase(currentUserId, userId).fold(
                    onSuccess = {
                        // Verify follow status with server to ensure UI reflects actual backend state
                        getProfileFollowStatusUseCase(userId).fold(
                            onSuccess = { followStatus ->
                                _state.update { state ->
                                    state.copy(
                                        isFollowing = followStatus == ProfileFollowStatus.FOLLOWING,
                                        isFollowRequested = followStatus == ProfileFollowStatus.REQUESTED,
                                        isFollowLoading = false,
                                        followStatusError = null,
                                        profileState = (state.profileState as? ProfileUiState.Success)?.let { success ->
                                            success.copy(profile = success.profile.copy(followerCount = maxOf(0, success.profile.followerCount - if (wasFollowing) 1 else 0)))
                                        } ?: state.profileState
                                    )
                                }
                            },
                            onFailure = { error ->
                                android.util.Log.w("ProfileViewModel", "Failed to verify unfollow status: ${error.message}")
                                // Keep optimistic state but clear loading
                                _state.update { it.copy(isFollowLoading = false) }
                            }
                        )
                        // Clear content if profile was private and we unfollowed
                        val isPrivate = (previousState.profileState as? ProfileUiState.Success)?.profile?.isPrivate == true
                        if (isPrivate) {
                            _state.update { state ->
                                state.copy(
                                    hasStory = false,
                                    posts = emptyList(), photos = emptyList(), reels = emptyList(), replies = emptyList(),
                                    followingList = emptyList()
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        // Revert optimistic update on failure
                        _state.value = previousState.copy(
                            isFollowLoading = false,
                            followStatusError = error.message
                        )
                    }
                )
            } finally {
                isFollowOperationInProgress.set(false)
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

    fun resharePost(post: Post) {
        viewModelScope.launch {
            val isCurrentlyReshared = post.isReshared
            val newResharesCount = if (isCurrentlyReshared) maxOf(0, post.resharesCount - 1) else post.resharesCount + 1
            val optimisticPost = post.copy(
                isReshared = !isCurrentlyReshared,
                resharesCount = newResharesCount
            )

            // Optimistic update
            _state.update { state ->
                state.copy(
                    posts = state.posts.map {
                        if (it is com.synapse.social.studioasinc.domain.model.Post && it.id == post.id) optimisticPost
                        else if (it is com.synapse.social.studioasinc.domain.model.FeedItem.PostItem && it.id == post.id) it.copy(post = optimisticPost)
                        else it
                    }
                )
            }
            com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus.emit(com.synapse.social.studioasinc.feature.shared.components.post.PostEvent.Updated(optimisticPost))

            val result = if (isCurrentlyReshared) {
                postRepository.unresharePost(post.id)
            } else {
                postRepository.resharePost(post.id)
            }

            result.onFailure {
                // Revert on failure
                _state.update { state ->
                    state.copy(
                        posts = state.posts.map {
                            if (it is com.synapse.social.studioasinc.domain.model.Post && it.id == post.id) post
                            else if (it is com.synapse.social.studioasinc.domain.model.FeedItem.PostItem && it.id == post.id) it.copy(post = post)
                            else it
                        }
                    )
                }
                com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus.emit(com.synapse.social.studioasinc.feature.shared.components.post.PostEvent.Updated(post))
            }
        }
    }

    fun quotePost(post: Post, text: String) {
        viewModelScope.launch {
            postRepository.quotePost(post.id, text).onSuccess {
                // Refresh or update state if needed
            }
        }
    }

    /**
     * Saves or unsaves [post] from wherever the profile shows it (post row or options sheet).
     *
     * The bookmark lives on the post, because that is what the profile renders: the row is built
     * from `state.posts`, and `savedPostIds` is not a source any of these composables read. The
     * current state is therefore taken from `post.isBookmarked` and not from `savedPostIds`,
     * which starts empty and would turn the first tap on an already-saved post into another save.
     *
     * The feed's own bookmark action works because it flips the post and announces it on
     * [PostEventBus]; this does the same flip, in state and on the bus, so the row recomposes
     * immediately and the change survives navigating away and back. A failure puts the original
     * post back rather than leaving the UI lying about what the backend holds.
     */
    fun toggleSave(post: Post) {
        val isSaved = post.isBookmarked
        val currentUserId = _state.value.currentUserId.ifBlank { getCurrentUserIdUseCase().orEmpty() }
        if (currentUserId.isBlank()) return

        val optimisticPost = post.copy(isBookmarked = !isSaved)
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    posts = state.posts.map { item ->
                        if (item is Post && item.id == post.id) optimisticPost else item
                    },
                    savedPostIds = state.savedPostIds.withBookmark(post.id, saved = !isSaved)
                )
            }
            PostEventBus.emit(PostEvent.Updated(optimisticPost))

            bookmarkPostUseCase(post.id, currentUserId, isSaved).collect { result ->
                result.onFailure {
                    _state.update { state ->
                        state.copy(
                            posts = state.posts.map { item ->
                                if (item is Post && item.id == post.id) post else item
                            },
                            savedPostIds = state.savedPostIds.withBookmark(post.id, saved = isSaved)
                        )
                    }
                    // Put the original back for everyone else already showing this post too.
                    PostEventBus.emit(PostEvent.Updated(post))
                }
            }
        }
    }

    private fun Set<String>.withBookmark(postId: String, saved: Boolean): Set<String> {
        val ids = toMutableSet()
        if (saved) ids.add(postId) else ids.remove(postId)
        return ids
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
    fun showViewAsSheet() { _state.update { it.copy(showViewAsSheet = true) } }
    fun hideViewAsSheet() { _state.update { it.copy(showViewAsSheet = false) } }
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

    fun hideMoreMenu() {
        _state.update { it.copy(showMoreMenu = false) }
    }

    fun showLockProfileScreen() {
        _state.update { it.copy(navigateToLockProfile = true, showMoreMenu = false) }
    }

    fun hideLockProfileScreen() {
        _state.update { it.copy(navigateToLockProfile = false) }
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

    private fun checkStory(userId: String) {
        storyJob?.cancel()
        storyJob = viewModelScope.launch {
            hasActiveStoryUseCase(userId).onSuccess { hasStory ->
                val current = _state.value
                val profile = (current.profileState as? ProfileUiState.Success)?.profile
                if (profile?.id != userId) return@onSuccess
                val authorized = profile?.let { canViewProfileContent(it.isPrivate, current.isOwnProfile, current.isFollowing) } == true
                _state.update { it.copy(hasStory = authorized && hasStory) }
            }
        }
    }

    private fun loadContent(userId: String, filter: ProfileContentFilter) {
        contentJob?.cancel()
        contentJob = viewModelScope.launch {
            val currentState = _state.value
            val loadedProfile = (currentState.profileState as? ProfileUiState.Success)?.profile
            if (loadedProfile != null && !canViewProfileContent(loadedProfile.isPrivate, currentState.isOwnProfile, currentState.isFollowing)) {
                _state.update { it.copy(posts = emptyList(), photos = emptyList(), reels = emptyList(), replies = emptyList(), followingList = emptyList(), hasStory = false) }
                return@launch
            }
            if (filter == ProfileContentFilter.POSTS) {
                getFollowingUseCase(userId).onSuccess { users ->
                    val followingUsers = users.map { user ->
                        FollowingUser(
                            id = user.uid,
                            username = user.username,
                            name = user.displayName ?: user.username,
                            avatarUrl = user.avatar,
                            isMutual = false
                        )
                    }
                    _state.update { it.copy(followingList = followingUsers) }
                }
            }

            when (filter) {
                ProfileContentFilter.POSTS -> {
                    getProfilePostsUseCase(userId).onSuccess { posts ->
                        _state.update { it.copy(posts = posts, postsOffset = posts.size) }
                    }
                }
                ProfileContentFilter.PHOTOS -> {
                    getProfilePhotosUseCase(userId).onSuccess { photos ->
                        _state.update { it.copy(photos = photos, photosOffset = photos.size) }
                    }
                }
                ProfileContentFilter.REELS -> {
                    getProfileReelsUseCase(userId).onSuccess { reels ->
                        _state.update { it.copy(reels = reels, reelsOffset = reels.size) }
                    }
                }
                ProfileContentFilter.REPLIES -> {
                    getProfileRepliesUseCase(userId).onSuccess { replies ->
                        _state.update { it.copy(replies = replies, repliesOffset = replies.size) }
                    }
                }
            }
        }
    }
}