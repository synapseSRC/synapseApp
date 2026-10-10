package com.synapse.social.studioasinc.ui.createpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.domain.model.FeelingType
import com.synapse.social.studioasinc.domain.model.LocationData
import com.synapse.social.studioasinc.domain.model.User
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

data class CreatePostSearchUiState(
    val userSearchResults: List<User> = emptyList(),
    val locationSearchResults: List<LocationData> = emptyList(),
    val feelingSearchResults: List<FeelingActivity> = emptyList(),
    val isSearchLoading: Boolean = false
)

@HiltViewModel
class CreatePostSearchViewModel @Inject constructor(
    private val searchHandler: SearchHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostSearchUiState())
    val uiState: StateFlow<CreatePostSearchUiState> = _uiState.asStateFlow()

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
        _uiState.update { it.copy(feelingSearchResults = allFeelings) }
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
                    android.util.Log.e("CreatePostSearch", "User search failed", error)
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
                    android.util.Log.e("CreatePostSearch", "Location search failed", error)
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
}
