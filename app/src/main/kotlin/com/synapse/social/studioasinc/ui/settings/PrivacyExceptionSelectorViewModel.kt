package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyExceptionSelectorUiState(
    val settingKey: String = "",
    val contacts: List<User> = emptyList(),
    val filteredContacts: List<User> = emptyList(),
    val selectedUserIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PrivacyExceptionSelectorViewModel @Inject constructor(
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyExceptionSelectorUiState())
    val uiState: StateFlow<PrivacyExceptionSelectorUiState> = _uiState.asStateFlow()

    fun initialize(settingKey: String) {
        _uiState.value = _uiState.value.copy(settingKey = settingKey, isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId() ?: ""
                val followers = getFollowersUseCase(currentUserId).getOrDefault(emptyList())
                val following = getFollowingUseCase(currentUserId).getOrDefault(emptyList())

                // Distinct population of followers and following contacts
                val combined = (followers + following).distinctBy { it.uid }

                val initialSelectedIds: Set<String> = when (settingKey) {
                    "last_seen" -> settingsRepository.lastSeenExcludedUserIds.first()
                    "profile_photo" -> settingsRepository.profilePhotoExcludedUserIds.first()
                    "about" -> settingsRepository.aboutExcludedUserIds.first()
                    "status" -> settingsRepository.statusExcludedUserIds.first()
                    "group" -> settingsRepository.groupExcludedUserIds.first()
                    else -> emptySet()
                }

                _uiState.value = _uiState.value.copy(
                    contacts = combined,
                    filteredContacts = combined,
                    selectedUserIds = initialSelectedIds,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load contacts: ${e.message}"
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val contacts = _uiState.value.contacts
        val filtered = if (query.isBlank()) {
            contacts
        } else {
            contacts.filter { user ->
                val name = user.displayName ?: user.username ?: ""
                name.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(searchQuery = query, filteredContacts = filtered)
    }

    fun toggleUserSelection(userId: String) {
        val current = _uiState.value.selectedUserIds.toMutableSet()
        if (current.contains(userId)) {
            current.remove(userId)
        } else {
            current.add(userId)
        }
        _uiState.value = _uiState.value.copy(selectedUserIds = current)
    }

    fun saveSelection(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val selected = _uiState.value.selectedUserIds
                when (_uiState.value.settingKey) {
                    "last_seen" -> settingsRepository.setLastSeenExcludedUserIds(selected)
                    "profile_photo" -> settingsRepository.setProfilePhotoExcludedUserIds(selected)
                    "about" -> settingsRepository.setAboutExcludedUserIds(selected)
                    "status" -> settingsRepository.setStatusExcludedUserIds(selected)
                    "group" -> settingsRepository.setGroupExcludedUserIds(selected)
                }
                _uiState.value = _uiState.value.copy(isSaving = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save selection: ${e.message}"
                )
            }
        }
    }
}
