package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AvatarViewModel @Inject constructor(
    private val editProfileRepository: EditProfileRepository
) : ViewModel() {

    private val _isRemoving = MutableStateFlow(false)
    val isRemoving: StateFlow<Boolean> = _isRemoving.asStateFlow()

    fun removeProfilePhoto(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isRemoving.value = true
            try {
                val userId = editProfileRepository.getCurrentUserId()
                if (userId != null) {
                    val updateData = mapOf("avatar" to "")
                    val result = editProfileRepository.updateProfile(userId, updateData)
                    result.fold(
                        onSuccess = { onSuccess() },
                        onFailure = { error -> onError(error.message ?: "Failed to remove profile photo") }
                    )
                } else {
                    onError("User not logged in")
                }
            } catch (e: Exception) {
                onError(e.message ?: "An unexpected error occurred")
            } finally {
                _isRemoving.value = false
            }
        }
    }
}
