package com.synapse.social.studioasinc.feature.shared.sharepost

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SharePostToChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharePostSheetUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val query: String = "",
    val sendingChatId: String? = null,
    val confirmation: String? = null,
    val error: String? = null
)

@HiltViewModel
class SharePostSheetViewModel @Inject constructor(
    private val application: Application,
    private val getConversationsUseCase: GetConversationsUseCase,
    private val sharePostToChatUseCase: SharePostToChatUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharePostSheetUiState())
    val uiState: StateFlow<SharePostSheetUiState> = _uiState.asStateFlow()

    private var post: Post? = null

    fun start(post: Post) {
        if (post.id == this.post?.id) return
        this.post = post
        loadConversations()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(confirmation = null, error = null) }
    }

    fun sendToChat(conversation: Conversation) {
        val post = post ?: return
        if (_uiState.value.sendingChatId != null) return

        _uiState.update { it.copy(sendingChatId = conversation.chatId) }
        viewModelScope.launch {
            val sharedPost = post.toSharedPostMetadata()
            sharePostToChatUseCase(
                chatId = conversation.chatId,
                sharedPost = sharedPost,
                previewText = application.getString(R.string.share_post_chat_preview, sharedPost.authorName)
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        sendingChatId = null,
                        confirmation = application.getString(R.string.shared_to_chat, conversation.participantName)
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        sendingChatId = null,
                        error = throwable.message ?: application.getString(R.string.share_to_chat_failed)
                    )
                }
            }
        }
    }

    private fun loadConversations() {
        if (_uiState.value.conversations.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getConversationsUseCase(forceNetwork = false)
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            conversations = list.distinctBy { conversation -> conversation.chatId }
                                .sortedWith(
                                    compareByDescending<Conversation> { it.isPinned }
                                        .thenByDescending { it.lastMessageTime ?: "" }
                                )
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: application.getString(R.string.failed_to_load_conversations)
                        )
                    }
                }
        }
    }
}
