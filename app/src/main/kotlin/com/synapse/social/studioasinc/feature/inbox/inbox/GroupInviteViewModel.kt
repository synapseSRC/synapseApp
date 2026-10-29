package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupInvitePreview
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinOutcome
import com.synapse.social.studioasinc.shared.domain.repository.GroupCreationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupInviteUiState(
    val isLoading: Boolean = false,
    val isJoining: Boolean = false,
    val preview: GroupInvitePreview? = null,
    val outcome: GroupJoinOutcome? = null,
    val error: Boolean = false
)

@HiltViewModel
class GroupInviteViewModel @Inject constructor(
    private val groupCreationRepository: GroupCreationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(GroupInviteUiState())
    val state: StateFlow<GroupInviteUiState> = _state.asStateFlow()

    fun loadInvite(code: String) {
        if (code.isBlank()) {
            _state.value = _state.value.copy(error = true, isLoading = false)
            return
        }
        viewModelScope.launch {
            _state.value = GroupInviteUiState(isLoading = true)
            groupCreationRepository.resolveGroupInvite(code).onSuccess { preview ->
                _state.value = _state.value.copy(isLoading = false, preview = preview, error = preview == null)
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = true)
            }
        }
    }

    fun requestToJoin(code: String) {
        if (_state.value.isJoining) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isJoining = true, error = false)
            groupCreationRepository.requestGroupJoinByInvite(code).onSuccess { outcome ->
                _state.value = _state.value.copy(isJoining = false, outcome = outcome)
            }.onFailure {
                _state.value = _state.value.copy(isJoining = false, error = true)
            }
        }
    }
}
