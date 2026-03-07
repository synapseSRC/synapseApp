package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.usecase.chat.*
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import com.synapse.social.studioasinc.core.util.NotificationHelper
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
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val subscribeToMessagesUseCase: SubscribeToMessagesUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val broadcastTypingStatusUseCase: BroadcastTypingStatusUseCase,
    private val subscribeToTypingStatusUseCase: SubscribeToTypingStatusUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val initializeE2EUseCase: InitializeE2EUseCase,
    private val chatRepository: ChatRepository
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
        get() = chatRepository.getCurrentUserId()

    fun initialize(chatId: String, participantId: String? = null) {
        if (currentChatId == chatId && chatId != "new") return
        
        cleanup()
        
        _isLoading.value = true
        _error.value = null

        // Set participant profile info
        if (participantId != null) {
            viewModelScope.launch {
                getUserProfileUseCase(participantId).onSuccess { user ->
                    _participantProfile.value = user
                }.onFailure { e ->
                    Napier.e("Failed to load participant profile", e)
                }
            }
        }

        // Initialize E2EE keys if not already (safeguard)
        viewModelScope.launch {
            initializeE2EUseCase()
        }

        viewModelScope.launch {
            val actualChatId = if (chatId == "new" && participantId != null) {
                // We need to resolve the actual chat ID or create a new chat
                chatRepository.getOrCreateChat(participantId).getOrElse { 
                    _error.value = "Failed to create chat"
                    _isLoading.value = false
                    return@launch 
                }
            } else {
                chatId
            }
            
            currentChatId = actualChatId

            // Fetch initial messages
            getMessagesUseCase(actualChatId).onSuccess { messages ->
                _messages.value = messages.sortedBy { it.createdAt } // oldest first for UI
                _isLoading.value = false
            }.onFailure { e ->
                _error.value = e.message
                _isLoading.value = false
            }

            // Subscribe to real-time message updates
            messageSubscriptionJob = launch {
                subscribeToMessagesUseCase(actualChatId).collect { newMessage ->
                    _messages.update { current ->
                        val existing = current.find { it.id == newMessage.id }
                        if (existing != null) {
                            current.map { if (it.id == newMessage.id) newMessage else it }
                        } else {
                            (current + newMessage).sortedBy { it.createdAt }
                        }
                    }
                    markMessagesAsReadUseCase(actualChatId)
                }
            }

            // Mark current messages as read
            markMessagesAsReadUseCase(actualChatId)
        }
    }

    fun onInputTextChange(newText: String) {
        _inputText.value = newText
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val chatId = currentChatId ?: return
        if (text.isEmpty()) return

        _inputText.value = ""

        viewModelScope.launch {
            // Optimistic update
            val tempId = UUID.randomUUID().toString()
            val newMessage = Message(
                id = tempId,
                chatId = chatId,
                senderId = currentUserId ?: "",
                content = text,
                messageType = MessageType.TEXT,
                deliveryStatus = DeliveryStatus.SENT,
                createdAt = Instant.now().toString()
            )
            _messages.update { (it + newMessage).sortedBy { msg -> msg.createdAt } }

            // Actual send
            sendMessageUseCase(
                chatId = chatId,
                content = text,
                messageType = "text"
            ).onSuccess { actualMessage ->
                _messages.update { current ->
                    current.map { if (it.id == tempId) actualMessage else it }.sortedBy { msg -> msg.createdAt }
                }
                
                // Notify via OneSignal
                val recipientId = _participantProfile.value?.uid
                if (recipientId != null && currentUserId != null && recipientId != currentUserId) {
                    NotificationHelper.sendMessageAndNotifyIfNeeded(
                        chatId = chatId,
                        senderId = currentUserId!!,
                        recipientId = recipientId,
                        message = text
                    )
                }
            }.onFailure { e ->
                _error.value = "Failed to send: ${e.message}"
                // Remove optimistic message on failure
                _messages.update { current -> current.filter { it.id != tempId } }
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
