package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.usecase.chat.*

import com.synapse.social.studioasinc.domain.usecase.reaction.ToggleMessageReactionUseCase
import com.synapse.social.studioasinc.domain.usecase.reaction.PopulateMessageReactionsUseCase
import com.synapse.social.studioasinc.domain.model.ReactionType as AppReactionType

import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import kotlin.time.Duration.Companion.seconds
import com.synapse.social.studioasinc.core.util.NotificationHelper
import com.synapse.social.studioasinc.core.util.UploadProgressManager
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
    private val deleteMessageForMeUseCase: DeleteMessageForMeUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val initializeE2EUseCase: InitializeE2EUseCase,
    private val chatRepository: ChatRepository,
    private val chatLockManager: com.synapse.social.studioasinc.core.util.ChatLockManager,
    private val generateSmartRepliesUseCase: com.synapse.social.studioasinc.domain.usecase.ai.GenerateSmartRepliesUseCase,
    private val summarizeChatUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizeChatUseCase,
    private val uploadProgressManager: UploadProgressManager,
    private val fileUploader: com.synapse.social.studioasinc.shared.data.FileUploader,
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase,
    private val populateMessageReactionsUseCase: PopulateMessageReactionsUseCase
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

    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds: StateFlow<Set<String>> = _selectedMessageIds.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<Message?>(null)
    val replyingToMessage: StateFlow<Message?> = _replyingToMessage.asStateFlow()

    private val _disappearingMode = MutableStateFlow<DisappearingMode>(DisappearingMode.OFF)
    val disappearingMode: StateFlow<DisappearingMode> = _disappearingMode.asStateFlow()

    private val _smartReplies = MutableStateFlow<List<String>>(emptyList())
    val smartReplies: StateFlow<List<String>> = _smartReplies.asStateFlow()

    private val _chatSummary = MutableStateFlow<String?>(null)
    val chatSummary: StateFlow<String?> = _chatSummary.asStateFlow()

    private val _isE2EEReady = MutableStateFlow(false)
    val isE2EEReady: StateFlow<Boolean> = _isE2EEReady.asStateFlow()

    private var currentChatId: String? = null
    private var messageSubscriptionJob: Job? = null
    private var typingSubscriptionJob: Job? = null
    private var typingDebounceJob: Job? = null

    // Track pending optimistic message temp IDs to prevent duplicates from realtime
    private val pendingTempIds = MutableStateFlow<Set<String>>(emptySet())

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
                    Napier.d("Loaded participant profile: ${user?.username}, avatar: ${user?.avatar}", tag = "ChatViewModel")
                    _participantProfile.value = user
                }.onFailure { e ->
                    Napier.e("Failed to load participant profile", e)
                }
            }
        }

        // Initialize E2EE keys if not already (safeguard)
        viewModelScope.launch {
            initializeE2EUseCase().onSuccess {
                _isE2EEReady.value = true
                Napier.d("E2EE initialization successful", tag = "E2EE")
            }.onFailure { e ->
                _isE2EEReady.value = false
                Napier.e("E2EE initialization failed: ${e.message}", e, tag = "E2EE")
            }
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
                val populated = populateMessageReactionsUseCase(messages)
                _messages.value = populated.distinctBy { it.id }.sortedBy { it.createdAt } // oldest first for UI

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
                            // Message already exists (e.g., realtime update for an edit), replace it
                            current.map { if (it.id == newMessage.id) newMessage else it }
                        } else if (newMessage.senderId == currentUserId && pendingTempIds.value.isNotEmpty()) {
                            // This is our own message arriving via realtime while we have a pending
                            // optimistic message. Replace the oldest pending temp message with the
                            // real server message to avoid duplicates.
                            val tempId = pendingTempIds.value.firstOrNull { id ->
                                current.any { it.id == id }
                            }
                            if (tempId != null) {
                                pendingTempIds.update { it - tempId }
                                current.map { if (it.id == tempId) newMessage else it }
                                    .sortedBy { it.createdAt }
                            } else {
                                (current + newMessage).sortedBy { it.createdAt }
                            }
                        } else {
                            (current + newMessage).sortedBy { it.createdAt }
                        }
                    }
                    markMessagesAsReadUseCase(actualChatId)
                    generateSmartReplies()
                }
            }

            // Subscribe to typing status
            typingSubscriptionJob = launch {
                subscribeToTypingStatusUseCase(actualChatId).collect { status ->
                    if (status.userId != currentUserId) {
                        _typingStatus.value = if (status.isTyping) status else null
                    }
                }
            }

            // Mark current messages as read
            markMessagesAsReadUseCase(actualChatId)

            // Generate initial smart replies
            generateSmartReplies()
        }
    }

    private fun generateSmartReplies() {
        viewModelScope.launch {
            val currentMessages = _messages.value
            if (currentMessages.isEmpty()) {
                _smartReplies.value = emptyList()
                return@launch
            }

            // Only consider recent messages to avoid prompt limits
            val recentMessages = currentMessages.takeLast(10).map { msg ->
                val senderName = if (msg.senderId == currentUserId) "Me" else participantProfile.value?.displayName ?: "Them"
                "$senderName: ${msg.content}"
            }

            generateSmartRepliesUseCase(recentMessages)
                .onSuccess { replies ->
                    _smartReplies.value = replies
                }
                .onFailure {
                    // Ignore errors for smart replies
                    _smartReplies.value = emptyList()
                }
        }
    }

    fun summarizeChat() {
        viewModelScope.launch {
            val currentMessages = _messages.value
            if (currentMessages.isEmpty()) {
                _chatSummary.value = "No messages to summarize."
                return@launch
            }

            _isLoading.value = true

            // Consider recent messages to avoid prompt limits
            val messagesToSummarize = currentMessages.takeLast(50).map { msg ->
                val senderName = if (msg.senderId == currentUserId) "Me" else participantProfile.value?.displayName ?: "Them"
                "$senderName: ${msg.content}"
            }

            summarizeChatUseCase(messagesToSummarize)
                .onSuccess { summary ->
                    _chatSummary.value = summary
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = "Failed to summarize chat: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    fun clearSummary() {
        _chatSummary.value = null
    }

    fun onInputTextChange(newText: String) {
        _inputText.value = newText
        
        val chatId = currentChatId ?: return
        
        // Cancel previous debounce job
        typingDebounceJob?.cancel()
        
        // Broadcast typing = true
        viewModelScope.launch {
            broadcastTypingStatusUseCase(chatId, true)
        }
        
        // Debounce typing = false after 2 seconds
        typingDebounceJob = viewModelScope.launch {
            delay(2000)
            broadcastTypingStatusUseCase(chatId, false)
        }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val chatId = currentChatId ?: return
        if (text.isEmpty()) return

        val editingMsg = _editingMessage.value
        if (editingMsg != null) {
            saveEdit(editingMsg, text)
            return
        }

        // Wait for E2EE initialization if not ready
        if (!_isE2EEReady.value) {
            Napier.d("E2EE not ready, waiting for initialization...", tag = "E2EE")
            viewModelScope.launch {
                // Wait up to 3 seconds for E2EE to initialize
                var attempts = 0
                while (!_isE2EEReady.value && attempts < 30) {
                    delay(100)
                    attempts++
                }
                if (_isE2EEReady.value) {
                    Napier.d("E2EE ready after waiting", tag = "E2EE")
                } else {
                    Napier.w("E2EE initialization timeout, proceeding anyway", tag = "E2EE")
                }
                performSendMessage(chatId, text)
            }
            return
        }

        performSendMessage(chatId, text)
    }

    private fun performSendMessage(chatId: String, text: String) {
        _inputText.value = ""

        val currentMode = _disappearingMode.value
        val expiresAt = currentMode.seconds?.let { seconds ->
            Instant.now().plusSeconds(seconds).toString()
        }

        val replyToMessage = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            // Optimistic update
            val tempId = UUID.randomUUID().toString()
            // Register this temp ID so the realtime handler knows to replace it
            pendingTempIds.update { it + tempId }

            val newMessage = Message(
                id = tempId,
                chatId = chatId,
                senderId = currentUserId ?: "",
                content = text,
                messageType = MessageType.TEXT,
                deliveryStatus = DeliveryStatus.SENT,
                createdAt = Instant.now().toString(),
                expiresAt = expiresAt,
                replyToId = replyToMessage?.id
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
                replyToId = replyToMessage?.id
            ).onSuccess { actualMessage ->
                // Only replace if the temp message hasn't already been swapped by the realtime handler
                pendingTempIds.update { it - tempId }
                _messages.update { current ->
                    val hasTempMessage = current.any { it.id == tempId }
                    val hasActualMessage = current.any { it.id == actualMessage.id }
                    when {
                        hasTempMessage && !hasActualMessage -> {
                            // Normal case: realtime hasn't arrived yet, replace temp with actual
                            current.map { if (it.id == tempId) actualMessage else it }
                                .sortedBy { msg -> msg.createdAt }
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
                _error.value = "Failed to send: ${e.message}"
                // Remove optimistic message on failure
                _messages.update { current -> current.filter { it.id != tempId } }
            }
        }
    }


    fun toggleMessageReaction(messageId: String, reactionType: com.synapse.social.studioasinc.shared.domain.model.ReactionType) {
        viewModelScope.launch {
            val oldMessage = _messages.value.find { it.id == messageId } ?: return@launch
            val oldUserReaction = oldMessage.userReaction
            val oldReactionCounts = oldMessage.reactions.toMutableMap()

            val isSame = oldUserReaction == reactionType
            val newUserReaction = if (isSame) null else reactionType

            if (oldUserReaction != null) {
                oldReactionCounts[oldUserReaction] = (oldReactionCounts[oldUserReaction] ?: 0) - 1
                if (oldReactionCounts[oldUserReaction] == 0) oldReactionCounts.remove(oldUserReaction)
            }
            if (!isSame) {
                oldReactionCounts[reactionType] = (oldReactionCounts[reactionType] ?: 0) + 1
            }

            // Optimistic update
            _messages.update { current ->
                current.map {
                    if (it.id == messageId) {
                        it.copy(
                            userReaction = newUserReaction,
                            reactions = oldReactionCounts
                        )
                    } else it
                }
            }

            val appReactionType = AppReactionType.fromString(reactionType.name)
            val oldAppReaction = oldUserReaction?.let { AppReactionType.fromString(it.name) }
            toggleMessageReactionUseCase(messageId, appReactionType, oldAppReaction).onFailure { e ->
                _error.value = "Failed to toggle reaction: ${e.message}"
                // Revert
                _messages.update { current ->
                    current.map {
                        if (it.id == messageId) oldMessage else it
                    }
                }
            }
        }
    }

    fun startEditing(message: Message) {
        _editingMessage.value = message
        _inputText.value = message.content
    }

    fun cancelEditing() {
        _editingMessage.value = null
        _inputText.value = ""
    }

    fun toggleMessageSelection(messageId: String) {
        _selectedMessageIds.update { current ->
            if (current.contains(messageId)) {
                current - messageId
            } else {
                current + messageId
            }
        }
    }

    fun clearSelection() {
        _selectedMessageIds.value = emptySet()
    }

    fun deleteSelectedMessages() {
        val selectedIds = _selectedMessageIds.value
        _selectedMessageIds.value = emptySet()
        selectedIds.forEach { id ->
            deleteMessageForMe(id) // Or deleteMessage(id) if intended for everyone, but for me is safer default.
        }
    }

    fun setReplyingToMessage(message: Message) {
        _replyingToMessage.value = message
        // Automatically open keyboard or focus input if possible in UI layer
    }

    fun cancelReply() {
        _replyingToMessage.value = null
    }

    private fun saveEdit(message: Message, newContent: String) {
        val messageId = message.id ?: return
        _editingMessage.value = null
        _inputText.value = ""

        viewModelScope.launch {
            // Optimistic update
            _messages.update { current ->
                current.map { 
                    if (it.id == messageId) it.copy(content = newContent, isEdited = true) 
                    else it 
                }
            }

            editMessageUseCase(messageId, newContent).onSuccess {
                // Notification handled by repository if needed
            }.onFailure { e ->
                _error.value = "Failed to edit: ${e.message}"
                // Revert optimistic update
                _messages.update { current ->
                    current.map { 
                        if (it.id == messageId) message 
                        else it 
                    }
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

    fun isChatLocked(): Boolean {
        return currentChatId?.let { chatLockManager.isChatLocked(it) } ?: false
    }

    fun lockCurrentChat() {
        currentChatId?.let { chatLockManager.lockChat(it) }
    }

    fun unlockCurrentChat() {
        currentChatId?.let { chatLockManager.unlockChat(it) }
    }

    fun setDisappearingMode(mode: DisappearingMode) {
        _disappearingMode.value = mode
    }

    fun uploadAndSendMedia(filePath: String, fileName: String, contentType: String, messageType: String) {
        val chatId = currentChatId ?: return

        viewModelScope.launch {
            val fileSize = fileUploader.getFileSize(filePath)
            val maxVideoSize = 50 * 1024 * 1024L // 50MB
            val maxImageSize = 10 * 1024 * 1024L // 10MB

            if (messageType == "video" && fileSize > maxVideoSize) {
                _error.value = "Video file size exceeds 50MB limit"
                return@launch
            }
            if (messageType == "image" && fileSize > maxImageSize) {
                _error.value = "Image file size exceeds 10MB limit"
                return@launch
            }

            val notificationId = kotlin.random.Random.nextInt()
            uploadProgressManager.showProgress(notificationId, 0, "Uploading media")

            // Optimistic update
            val tempId = UUID.randomUUID().toString()
            // Register this temp ID so the realtime handler knows to replace it
            pendingTempIds.update { it + tempId }

            val type = when(messageType) {
                "image" -> MessageType.IMAGE
                "video" -> MessageType.VIDEO
                "audio" -> MessageType.AUDIO
                else -> MessageType.FILE
            }
            val newMessage = Message(
                id = tempId,
                chatId = chatId,
                senderId = currentUserId ?: "",
                content = "Uploading...",
                messageType = type,
                deliveryStatus = DeliveryStatus.SENT,
                createdAt = Instant.now().toString()
            )
            _messages.update { current ->
                (current + newMessage).distinctBy { it.id }.sortedBy { msg -> msg.createdAt }
            }

            uploadMediaUseCase(
                chatId = chatId,
                filePath = filePath,
                fileName = fileName,
                contentType = contentType,
                onProgress = { progress ->
                    uploadProgressManager.updateProgress(chatId, fileName, progress)
                    _messages.update { current ->
                        current.map {
                            if (it.id == tempId) {
                                it.copy(content = "Uploading... $progress%")
                            } else {
                                it
                            }
                        }
                    }
                }
            ).onSuccess { mediaUrl ->
                val finalContent = if (messageType == "image" || messageType == "video") "Media message" else fileName
                sendMessageUseCase(
                    chatId = chatId,
                    content = finalContent,
                    mediaUrl = mediaUrl,
                    messageType = messageType
                ).onSuccess { actualMessage ->
                    pendingTempIds.update { it - tempId }
                    _messages.update { current ->
                        val hasTempMessage = current.any { it.id == tempId }
                        val hasActualMessage = current.any { it.id == actualMessage.id }
                        when {
                            hasTempMessage && !hasActualMessage -> {
                                current.map { if (it.id == tempId) actualMessage else it }
                                    .sortedBy { msg -> msg.createdAt }
                            }
                            hasTempMessage && hasActualMessage -> {
                                current.filter { it.id != tempId }
                                    .sortedBy { msg -> msg.createdAt }
                            }
                            else -> current
                        }
                    }
                    // Notification handled by repository
                }.onFailure { e ->
                    pendingTempIds.update { it - tempId }
                    _error.value = "Failed to send: ${e.message}"
                    _messages.update { current -> current.filter { it.id != tempId } }
                }
            }.onFailure { e ->
                uploadProgressManager.finishProgress(notificationId, false, "Upload Failed")
                pendingTempIds.update { it - tempId }
                _error.value = "Upload failed: ${e.message}"
                _messages.update { current -> current.filter { it.id != tempId } }
            }
        }
    }

    fun getFormattedTimestamp(timestamp: String?): String = TimestampFormatter.formatRelative(timestamp)

    private fun cleanup() {
        messageSubscriptionJob?.cancel()
        typingSubscriptionJob?.cancel()
        typingDebounceJob?.cancel()
        messageSubscriptionJob = null
        typingSubscriptionJob = null
        typingDebounceJob = null
        pendingTempIds.value = emptySet()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
