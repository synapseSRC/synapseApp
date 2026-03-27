package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserMoreOptionsViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase
) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            getUserProfileUseCase(userId)
                .onSuccess { user ->
                    _userProfile.value = user
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to load user profile"
                }
            _isLoading.value = false
        }
    }
}
