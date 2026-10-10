package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.usecase.chat.BulkDeleteMessagesForMeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageForMeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.EditMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.PopulateMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.mergeMonotonic
import com.synapse.social.studioasinc.shared.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val paginationCursorLock = Any()
    private var _oldestLoadedPageMessage: Message? = null

    /** Oldest message returned by a regular history-page fetch, excluding quote-only lookups. */
    val oldestLoadedPageMessage: Message?
        get() = synchronized(paginationCursorLock) { _oldestLoadedPageMessage }

    /** Atomically records the earliest message returned by a normal history-page fetch. */
    fun recordFetchedPage(page: List<Message>) {
        val ordering = compareBy<Message>({ it.createdAt }, { it.id })
        val candidate = page.minWithOrNull(ordering) ?: return
        synchronized(paginationCursorLock) {
            val current = _oldestLoadedPageMessage
            if (current == null || ordering.compare(candidate, current) < 0) {
                _oldestLoadedPageMessage = candidate
            }
        }
    }

    /** Adds a fetched reply target to the timeline without moving the regular-page cursor. */
    fun addQuotedMessageToTimeline(message: Message) {
        _messages.update { current ->
            if (current.any { it.id == message.id }) current
            else (current + message).sortedBy { it.createdAt }
        }
    }

    suspend fun setMessages(messages: List<Message>) {
        val populated = populateMessageReactionsUseCase(messages)
        _messages.update { current ->
            val messageMap = current.associateBy { it.id }.toMutableMap()
            populated.forEach { freshMsg ->
                val existing = messageMap[freshMsg.id]
                val merged = if (existing != null) existing.mergeMonotonic(freshMsg) else freshMsg
                messageMap[freshMsg.id] = merged
            }
            messageMap.values.sortedBy { it.createdAt }
        }
        recordFetchedPage(messages)
    }

    fun clearMessages() {
        _messages.value = emptyList()
        synchronized(paginationCursorLock) { _oldestLoadedPageMessage = null }
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
            val tempId = UUID.randomUUID().toString()
            pendingTempIds.update { it + tempId }

            val nowStr = TimeProvider.nowInstant().toString()
            val newMessage = Message(
                id = tempId,
                chatId = chatId,
                senderId = currentUserIdProvider() ?: "",
                content = text,
                messageType = MessageType.TEXT,
                deliveryStatus = DeliveryStatus.SENDING,
                contentStatus = ContentStatus.ACTIVE,
                createdAt = nowStr,
                sentAt = nowStr,
                expiresAt = expiresAt,
                replyToId = replyToId
            )
            _messages.update { current ->
                (current + newMessage).distinctBy { it.id }.sortedBy { msg -> msg.createdAt }
            }

            sendMessageUseCase(
                chatId = chatId,
                content = text,
                messageType = "text",
                expiresAt = expiresAt,
                replyToId = replyToId
            ).onSuccess { actualMessage ->
                pendingTempIds.update { it - tempId }
                _messages.update { current ->
                    val hasTempMessage = current.any { it.id == tempId }
                    val hasActualMessage = current.any { it.id == actualMessage.id }
                    when {
                        hasTempMessage && !hasActualMessage -> {
                            current.updateById(tempId) { tempMsg ->
                                val updated = if (tempMsg.content !in ENCRYPTED_PLACEHOLDERS &&
                                    actualMessage.content in ENCRYPTED_PLACEHOLDERS) {
                                    actualMessage.copy(content = tempMsg.content)
                                } else {
                                    actualMessage
                                }
                                tempMsg.mergeMonotonic(updated)
                            }.sortedBy { msg -> msg.createdAt }
                        }
                        hasTempMessage && hasActualMessage -> {
                            current.filter { it.id != tempId }
                                .sortedBy { msg -> msg.createdAt }
                        }
                        else -> current
                    }
                }
            }.onFailure { e ->
                pendingTempIds.update { it - tempId }
                onError(e.message)
                _messages.update { current ->
                    current.updateById(tempId) { tempMsg ->
                        tempMsg.copy(
                            deliveryStatus = DeliveryStatus.FAILED,
                            failureReason = e.message
                        )
                    }
                }
            }
        }
    }

    fun retrySendMessage(
        message: Message,
        onError: (String?) -> Unit
    ) {
        if (message.deliveryStatus != DeliveryStatus.FAILED) return
        performSendMessage(
            chatId = message.chatId,
            text = message.content,
            expiresAt = message.expiresAt,
            replyToId = message.replyToId,
            onError = onError
        )
        _messages.update { current -> current.filter { it.id != message.id } }
    }

    fun saveEdit(
        message: Message,
        newContent: String,
        onError: (String?) -> Unit,
        onSuccess: () -> Unit
    ) {
        val messageId = message.id.takeIf { it.isNotBlank() } ?: return

        viewModelScope.launch {
            val nowStr = TimeProvider.nowInstant().toString()
            _messages.update { current ->
                current.updateById(messageId) {
                    it.copy(
                        content = newContent,
                        isEdited = true,
                        contentStatus = ContentStatus.EDITED,
                        editedAt = nowStr
                    )
                }
            }

            editMessageUseCase(messageId, newContent).onSuccess {
                onSuccess()
            }.onFailure { e ->
                onError(e.message)
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
            val nowStr = TimeProvider.nowInstant().toString()
            _messages.update { current ->
                current.updateById(messageId) {
                    it.copy(
                        isDeleted = true,
                        contentStatus = ContentStatus.DELETED,
                        deletedAt = nowStr
                    )
                }
            }
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
            _messages.update { current ->
                current.filter { it.id !in selectedIds }
            }

            bulkDeleteMessagesForMeUseCase(selectedIds).onFailure { e ->
                onError(e.message)
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
