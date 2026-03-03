package com.synapse.social.studioasinc.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.domain.model.AccountInfo
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountInfoViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountInfoState>(AccountInfoState.Loading)
    val uiState: StateFlow<AccountInfoState> = _uiState.asStateFlow()

    fun loadAccountInfo() {
        viewModelScope.launch {
            _uiState.value = AccountInfoState.Loading

            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.value = AccountInfoState.Error("User not logged in")
                return@launch
            }

            userRepository.getAccountInfo(userId)
                .onSuccess { accountInfo ->
                    _uiState.value = AccountInfoState.Success(accountInfo)
                }
                .onFailure { error ->
                     _uiState.value = AccountInfoState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun copyUserId(context: Context, userId: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("User ID", userId)
        clipboard.setPrimaryClip(clip)
    }
}

sealed class AccountInfoState {
    object Loading : AccountInfoState()
    data class Success(val data: AccountInfo) : AccountInfoState()
    data class Error(val message: String) : AccountInfoState()
}
