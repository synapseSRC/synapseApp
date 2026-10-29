package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.ChatVerificationException
import com.synapse.social.studioasinc.domain.model.ChatVerificationFailureReason
import com.synapse.social.studioasinc.domain.model.ChatVerificationFeedback
import com.synapse.social.studioasinc.domain.model.ChatVerificationInfo
import com.synapse.social.studioasinc.domain.usecase.chat.LoadChatVerificationUseCase
import com.synapse.social.studioasinc.domain.usecase.chat.SetChatVerificationUseCase
import com.synapse.social.studioasinc.domain.usecase.chat.VerifyScannedChatCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatEncryptionUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val info: ChatVerificationInfo? = null,
    val failure: ChatVerificationFailureReason? = null,
    val feedback: ChatVerificationFeedback = ChatVerificationFeedback.NONE
)

@HiltViewModel
class ChatEncryptionViewModel @Inject constructor(
    private val loadChatVerification: LoadChatVerificationUseCase,
    private val setChatVerification: SetChatVerificationUseCase,
    private val verifyScannedChatCode: VerifyScannedChatCodeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatEncryptionUiState())
    val uiState: StateFlow<ChatEncryptionUiState> = _uiState.asStateFlow()

    fun load(chatId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null, feedback = ChatVerificationFeedback.NONE) }
            loadChatVerification(chatId)
                .onSuccess { info -> _uiState.update { it.copy(isLoading = false, info = info, failure = null) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, info = null, failure = (error as? ChatVerificationException)?.reason ?: ChatVerificationFailureReason.GENERAL) }
                }
        }
    }

    fun setVerified(verified: Boolean) {
        val current = _uiState.value.info ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, failure = null, feedback = ChatVerificationFeedback.NONE) }
            setChatVerification(current, verified)
                .onSuccess {
                    _uiState.update { it.copy(isActionInProgress = false, info = current.copy(isVerified = verified), feedback = if (verified) ChatVerificationFeedback.MANUALLY_VERIFIED else ChatVerificationFeedback.VERIFICATION_CLEARED) }
                }
                .onFailure { error -> _uiState.update { it.copy(isActionInProgress = false, failure = (error as? ChatVerificationException)?.reason ?: ChatVerificationFailureReason.GENERAL) } }
        }
    }

    fun verifyScannedCode(code: String) {
        val current = _uiState.value.info ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, failure = null, feedback = ChatVerificationFeedback.NONE) }
            verifyScannedChatCode(current, code)
                .onSuccess { matches ->
                    _uiState.update { it.copy(isActionInProgress = false, info = if (matches) current.copy(isVerified = true) else current, feedback = if (matches) ChatVerificationFeedback.CODE_MATCHED else ChatVerificationFeedback.CODE_MISMATCH) }
                }
                .onFailure { error -> _uiState.update { it.copy(isActionInProgress = false, failure = (error as? ChatVerificationException)?.reason ?: ChatVerificationFailureReason.GENERAL) } }
        }
    }

    fun reportScannerUnavailable() {
        _uiState.update { it.copy(feedback = ChatVerificationFeedback.SCANNER_UNAVAILABLE) }
    }
}
