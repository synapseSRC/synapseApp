package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.ChatEffectAnimation
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.MessageEffect
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect
import com.synapse.social.studioasinc.shared.domain.usecase.chat.effects.CreateWordEffectUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.effects.DeleteWordEffectUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.effects.GetWordEffectsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.effects.ResolveMessageEffectsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.effects.UpdateWordEffectUseCase
import com.synapse.social.studioasinc.shared.domain.util.WordEffectError
import com.synapse.social.studioasinc.shared.domain.util.WordEffectValidationException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WordEffectsUiState(
    val isLoading: Boolean = true,
    val effects: List<WordEffect> = emptyList(),
    val isSaving: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
    val canRetry: Boolean = false,
    @StringRes val editorErrorRes: Int? = null,
    val previewEffects: List<MessageEffect> = emptyList(),
    /** Increments after every successful write, so the editor knows when to close. */
    val savedCount: Int = 0
)

/**
 * State holder for the Word effects screen.
 *
 * It owns no matching or validation of its own: the use cases decide what is acceptable and what a
 * message resolves to, and this class only turns their Result into screen state.
 */
@HiltViewModel
class WordEffectsViewModel @Inject constructor(
    private val getWordEffectsUseCase: GetWordEffectsUseCase,
    private val createWordEffectUseCase: CreateWordEffectUseCase,
    private val updateWordEffectUseCase: UpdateWordEffectUseCase,
    private val deleteWordEffectUseCase: DeleteWordEffectUseCase,
    private val resolveMessageEffectsUseCase: ResolveMessageEffectsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordEffectsUiState())
    val uiState: StateFlow<WordEffectsUiState> = _uiState.asStateFlow()

    private var initializedChatId: String? = null
    private var previewText: String = ""

    fun loadEffects(chatId: String, force: Boolean = false) {
        if (!force && initializedChatId == chatId) return
        initializedChatId = chatId
        _uiState.update { it.copy(isLoading = true, canRetry = false, errorMessageRes = null) }

        viewModelScope.launch {
            getWordEffectsUseCase(chatId)
                .onSuccess { effects ->
                    _uiState.update {
                        it.copy(isLoading = false, effects = effects, canRetry = false, errorMessageRes = null)
                    }
                    resolvePreview()
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            canRetry = true,
                            errorMessageRes = R.string.word_effects_error_load
                        )
                    }
                }
        }
    }

    fun retryLoad(chatId: String) = loadEffects(chatId, force = true)

    fun addEffect(chatId: String, trigger: String, emoji: String, animation: ChatEffectAnimation) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, editorErrorRes = null) }

        viewModelScope.launch {
            createWordEffectUseCase(chatId, trigger, emoji, animation)
                .onSuccess { created ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            effects = (it.effects + created).sortedBy { effect -> effect.trigger },
                            editorErrorRes = null,
                            savedCount = it.savedCount + 1
                        )
                    }
                    resolvePreview()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, editorErrorRes = editorMessage(error)) }
                }
        }
    }

    fun updateEffect(
        chatId: String,
        effect: WordEffect,
        trigger: String,
        emoji: String,
        animation: ChatEffectAnimation
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, editorErrorRes = null) }

        viewModelScope.launch {
            updateWordEffectUseCase(effect.copy(trigger = trigger, emoji = emoji, animation = animation))
                .onSuccess { saved ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            effects = state.effects
                                .map { if (it.id == saved.id) saved else it }
                                .sortedBy { stored -> stored.trigger },
                            editorErrorRes = null,
                            savedCount = state.savedCount + 1
                        )
                    }
                    resolvePreview()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, editorErrorRes = editorMessage(error)) }
                }
        }
    }

    /**
     * Flips one effect on or off.
     *
     * The switch moves first and the backend follows, which is what makes the row feel instant; if
     * the write fails the previous value comes back so the screen never shows a state the backend
     * does not have.
     */
    fun setEffectEnabled(effect: WordEffect, isEnabled: Boolean) {
        val optimistic = effect.copy(isEnabled = isEnabled)
        replaceEffect(optimistic)

        viewModelScope.launch {
            updateWordEffectUseCase(optimistic)
                .onSuccess { saved -> replaceEffect(saved) }
                .onFailure {
                    replaceEffect(effect)
                    _uiState.update { it.copy(errorMessageRes = R.string.word_effects_error_save) }
                }
            resolvePreview()
        }
    }

    fun deleteEffect(chatId: String, effectId: String) {
        viewModelScope.launch {
            deleteWordEffectUseCase(chatId, effectId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(effects = state.effects.filterNot { it.id == effectId })
                    }
                    resolvePreview()
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessageRes = R.string.word_effects_error_delete) }
                }
        }
    }

    fun clearEditorError() {
        _uiState.update { it.copy(editorErrorRes = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessageRes = null) }
    }

    /** Feeds the sample line in the preview card. */
    fun updatePreviewText(text: String) {
        previewText = text
        resolvePreview()
    }

    private fun replaceEffect(effect: WordEffect) {
        _uiState.update { state ->
            state.copy(effects = state.effects.map { if (it.id == effect.id) effect else it })
        }
    }

    private fun resolvePreview() {
        val effects = _uiState.value.effects
        viewModelScope.launch {
            resolveMessageEffectsUseCase(previewText, effects)
                .onSuccess { resolved -> _uiState.update { it.copy(previewEffects = resolved) } }
        }
    }

    private fun editorMessage(error: Throwable): Int =
        when ((error as? WordEffectValidationException)?.error) {
            WordEffectError.BLANK_TRIGGER -> R.string.word_effects_error_trigger_blank
            WordEffectError.TRIGGER_TOO_LONG -> R.string.word_effects_error_trigger_too_long
            WordEffectError.TRIGGER_HAS_CONTROL_CHARACTERS -> R.string.word_effects_error_trigger_invalid
            WordEffectError.BLANK_EMOJI -> R.string.word_effects_error_emoji_blank
            WordEffectError.EMOJI_TOO_LONG -> R.string.word_effects_error_emoji_too_long
            WordEffectError.DUPLICATE_TRIGGER -> R.string.word_effects_error_trigger_duplicate
            null -> R.string.word_effects_error_save
        }
}
