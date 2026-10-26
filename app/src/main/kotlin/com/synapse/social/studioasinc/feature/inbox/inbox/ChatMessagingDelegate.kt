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
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

    private val sendMutexes = ConcurrentHashMap<String, Mutex>()

    private val paginationCursorLock = Any()
    private var _oldestLoadedPageMessage: Message? = null

    /** Oldest message returned by a regular history-page fetch, excluding quote-only lookups. */
    val oldestLoadedPageMessage: Message?
        get() = synchronized(paginationCursorLock) { _oldestLoadedPageMessage }

    /** Atomically records the earliest message returned by a normal history-page fetch. */
    fun recordFetchedPage(page: List<Message>) {
        val ordering = ChatMapper.MessageChronologicalComparator
        val candidate = page.minWithOrNull(ordering) ?: return
        synchronized(paginationCursorLock) {
            val current = _oldestLoadedPageMessage
            if (current == null || ordering.compare(candidate, current) < 0) {
                _oldestLoadedPageMessage = candidate
            }
        }
    }

    fun insertChronologically(current: List<Message>, message: Message): List<Message> {
        val ordering = ChatMapper.MessageChronologicalComparator
        val last = current.lastOrNull()
        if (last == null || ordering.compare(last, message) <= 0) return current + message

        var low = 0
        var high = current.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (ordering.compare(current[middle], message) <= 0) low = middle + 1 else high = middle
        }
        return current.toMutableList().apply { add(low, message) }
    }

    fun replaceChronologically(current: List<Message>, targetId: String, message: Message): List<Message> {
        val index = current.indexOfFirst { it.id == targetId }
        if (index < 0) return insertChronologically(current, message)
        val ordering = ChatMapper.MessageChronologicalComparator
        val previous = current.getOrNull(index - 1)
        val next = current.getOrNull(index + 1)
        if ((previous == null || ordering.compare(previous, message) <= 0) &&
            (next == null || ordering.compare(message, next) <= 0)
        ) {
            return current.toMutableList().apply { set(index, message) }
        }
        return insertChronologically(current.toMutableList().apply { removeAt(index) }, message)
    }

    fun mergeChronologically(current: List<Message>, incoming: List<Message>): List<Message> {
        if (incoming.isEmpty()) return current
        val ordering = ChatMapper.MessageChronologicalComparator

        val currentByClientId = current.filter { it.clientId.isNotBlank() }.associateBy { it.clientId }

        val mergedMap = LinkedHashMap<String, Message>()
        current.forEach { msg -> mergedMap[msg.id] = msg }

        incoming.forEach { fresh ->
            val existingById = mergedMap[fresh.id]
            val existingByClient = if (fresh.clientId.isNotBlank()) currentByClientId[fresh.clientId] else null
            val existing = existingById ?: existingByClient

            if (existing != null) {
                val merged = existing.mergeMonotonic(fresh)
                if (existing.id != fresh.id) {
                    mergedMap.remove(existing.id)
                }
                mergedMap[merged.id] = merged
            } else {
                mergedMap[fresh.id] = fresh
            }
        }

        return mergedMap.values.sortedWith(ordering)
    }

    /** Adds a fetched reply target to the timeline without moving the regular-page cursor. */
    fun addQuotedMessageToTimeline(message: Message) {
        _messages.update { current ->
            if (current.any { it.id == message.id }) current
            else insertChronologically(current, message)
        }
    }

    suspend fun setMessages(messages: List<Message>) {
        val populated = populateMessageReactionsUseCase(messages)
        _messages.update { current -> mergeChronologically(current, populated) }
        recordFetchedPage(messages)
    }

    fun clearMessages() {
        pendingTempIds.value = emptySet()
        _messages.value = emptyList()
        synchronized(paginationCursorLock) { _oldestLoadedPageMessage = null }
    }

    fun markAllIncomingAsRead(currentUserId: String) {
        _messages.update { current ->
            var changed = false
            val updated = current.map { msg ->
                if (msg.senderId != currentUserId && currentUserId !in msg.readBy) {
                    changed = true
                    msg.copy(readBy = (msg.readBy + currentUserId).distinct())
                } else {
                    msg
                }
            }
            if (changed) updated else current
        }
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

            val latestTimestamp = _messages.value.lastOrNull()?.createdAt
            val now = TimeProvider.nowInstant()
            val nowInstant = if (latestTimestamp != null) {
                try {
                    val parsedLatest = kotlinx.datetime.Instant.parse(latestTimestamp)
                    if (now <= parsedLatest) {
                        kotlinx.datetime.Instant.fromEpochMilliseconds(parsedLatest.toEpochMilliseconds() + 1)
                    } else {
                        now
                    }
                } catch (_: Exception) { now }
            } else {
                now
            }
            val nowStr = nowInstant.toString()

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
                replyToId = replyToId,
                clientId = tempId
            )
            _messages.update { current ->
                insertChronologically(current, newMessage)
            }

            val sendMutex = sendMutexes.computeIfAbsent(chatId) { Mutex() }
            val sendResult: Result<Message> = try {
                kotlinx.coroutines.withTimeout(30_000L) {
                    sendMutex.withLock {
                        sendMessageUseCase(
                            chatId = chatId,
                            content = text,
                            messageType = "text",
                            expiresAt = expiresAt,
                            replyToId = replyToId
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }

            sendResult.onSuccess { actualMessage ->
                completeOptimisticSend(tempId, actualMessage)
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

    fun completeOptimisticSend(tempId: String, actualMessage: Message) {
        pendingTempIds.update { it - tempId }
        val confirmedMessage = if (actualMessage.deliveryStatus == DeliveryStatus.SENDING) {
            actualMessage.copy(deliveryStatus = DeliveryStatus.SENT)
        } else {
            actualMessage
        }

        _messages.update { current ->
            val optimisticMessage = current.firstOrNull { it.id == tempId }
            val existingServerMessage = current.firstOrNull { it.id == confirmedMessage.id }
            val sourceMessage = optimisticMessage ?: existingServerMessage ?: return@update current
            val stableClientId = sourceMessage.clientId
            val merged = sourceMessage.mergeMonotonic(confirmedMessage.copy(clientId = stableClientId))
            val base = if (optimisticMessage != null && existingServerMessage != null && optimisticMessage.id != existingServerMessage.id) {
                current.filterNot { it.id == tempId }
            } else {
                current
            }
            val targetId = existingServerMessage?.id ?: optimisticMessage?.id ?: confirmedMessage.id
            replaceChronologically(base, targetId, merged)
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
