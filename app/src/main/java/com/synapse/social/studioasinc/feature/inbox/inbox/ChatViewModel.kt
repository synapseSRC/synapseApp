package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.usecase.chat.*
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
//    private val getMessagesUseCase: GetMessagesUseCase,
//    private val sendMessageUseCase: SendMessageUseCase,
//    private val subscribeToMessagesUseCase: SubscribeToMessagesUseCase,
//    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
//    private val broadcastTypingStatusUseCase: BroadcastTypingStatusUseCase,
//    private val subscribeToTypingStatusUseCase: SubscribeToTypingStatusUseCase,
//    private val editMessageUseCase: EditMessageUseCase,
//    private val deleteMessageUseCase: DeleteMessageUseCase,
//    private val uploadMediaUseCase: UploadMediaUseCase,
//    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _participantProfile = MutableStateFlow<User?>(null)
    val participantProfile: StateFlow<User?> = _participantProfile.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _typingStatus = MutableStateFlow<TypingStatus?>(null)
    val typingStatus: StateFlow<TypingStatus?> = _typingStatus.asStateFlow()

    private var currentChatId: String? = null
    private var messageSubscriptionJob: Job? = null
    private var typingSubscriptionJob: Job? = null
    private var typingDebounceJob: Job? = null

    val currentUserId: String?
        get() = "me"

    fun initialize(chatId: String, participantId: String? = null) {
        if (currentChatId == chatId) return
        
        cleanup()
        currentChatId = chatId

        // Mock initialization
        _participantProfile.value = User(
            id = "alice",
            uid = "alice",
            username = "alice",
            displayName = "Alice",
            email = "alice@example.com",
            avatar = "https://i.pravatar.cc/150?u=alice"
        )
        _messages.value = listOf(
            Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = "alice",
                content = "Hi there! I'm Alice.",
                messageType = MessageType.TEXT,
                deliveryStatus = DeliveryStatus.READ,
                createdAt = Instant.now().minusSeconds(3600).toString()
            ),
            Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = "me",
                content = "Hello Alice! Good to see you.",
                messageType = MessageType.TEXT,
                deliveryStatus = DeliveryStatus.READ,
                createdAt = Instant.now().minusSeconds(1800).toString()
            )
        )
        _isLoading.value = false
    }

    fun onInputTextChange(newText: String) {
        _inputText.value = newText
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val chatId = currentChatId ?: return
        if (text.isEmpty()) return

        _inputText.value = ""

        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = "me",
            content = text,
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.SENT,
            createdAt = Instant.now().toString()
        )
        
        _messages.value = _messages.value + newMessage
    }

    fun receiveMockMessage() {
        val text = _inputText.value.trim()
        val chatId = currentChatId ?: return
        if (text.isEmpty()) return

        _inputText.value = ""

        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = "alice", // Received as Alice
            content = text,
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.READ,
            createdAt = Instant.now().toString()
        )

        _messages.value = _messages.value + newMessage
    }

    fun loadMoreMessages() {
        // Mock: no-op
    }

    fun editMessage(messageId: String, newContent: String) {
        // Mock: edit local state directly
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) msg.copy(content = newContent, isEdited = true) else msg
        }
    }

    fun deleteMessage(messageId: String) {
        // Mock: delete local state directly
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) msg.copy(isDeleted = true, content = "") else msg
        }
    }

    fun uploadAndSendMedia(fileBytes: ByteArray, fileName: String, contentType: String, messageType: String) {
        // Mock: no-op
    }

    fun getFormattedTimestamp(timestamp: String?): String = TimestampFormatter.formatRelative(timestamp)

    private fun cleanup() {
        messageSubscriptionJob?.cancel()
        typingSubscriptionJob?.cancel()
        typingDebounceJob?.cancel()
        messageSubscriptionJob = null
        typingSubscriptionJob = null
        typingDebounceJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
