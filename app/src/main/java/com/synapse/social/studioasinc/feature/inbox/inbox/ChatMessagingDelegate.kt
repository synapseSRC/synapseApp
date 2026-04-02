package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.usecase.chat.BulkDeleteMessagesForMeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageForMeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.EditMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.PopulateMessageReactionsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class ChatMessagingDelegate(
    private val sendMessageUseCase: SendMessageUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val deleteMessageForMeUseCase: DeleteMessageForMeUseCase,
    private val bulkDeleteMessagesForMeUseCase: BulkDeleteMessagesForMeUseCase,
    private val populateMessageReactionsUseCase: PopulateMessageReactionsUseCase,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?
) {

    val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    val chatItems: StateFlow<List<ChatListItem>> = _messages
        .map { ChatItemsMapper.buildChatItems(it, currentUserIdProvider() ?: "") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTempIds = MutableStateFlow<Set<String>>(emptySet())

    suspend fun setMessages(messages: List<Message>) {
        val populated = populateMessageReactionsUseCase(messages)
        _messages.value = populated.distinctBy { it.id }.sortedBy { it.createdAt } // oldest first for UI
    }

    fun splitIntoChunks(text: String, chunkSize: Int): List<String> {
        if (text.length <= chunkSize) return listOf(text)
        return text.chunked(chunkSize)
    }

    fun performSendMessage(
        chatId: String,
        text: String,
        expiresAt: String?,
        replyToId: String?,
        onError: (String?) -> Unit
    ) {
        viewModelScope.launch {
            // Optimistic update
            val tempId = UUID.randomUUID().toString()
            // Register this temp ID so the realtime handler knows to replace it
            pendingTempIds.update { it + tempId }

            val newMessage = Message(
                id = tempId,
                chatId = chatId,
                senderId = currentUserIdProvider() ?: "",
                content = text,
                messageType = MessageType.TEXT,
                deliveryStatus = DeliveryStatus.SENT,
                createdAt = Instant.now().toString(),
                expiresAt = expiresAt,
                replyToId = replyToId
            )
            _messages.update { current ->
                (current + newMessage).distinctBy { it.id }.sortedBy { msg -> msg.createdAt }
            }

            // Actual send
            sendMessageUseCase(
                chatId = chatId,
                content = text,
                messageType = "text",
                expiresAt = expiresAt,
                replyToId = replyToId
            ).onSuccess { actualMessage ->
                // Only replace if the temp message hasn't already been swapped by the realtime handler
                pendingTempIds.update { it - tempId }
                _messages.update { current ->
                    val hasTempMessage = current.any { it.id == tempId }
                    val hasActualMessage = current.any { it.id == actualMessage.id }
                    when {
                        hasTempMessage && !hasActualMessage -> {
                            // Normal case: realtime hasn't arrived yet, replace temp with actual
                            current.updateById(tempId) { tempMsg ->
                                if (tempMsg.content !in ENCRYPTED_PLACEHOLDERS &&
                                    actualMessage.content in ENCRYPTED_PLACEHOLDERS) {
                                    actualMessage.copy(content = tempMsg.content)
                                } else {
                                    actualMessage
                                }
                            }.sortedBy { msg -> msg.createdAt }
                        }
                        hasTempMessage && hasActualMessage -> {
                            // Realtime arrived but didn't match to our temp — remove duplicate temp
                            current.filter { it.id != tempId }
                                .sortedBy { msg -> msg.createdAt }
                        }
                        else -> {
                            // Realtime already replaced the temp message, nothing to do
                            current
                        }
                    }
                }
                // Notification is sent by the repository
            }.onFailure { e ->
                pendingTempIds.update { it - tempId }
                onError(e.message)
                // Remove optimistic message on failure
                _messages.update { current -> current.filter { it.id != tempId } }
            }
        }
    }

    fun saveEdit(
        message: Message,
        newContent: String,
        onError: (String?) -> Unit,
        onSuccess: () -> Unit
    ) {
        val messageId = message.id ?: return

        viewModelScope.launch {
            // Optimistic update
            _messages.update { current ->
                current.updateById(messageId) {
                    it.copy(content = newContent, isEdited = true)
                }
            }

            editMessageUseCase(messageId, newContent).onSuccess {
                onSuccess()
                // Notification handled by repository if needed
            }.onFailure { e ->
                onError(e.message)
                // Revert optimistic update
                _messages.update { current ->
                    current.replaceById(messageId, message)
                }
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            editMessageUseCase(messageId, newContent)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            deleteMessageUseCase(messageId)
        }
    }

    fun deleteMessageForMe(messageId: String) {
        viewModelScope.launch {
            deleteMessageForMeUseCase(messageId)
            _messages.update { current -> current.filter { it.id != messageId } }
        }
    }

    fun deleteSelectedMessages(
        selectedIds: List<String>,
        onError: (String?) -> Unit,
        onRequiresRefresh: () -> Unit
    ) {
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            // Optimistic UI update
            _messages.update { current ->
                current.filter { it.id !in selectedIds }
            }

            bulkDeleteMessagesForMeUseCase(selectedIds).onFailure { e ->
                onError(e.message)
                // Note: Reverting optimistic update for multiple messages is complex
                // because we don't know which ones failed. For simplicity, we just show an error.
                // A better approach would be to refresh the whole message list.
                onRequiresRefresh()
            }
        }
    }

    fun List<Message>.replaceById(id: String, newMessage: Message): List<Message> {
        val index = indexOfFirst { it.id == id }
        return if (index != -1) {
            toMutableList().apply { set(index, newMessage) }
        } else {
            this
        }
    }

    fun List<Message>.updateById(id: String, transform: (Message) -> Message): List<Message> {
        val index = indexOfFirst { it.id == id }
        return if (index != -1) {
            toMutableList().apply { set(index, transform(get(index))) }
        } else {
            this
        }
    }

    companion object {
        val ENCRYPTED_PLACEHOLDERS = setOf(
            "Message is encrypted",
            "🔒 Encrypted message",
            "🔒 You sent an encrypted message",
            "🔒 You sent an encrypted message (Copy)"
        )
    }
}
