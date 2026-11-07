package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.models.CreatePollUiState
import com.synapse.social.studioasinc.shared.domain.usecase.chat.CreatePollUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the poll creation form. All authoritative validation lives in [CreatePollUseCase]; this
 * ViewModel keeps the enabled-state mirror in [CreatePollUiState.canSend] in sync and surfaces
 * failures as string resources so the UI never hardcodes copy.
 */
@HiltViewModel
class CreatePollViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createPollUseCase: CreatePollUseCase
) : ViewModel() {

    // Type-safe navigation route args are stored under the argument name.
    private val chatId: String = savedStateHandle[KEY_CHAT_ID] ?: ""

    private val _uiState = MutableStateFlow(CreatePollUiState())
    val uiState: StateFlow<CreatePollUiState> = _uiState.asStateFlow()

    fun updateQuestion(value: String) {
        _uiState.update {
            it.copy(
                question = value.take(CreatePollUiState.MAX_QUESTION_LENGTH),
                errorRes = null
            )
        }
    }

    fun updateOption(index: Int, text: String) {
        _uiState.update { state ->
            val options = state.options.toMutableList()
            if (index in options.indices) {
                options[index] = text.take(CreatePollUiState.MAX_OPTION_LENGTH)
            }
            state.copy(options = options, errorRes = null)
        }
    }

    fun addOption() {
        _uiState.update { state ->
            if (state.options.size >= CreatePollUiState.MAX_OPTIONS) state
            else state.copy(options = state.options + "")
        }
    }

    fun removeOption(index: Int) {
        _uiState.update { state ->
            if (state.options.size <= CreatePollUiState.MIN_OPTIONS || index !in state.options.indices) {
                state
            } else {
                state.copy(options = state.options.filterIndexed { i, _ -> i != index })
            }
        }
    }

    fun toggleAllowMultiple() {
        _uiState.update { it.copy(allowMultipleAnswers = !it.allowMultipleAnswers) }
    }

    fun sendPoll() {
        val state = _uiState.value
        if (state.isSending) return

        val question = state.question.trim()
        val options = state.filledOptions

        if (question.isBlank()) {
            _uiState.update { it.copy(errorRes = R.string.create_poll_error_question_blank) }
            return
        }
        if (options.size < CreatePollUiState.MIN_OPTIONS) {
            _uiState.update { it.copy(errorRes = R.string.create_poll_error_options_min) }
            return
        }
        if (options.distinctBy { it.lowercase() }.size != options.size) {
            _uiState.update { it.copy(errorRes = R.string.create_poll_error_options_duplicate) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorRes = null) }
            createPollUseCase(
                chatId = chatId,
                question = question,
                options = options,
                allowMultipleAnswers = state.allowMultipleAnswers
            ).onSuccess {
                _uiState.update { it.copy(isSending = false, sendSuccess = true) }
            }.onFailure {
                _uiState.update { it.copy(isSending = false, errorRes = R.string.create_poll_error_send_failed) }
            }
        }
    }

    private companion object {
        const val KEY_CHAT_ID = "chatId"
    }
}
