package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeNumberViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    fun updatePhoneNumberInput(input: String) {
        _phoneNumber.value = input
    }

    fun clearError() {
        _error.value = null
    }

    fun updatePhoneNumber() {
        val phone = _phoneNumber.value
        if (phone.isBlank()) {
            _error.value = "Phone number cannot be empty"
            return
        }
        // Basic validation
        if (phone.length < 8) {
            _error.value = "Phone number is too short"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isSuccess.value = false

            val result = authRepository.updatePhoneNumber(phone)

            if (result.isSuccess) {
                _isSuccess.value = true
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to update phone number"
            }

            _isLoading.value = false
        }
    }
}
