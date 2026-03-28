package com.synapse.social.studioasinc.ui.createpost

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.util.UriUtils
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.domain.model.FeelingType
import com.synapse.social.studioasinc.domain.model.LocationData
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.usecase.post.GetPostByIdUseCase
import com.synapse.social.studioasinc.domain.usecase.user.GetUserByIdUseCase
import com.synapse.social.studioasinc.domain.usecase.post.GetDraftUseCase
import com.synapse.social.studioasinc.domain.usecase.post.SaveDraftUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ClearDraftUseCase
import com.synapse.social.studioasinc.domain.usecase.post.SubmitPostUseCase
import com.synapse.social.studioasinc.domain.usecase.search.SearchUsersForPostUseCase
import com.synapse.social.studioasinc.domain.usecase.search.SearchLocationsUseCase
import com.synapse.social.studioasinc.domain.usecase.search.SearchFeelingsUseCase
import com.synapse.social.studioasinc.shared.core.util.UiEvent
import com.synapse.social.studioasinc.shared.core.util.UiEventManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CompositionType {
    POST, REEL
}

private val allFeelings = listOf(
    FeelingActivity("😊", "Happy", FeelingType.MOOD),
    FeelingActivity("😢", "Sad", FeelingType.MOOD),
    FeelingActivity("😠", "Angry", FeelingType.MOOD),
    FeelingActivity("🤩", "Excited", FeelingType.MOOD),
    FeelingActivity("😴", "Tired", FeelingType.MOOD),
    FeelingActivity("🥰", "Loved", FeelingType.MOOD),
    FeelingActivity("😑", "Bored", FeelingType.MOOD),
    FeelingActivity("😰", "Anxious", FeelingType.MOOD),
    FeelingActivity("😕", "Confused", FeelingType.MOOD),
    FeelingActivity("🦚", "Proud", FeelingType.MOOD)
)

data class PostDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    val postText: String = "",
    val mediaItems: List<MediaItem> = emptyList()
)

data class CreatePostUiState(
    val compositionType: CompositionType = CompositionType.POST,
    val isLoading: Boolean = false,
    val postText: String = "",
    val mediaItems: List<MediaItem> = emptyList(),
    val pollData: PollData? = null,
    val location: LocationData? = null,
    val youtubeUrl: String? = null,
    val privacy: String = "public",
    val settings: PostSettings = PostSettings(),
    val isPostCreated: Boolean = false,
    val uploadProgress: Float = 0f,
    val isEditMode: Boolean = false,
    val replyToPostId: String? = null,
    val checkDraft: Boolean = true,
    val threadPosts: List<PostDraft> = emptyList(),
    val currentUserProfile: User? = null,

    val taggedPeople: List<User> = emptyList(),
    val feeling: FeelingActivity? = null,
    val textBackgroundColor: Long? = null,
    
    // Search related state
    val userSearchResults: List<User> = emptyList(),
    val locationSearchResults: List<LocationData> = emptyList(),
    val feelingSearchResults: List<FeelingActivity> = allFeelings,
    val isSearchLoading: Boolean = false
)

data class PollData(
    val question: String,
    val options: List<String>,
    val durationHours: Int
)

data class PostSettings(
    val hideViewsCount: Boolean = false,
    val hideLikeCount: Boolean = false,
    val hideCommentsCount: Boolean = false,
    val disableComments: Boolean = false
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    application: Application,
    private val getPostByIdUseCase: GetPostByIdUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getDraftUseCase: GetDraftUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val clearDraftUseCase: ClearDraftUseCase,
    private val searchUsersForPostUseCase: SearchUsersForPostUseCase,
    private val searchLocationsUseCase: SearchLocationsUseCase,
    private val searchFeelingsUseCase: SearchFeelingsUseCase,
    private val submitPostUseCase: SubmitPostUseCase
) : AndroidViewModel(application) {

    private val authService = SupabaseAuthenticationService()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private var editPostId: String? = null
    private var originalPost: Post? = null

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authService.getCurrentUserId()?.let { uid ->
                getUserByIdUseCase(uid)
                    .onSuccess { user ->
                        _uiState.update { it.copy(currentUserProfile = user) }
                    }
                    .onFailure { error ->
                        android.util.Log.e("CreatePostViewModel", "Failed to load current user: ${error.message}", error)
                    }
            }
        }
    }

    fun loadDraft() {
        if (_uiState.value.isEditMode || !_uiState.value.checkDraft) return

        val draftText = getDraftUseCase()

        if (!draftText.isNullOrEmpty()) {
             _uiState.update { it.copy(postText = draftText, checkDraft = false) }
        } else {
            _uiState.update { it.copy(checkDraft = false) }
        }
    }

    fun saveDraft() {
        if (_uiState.value.isPostCreated) return
        if (_uiState.value.isEditMode) return

        val text = _uiState.value.postText

        if (text.isNotBlank() || _uiState.value.mediaItems.isNotEmpty()) {
            saveDraftUseCase(text)
        }
    }

    fun clearDraft() {
        clearDraftUseCase()
    }

    fun setCompositionType(type: String) {
        val compositionType = try {
            CompositionType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            CompositionType.POST
        }
        _uiState.update { it.copy(compositionType = compositionType) }
    }

    fun setReplyToPostId(postId: String?) {
        _uiState.update { it.copy(replyToPostId = postId) }
    }

    fun loadPostForEdit(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true, checkDraft = false) }
            getPostByIdUseCase(postId).onSuccess { post ->
                post?.let {
                    originalPost = it
                    editPostId = it.id

                    val mediaItems = it.mediaItems?.toMutableList() ?: mutableListOf()

                    if (mediaItems.isEmpty()) {
                        it.postImage?.let { imgUrl ->
                             mediaItems.add(MediaItem(id = java.util.UUID.randomUUID().toString(), url = imgUrl, type = MediaType.IMAGE))
                        }
                    }

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            postText = it.postText ?: "",
                            mediaItems = mediaItems,
                            privacy = it.postVisibility ?: "public",
                            youtubeUrl = it.youtubeUrl,
                            settings = PostSettings(
                                hideViewsCount = it.postHideViewsCount == "true",
                                hideLikeCount = it.postHideLikeCount == "true",
                                hideCommentsCount = it.postHideReplyCount == "true",
                                disableComments = it.postDisableReplies == "true"
                            ),

                            pollData = if (it.hasPoll == true) PollData(it.pollQuestion ?: "", it.pollOptions?.map { opt -> opt.text } ?: emptyList(), 24) else null,
                            location = if (it.hasLocation == true) LocationData(it.locationName ?: "", it.locationAddress, it.locationLatitude, it.locationLongitude) else null,
                            taggedPeople = it.metadata?.taggedPeople ?: emptyList(),
                            feeling = it.metadata?.feeling,
                            textBackgroundColor = it.metadata?.backgroundColor
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                viewModelScope.launch { UiEventManager.emit(UiEvent.Error("Failed to load post for editing")) }
            }
        }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(postText = text) }
    }

    fun addThreadPost() {
        val currentThreads = _uiState.value.threadPosts.toMutableList()
        currentThreads.add(PostDraft())
        _uiState.update { it.copy(threadPosts = currentThreads) }
    }

    fun removeThreadPost(index: Int) {
        val currentThreads = _uiState.value.threadPosts.toMutableList()
        if (index in currentThreads.indices) {
            currentThreads.removeAt(index)
            _uiState.update { it.copy(threadPosts = currentThreads) }
        }
    }

    fun updateThreadPostText(index: Int, text: String) {
        val currentThreads = _uiState.value.threadPosts.toMutableList()
        if (index in currentThreads.indices) {
            currentThreads[index] = currentThreads[index].copy(postText = text)
            _uiState.update { it.copy(threadPosts = currentThreads) }
        }
    }

    fun addThreadMedia(index: Int, uris: List<Uri>) {
        val currentThreads = _uiState.value.threadPosts.toMutableList()
        if (index in currentThreads.indices) {
            val context = getApplication<Application>()
            val currentMedia = currentThreads[index].mediaItems.toMutableList()

            uris.forEach { uri ->
                 val mimeType = context.contentResolver.getType(uri) ?: return@forEach
                 val type = if (mimeType.startsWith("video")) MediaType.VIDEO else MediaType.IMAGE
                 UriUtils.getPathFromUri(context, uri)?.let { path ->
                     currentMedia.add(MediaItem(id = java.util.UUID.randomUUID().toString(), url = path, type = type))
                 }
            }

            currentThreads[index] = currentThreads[index].copy(mediaItems = currentMedia)
            _uiState.update { it.copy(threadPosts = currentThreads) }
        }
    }

    fun removeThreadMedia(postIndex: Int, mediaIndex: Int) {
        val currentThreads = _uiState.value.threadPosts.toMutableList()
        if (postIndex in currentThreads.indices) {
            val currentMedia = currentThreads[postIndex].mediaItems.toMutableList()
            if (mediaIndex in currentMedia.indices) {
                currentMedia.removeAt(mediaIndex)
                currentThreads[postIndex] = currentThreads[postIndex].copy(mediaItems = currentMedia)
                _uiState.update { it.copy(threadPosts = currentThreads) }
            }
        }
    }

    fun updateThreadMediaItem(postIndex: Int, mediaIndex: Int, uri: Uri) {
        val currentThreads = _uiState.value.threadPosts.toMutableList()
        if (postIndex in currentThreads.indices) {
            val currentMedia = currentThreads[postIndex].mediaItems.toMutableList()
            if (mediaIndex in currentMedia.indices) {
                val context = getApplication<Application>()
                UriUtils.getPathFromUri(context, uri)?.let { path ->
                    val oldItem = currentMedia[mediaIndex]
                    val newItem = oldItem.copy(
                        url = path,
                        mimeType = context.contentResolver.getType(uri)
                    )
                    currentMedia[mediaIndex] = newItem
                    currentThreads[postIndex] = currentThreads[postIndex].copy(mediaItems = currentMedia)
                    _uiState.update { it.copy(threadPosts = currentThreads) }
                }
            }
        }
    }


    fun addTaggedPerson(user: User) {
        val current = _uiState.value.taggedPeople.toMutableList()

        if (current.none { it.uid == user.uid }) {
            current.add(user)
            _uiState.update { it.copy(taggedPeople = current) }
        }
    }

    fun removeTaggedPerson(user: User) {
        val current = _uiState.value.taggedPeople.toMutableList()
        current.removeAll { it.uid == user.uid }
        _uiState.update { it.copy(taggedPeople = current) }
    }

    fun toggleTaggedPerson(user: User) {
        val current = _uiState.value.taggedPeople
        if (current.any { it.uid == user.uid }) {
            removeTaggedPerson(user)
        } else {
            addTaggedPerson(user)
        }
    }

    fun setFeelingActivity(feeling: FeelingActivity?) {
        _uiState.update { it.copy(feeling = feeling) }
    }

    fun setTextBackgroundColor(color: Long?) {
        _uiState.update { it.copy(textBackgroundColor = color) }
    }

    fun addMedia(uris: List<Uri>) {
        val currentMedia = _uiState.value.mediaItems.toMutableList()
        val context = getApplication<Application>()

        uris.forEach { uri ->
             android.util.Log.d("CreatePost", "Processing URI: $uri")
             val mimeType = context.contentResolver.getType(uri) ?: return@forEach
             val type = if (mimeType.startsWith("video")) MediaType.VIDEO else MediaType.IMAGE
             UriUtils.getPathFromUri(context, uri)?.let { path ->
                 android.util.Log.d("CreatePost", "Converted URI to path: $path")
                 currentMedia.add(MediaItem(id = java.util.UUID.randomUUID().toString(), url = path, type = type))
             } ?: run {
                 android.util.Log.e("CreatePost", "Failed to convert URI to path: $uri")
             }
        }
        _uiState.update { it.copy(mediaItems = currentMedia) }
    }

    fun removeMedia(index: Int) {
        val currentMedia = _uiState.value.mediaItems.toMutableList()
        if (index in currentMedia.indices) {
            currentMedia.removeAt(index)
            _uiState.update { it.copy(mediaItems = currentMedia) }
        }
    }

    fun updateMediaItem(index: Int, uri: Uri) {
        val currentMedia = _uiState.value.mediaItems.toMutableList()
        val context = getApplication<Application>()

        if (index in currentMedia.indices) {
            UriUtils.getPathFromUri(context, uri)?.let { path ->
                val oldItem = currentMedia[index]
                val newItem = oldItem.copy(
                    url = path,
                    mimeType = context.contentResolver.getType(uri)
                )
                currentMedia[index] = newItem
                _uiState.update { it.copy(mediaItems = currentMedia) }
            } ?: run {
                android.util.Log.e("CreatePost", "Failed to convert edited URI to path: $uri")
                viewModelScope.launch { UiEventManager.emit(UiEvent.Error("Failed to save edited image")) }
            }
        }
    }

    fun setPoll(pollData: PollData?) {
        _uiState.update { it.copy(pollData = pollData, mediaItems = emptyList()) }
    }

    fun setLocation(location: LocationData?) {
        _uiState.update { it.copy(location = location) }
    }

    fun setYoutubeUrl(url: String?) {
        _uiState.update { it.copy(youtubeUrl = url) }
    }

    fun setPrivacy(privacy: String) {
        _uiState.update { it.copy(privacy = privacy) }
    }

    fun updateSettings(settings: PostSettings) {
        _uiState.update { it.copy(settings = settings) }
    }

    private var userSearchJob: kotlinx.coroutines.Job? = null
    private var locationSearchJob: kotlinx.coroutines.Job? = null

    fun searchUsers(query: String) {
        userSearchJob?.cancel()
        userSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoading = true) }
            searchUsersForPostUseCase(query).collect { result ->
                result.onSuccess { users ->
                    _uiState.update { it.copy(userSearchResults = users, isSearchLoading = false) }
                }.onFailure { error ->
                    android.util.Log.e("CreatePost", "User search failed", error)
                    _uiState.update { it.copy(isSearchLoading = false) }
                    UiEventManager.emit(UiEvent.Error("User search failed."))
                }
            }
        }
    }

    fun searchLocations(query: String) {
        locationSearchJob?.cancel()
        locationSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoading = true) }
            searchLocationsUseCase(query).collect { result ->
                result.onSuccess { locations ->
                    _uiState.update { it.copy(locationSearchResults = locations, isSearchLoading = false) }
                }.onFailure { error ->
                    android.util.Log.e("CreatePost", "Location search failed", error)
                    _uiState.update { it.copy(isSearchLoading = false) }
                    UiEventManager.emit(UiEvent.Error("Location search failed."))
                }
            }
        }
    }

    fun searchFeelings(query: String) {
        val filtered = searchFeelingsUseCase(query, allFeelings)
        _uiState.update { it.copy(feelingSearchResults = filtered) }
    }

    fun clearSearchResults() {
        userSearchJob?.cancel()
        locationSearchJob?.cancel()
        _uiState.update { it.copy(
            userSearchResults = emptyList(),
            locationSearchResults = emptyList(),
            feelingSearchResults = allFeelings,
            isSearchLoading = false
        )}
    }

    fun submitPost() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            val currentUser = authService.getCurrentUser()
            if (currentUser == null) {
                UiEventManager.emit(UiEvent.Error("Not logged in"))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, uploadProgress = 0f) }

            val state = _uiState.value
            val request = com.synapse.social.studioasinc.domain.model.CreatePostRequest(
                postText = state.postText,
                mediaItems = state.mediaItems,
                privacy = state.privacy,
                pollQuestion = state.pollData?.question,
                pollOptions = state.pollData?.options,
                pollDurationHours = state.pollData?.durationHours ?: 24,
                location = state.location,
                taggedPeople = state.taggedPeople.map { it.uid },
                feeling = state.feeling?.text,
                textBackgroundColor = state.textBackgroundColor,
                youtubeUrl = state.youtubeUrl,
                hideViewsCount = state.settings.hideViewsCount,
                hideLikeCount = state.settings.hideLikeCount,
                hideCommentsCount = state.settings.hideCommentsCount,
                disableComments = state.settings.disableComments,
                isEditMode = state.isEditMode,
                editPostId = editPostId,
                replyToPostId = state.replyToPostId
            )

            if (state.isEditMode) {
                val result = submitPostUseCase(
                    requests = listOf(request),
                    currentUserId = currentUser.id,
                    onProgress = { progress ->
                        _uiState.update { it.copy(uploadProgress = progress) }
                    }
                )

                result.onSuccess { posts ->
                    val post = posts.firstOrNull()
                    clearDraft()
                    if (post != null) {
                        com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus.emit(
                            com.synapse.social.studioasinc.feature.shared.components.post.PostEvent.Updated(post)
                        )

                        // Handle new thread items added during edit (if any)
                        if (state.threadPosts.isNotEmpty()) {
                            val threadRequests = state.threadPosts.filter { it.postText.isNotBlank() || it.mediaItems.isNotEmpty() }.map { threadPost ->
                                com.synapse.social.studioasinc.domain.model.CreatePostRequest(
                                    postText = threadPost.postText,
                                    mediaItems = threadPost.mediaItems,
                                    privacy = state.privacy,
                                    hideViewsCount = state.settings.hideViewsCount,
                                    hideLikeCount = state.settings.hideLikeCount,
                                    hideCommentsCount = state.settings.hideCommentsCount,
                                    disableComments = state.settings.disableComments,
                                    replyToPostId = post.id // Simplification: they all reply to the edited post for now, or we'd need to chain them
                                )
                            }
                            if (threadRequests.isNotEmpty()) {
                                submitPostUseCase(threadRequests, currentUser.id) {}.onSuccess { threadPosts ->
                                    threadPosts.forEach { newPost ->
                                        com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus.emit(
                                            com.synapse.social.studioasinc.feature.shared.components.post.PostEvent.Created(newPost)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    UiEventManager.emit(UiEvent.Error("Failed: ${e.message}"))
                }
            } else {
                // Batch submission for new thread
                val allRequests = mutableListOf(request)
                state.threadPosts.forEach { threadPost ->
                    if (threadPost.postText.isNotBlank() || threadPost.mediaItems.isNotEmpty()) {
                        allRequests.add(com.synapse.social.studioasinc.domain.model.CreatePostRequest(
                            postText = threadPost.postText,
                            mediaItems = threadPost.mediaItems,
                            privacy = state.privacy,
                            hideViewsCount = state.settings.hideViewsCount,
                            hideLikeCount = state.settings.hideLikeCount,
                            hideCommentsCount = state.settings.hideCommentsCount,
                            disableComments = state.settings.disableComments
                        ))
                    }
                }

                val result = submitPostUseCase(
                    requests = allRequests,
                    currentUserId = currentUser.id,
                    onProgress = { progress ->
                        _uiState.update { it.copy(uploadProgress = progress) }
                    }
                )

                result.onSuccess { posts ->
                    clearDraft()
                    posts.forEach { post ->
                        com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus.emit(
                            com.synapse.social.studioasinc.feature.shared.components.post.PostEvent.Created(post)
                        )
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    UiEventManager.emit(UiEvent.Error("Failed: ${e.message}"))
                    return@launch
                }
            }

            if (_uiState.value.isLoading) {
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        isPostCreated = true,
                        postText = "",
                        mediaItems = emptyList(),
                        threadPosts = emptyList(),
                        pollData = null,
                        location = null,
                        youtubeUrl = null,
                        taggedPeople = emptyList(),
                        feeling = null,
                        textBackgroundColor = null
                    )
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_LAYOUT_TYPE = "COLUMNS"
    }
}
