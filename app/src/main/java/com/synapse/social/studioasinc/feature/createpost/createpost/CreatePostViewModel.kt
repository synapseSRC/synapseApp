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
import com.synapse.social.studioasinc.feature.createpost.createpost.handlers.DraftHandler
import com.synapse.social.studioasinc.feature.createpost.createpost.handlers.PostSubmissionHandler
import com.synapse.social.studioasinc.feature.createpost.createpost.handlers.SearchHandler
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
    val checkDraft: Boolean = true,
    val currentUserProfile: User? = null,

    val taggedPeople: List<User> = emptyList(),
    val feeling: FeelingActivity? = null,
    val textBackgroundColor: Long? = null,

    val userSearchResults: List<User> = emptyList(),
    val locationSearchResults: List<LocationData> = emptyList(),
    val feelingSearchResults: List<FeelingActivity> = emptyList(),
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
    private val draftHandler: DraftHandler,
    private val searchHandler: SearchHandler,
    private val postSubmissionHandler: PostSubmissionHandler
) : AndroidViewModel(application) {

    private val authService = SupabaseAuthenticationService()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private var editPostId: String? = null
    private var originalPost: Post? = null

    private val allFeelings = listOf(
        // Moods
        FeelingActivity("😊", "Happy", FeelingType.MOOD),
        FeelingActivity("😇", "Blessed", FeelingType.MOOD),
        FeelingActivity("🤩", "Excited", FeelingType.MOOD),
        FeelingActivity("🙏", "Grateful", FeelingType.MOOD),
        FeelingActivity("🥰", "Loved", FeelingType.MOOD),
        FeelingActivity("😎", "Cool", FeelingType.MOOD),
        FeelingActivity("🥳", "Celebrating", FeelingType.MOOD),
        FeelingActivity("😴", "Tired", FeelingType.MOOD),
        FeelingActivity("😢", "Sad", FeelingType.MOOD),
        FeelingActivity("😡", "Angry", FeelingType.MOOD),
        FeelingActivity("🤔", "Thinking", FeelingType.MOOD),
        FeelingActivity("🤒", "Sick", FeelingType.MOOD),
        FeelingActivity("😴", "Sleepy", FeelingType.MOOD),
        FeelingActivity("😌", "Relaxed", FeelingType.MOOD),
        FeelingActivity("💪", "Motivated", FeelingType.MOOD),
        FeelingActivity("😜", "Silly", FeelingType.MOOD),
        FeelingActivity("😔", "Lonely", FeelingType.MOOD),
        FeelingActivity("😲", "Surprised", FeelingType.MOOD),
        FeelingActivity("🧘", "Peaceful", FeelingType.MOOD),
        FeelingActivity("😤", "Proud", FeelingType.MOOD),
        FeelingActivity("🥱", "Bored", FeelingType.MOOD),
        FeelingActivity("😟", "Worried", FeelingType.MOOD),
        FeelingActivity("😰", "Nervous", FeelingType.MOOD),
        
        // Activities
        FeelingActivity("✈️", "Traveling", FeelingType.ACTIVITY),
        FeelingActivity("🍴", "Eating", FeelingType.ACTIVITY),
        FeelingActivity("🍷", "Drinking", FeelingType.ACTIVITY),
        FeelingActivity("🎮", "Playing", FeelingType.ACTIVITY),
        FeelingActivity("📚", "Reading", FeelingType.ACTIVITY),
        FeelingActivity("📺", "Watching", FeelingType.ACTIVITY),
        FeelingActivity("🎧", "Listening to music", FeelingType.ACTIVITY),
        FeelingActivity("🎉", "Attending", FeelingType.ACTIVITY),
        FeelingActivity("🏋️", "Exercising", FeelingType.ACTIVITY),
        FeelingActivity("💼", "Working", FeelingType.ACTIVITY),
        FeelingActivity("📖", "Studying", FeelingType.ACTIVITY),
        FeelingActivity("🍳", "Cooking", FeelingType.ACTIVITY),
        FeelingActivity("🎨", "Painting", FeelingType.ACTIVITY),
        FeelingActivity("🏕️", "Camping", FeelingType.ACTIVITY),
        FeelingActivity("🏊", "Swimming", FeelingType.ACTIVITY),
        FeelingActivity("🥾", "Hiking", FeelingType.ACTIVITY),
        FeelingActivity("🛍️", "Shopping", FeelingType.ACTIVITY),
        FeelingActivity("💃", "Dancing", FeelingType.ACTIVITY),
        FeelingActivity("💻", "Coding", FeelingType.ACTIVITY),
        FeelingActivity("🤝", "Volunteering", FeelingType.ACTIVITY)
    )

    init {
        loadCurrentUser()
        _uiState.update { it.copy(feelingSearchResults = allFeelings) }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authService.getCurrentUserId()?.let { uid ->
                getUserByIdUseCase(uid).onSuccess { user ->
                    _uiState.update { it.copy(currentUserProfile = user) }
                }
            }
        }
    }

    fun loadDraft() {
        if (_uiState.value.isEditMode || !_uiState.value.checkDraft) return

        val draftText = draftHandler.getDraftText()

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
            draftHandler.saveDraftText(text)
        }
    }

    fun clearDraft() {
        draftHandler.clearDraft()
    }

    fun setCompositionType(type: String) {
        val compositionType = try {
            CompositionType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            CompositionType.POST
        }
        _uiState.update { it.copy(compositionType = compositionType) }
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
                                hideCommentsCount = it.postHideCommentsCount == "true",
                                disableComments = it.postDisableComments == "true"
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

    fun searchUsers(query: String) {
        searchHandler.searchUsers(
            scope = viewModelScope,
            query = query,
            onLoading = { _uiState.update { it.copy(isSearchLoading = true) } },
            onResult = { result ->
                result.onSuccess { users ->
                    _uiState.update { it.copy(userSearchResults = users, isSearchLoading = false) }
                }.onFailure { error ->
                    android.util.Log.e("CreatePost", "User search failed", error)
                    _uiState.update { it.copy(isSearchLoading = false) }
                    viewModelScope.launch { UiEventManager.emit(UiEvent.Error("User search failed.")) }
                }
            }
        )
    }

    fun searchLocations(query: String) {
        searchHandler.searchLocations(
            scope = viewModelScope,
            query = query,
            onLoading = { _uiState.update { it.copy(isSearchLoading = true) } },
            onResult = { result ->
                result.onSuccess { locations ->
                    _uiState.update { it.copy(locationSearchResults = locations, isSearchLoading = false) }
                }.onFailure { error ->
                    android.util.Log.e("CreatePost", "Location search failed", error)
                    _uiState.update { it.copy(isSearchLoading = false) }
                    viewModelScope.launch { UiEventManager.emit(UiEvent.Error("Location search failed.")) }
                }
            }
        )
    }

    fun searchFeelings(query: String) {
        val filtered = searchHandler.searchFeelings(query, allFeelings)
        _uiState.update { it.copy(feelingSearchResults = filtered) }
    }

    fun clearSearchResults() {
        searchHandler.cancelAll()
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

            val result = postSubmissionHandler.submitPost(
                state = _uiState.value,
                currentUserId = currentUser.id,
                originalPost = originalPost,
                editPostId = editPostId,
                onProgress = { progress ->
                     _uiState.update { it.copy(uploadProgress = progress) }
                }
            )

            result.onSuccess {
                clearDraft()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isPostCreated = true,
                        postText = "",
                        mediaItems = emptyList(),
                        pollData = null,
                        location = null,
                        youtubeUrl = null,
                        taggedPeople = emptyList(),
                        feeling = null,
                        textBackgroundColor = null
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false) }
                UiEventManager.emit(UiEvent.Error("Failed: ${e.message}"))
            }
        }
    }

    companion object {
        private const val DEFAULT_LAYOUT_TYPE = "COLUMNS"
    }
}
