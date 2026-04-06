package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.util.UploadProgressManager
import com.synapse.social.studioasinc.shared.domain.usecase.chat.PopulateMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ToggleMessageReactionUseCase
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.usecase.chat.*
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserActiveStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val subscribeToMessagesUseCase: SubscribeToMessagesUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val markMessagesAsDeliveredUseCase: MarkMessagesAsDeliveredUseCase,
    private val broadcastTypingStatusUseCase: BroadcastTypingStatusUseCase,
    private val subscribeToTypingStatusUseCase: SubscribeToTypingStatusUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val deleteMessageForMeUseCase: DeleteMessageForMeUseCase,
    private val bulkDeleteMessagesForMeUseCase: BulkDeleteMessagesForMeUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val initializeE2EUseCase: InitializeE2EUseCase,
    private val getCurrentUserIdUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetCurrentUserIdUseCase,
    private val getOrCreateChatUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetOrCreateChatUseCase,
    private val getChatInfoUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatInfoUseCase,
    private val getGroupMembersUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetGroupMembersUseCase,
    private val getMessageByIdUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessageByIdUseCase,
    private val chatLockManager: com.synapse.social.studioasinc.core.util.ChatLockManager,
    private val generateSmartRepliesUseCase: com.synapse.social.studioasinc.domain.usecase.ai.GenerateSmartRepliesUseCase,
    private val summarizeChatUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizeChatUseCase,
    private val summarizeMessageUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizeMessageUseCase,
    private val uploadProgressManager: UploadProgressManager,
    private val fileUploader: FileUploader,
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase,
    private val populateMessageReactionsUseCase: PopulateMessageReactionsUseCase,
    private val getChatSettingsUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatSettingsUseCase,
    private val observeUserActiveStatusUseCase: ObserveUserActiveStatusUseCase
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    private val _participantProfile = MutableStateFlow<User?>(null)
    val participantProfile: StateFlow<User?> = _participantProfile.asStateFlow()

    private val _isParticipantActive = MutableStateFlow(false)
    val isParticipantActive: StateFlow<Boolean> = _isParticipantActive.asStateFlow()

    private val _isGroupChat = MutableStateFlow(false)
    val isGroupChat: StateFlow<Boolean> = _isGroupChat.asStateFlow()

    private val _isCurrentUserAdmin = MutableStateFlow(false)
    val isCurrentUserAdmin: StateFlow<Boolean> = _isCurrentUserAdmin.asStateFlow()

    private val _onlyAdminsCanMessage = MutableStateFlow(false)
    val onlyAdminsCanMessage: StateFlow<Boolean> = _onlyAdminsCanMessage.asStateFlow()

    val canSendMessage: StateFlow<Boolean> = combine(_onlyAdminsCanMessage, _isCurrentUserAdmin) { restricted, isAdmin ->
        !restricted || isAdmin
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

    val chatWallpaperType = getChatSettingsUseCase.chatWallpaperType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType.DEFAULT)
    val chatWallpaperValue = getChatSettingsUseCase.chatWallpaperValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val chatWallpaperBlur = getChatSettingsUseCase.chatWallpaperBlur
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
    val chatFontScale = getChatSettingsUseCase.chatFontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
    val chatThemePreset = getChatSettingsUseCase.chatThemePreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset.DEFAULT)
    val chatMessageCornerRadius = getChatSettingsUseCase.chatMessageCornerRadius
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)
    
    private val chatMaxMessageChunkSize = getChatSettingsUseCase.chatMaxMessageChunkSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 500)

    val messageSuggestionEnabled = getChatSettingsUseCase.messageSuggestionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val chatAvatarDisabled = getChatSettingsUseCase.chatAvatarDisabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds: StateFlow<Set<String>> = _selectedMessageIds.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<Message?>(null)
    val replyingToMessage: StateFlow<Message?> = _replyingToMessage.asStateFlow()

    private val _disappearingMode = MutableStateFlow<DisappearingMode>(DisappearingMode.OFF)
    val disappearingMode: StateFlow<DisappearingMode> = _disappearingMode.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isE2EEReady = MutableStateFlow(false)
    val isE2EEReady: StateFlow<Boolean> = _isE2EEReady.asStateFlow()

    private var currentChatId: String? = null

    val currentUserId: String?
        get() = getCurrentUserIdUseCase()

    private val messagingDelegate = ChatMessagingDelegate(
        sendMessageUseCase = sendMessageUseCase,
        editMessageUseCase = editMessageUseCase,
        deleteMessageUseCase = deleteMessageUseCase,
        deleteMessageForMeUseCase = deleteMessageForMeUseCase,
        bulkDeleteMessagesForMeUseCase = bulkDeleteMessagesForMeUseCase,
        populateMessageReactionsUseCase = populateMessageReactionsUseCase,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId }
    )

    private val aiDelegate = ChatAiDelegate(
        generateSmartRepliesUseCase = generateSmartRepliesUseCase,
        summarizeChatUseCase = summarizeChatUseCase,
        summarizeMessageUseCase = summarizeMessageUseCase,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId },
        participantProfileProvider = { participantProfile.value },
        messageSuggestionEnabledProvider = { messageSuggestionEnabled.value },
        onError = { _error.value = it },
        onLoading = { _isLoading.value = it }
    )

    private val subscriptionDelegate = ChatSubscriptionDelegate(
        subscribeToMessagesUseCase = subscribeToMessagesUseCase,
        subscribeToTypingStatusUseCase = subscribeToTypingStatusUseCase,
        markMessagesAsReadUseCase = markMessagesAsReadUseCase,
        markMessagesAsDeliveredUseCase = markMessagesAsDeliveredUseCase,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId },
        onNewMessage = { newMessage ->
            handleIncomingMessage(newMessage)
        }
    )

    private val mediaDelegate = ChatMediaDelegate(
        uploadMediaUseCase = uploadMediaUseCase,
        sendMessageUseCase = sendMessageUseCase,
        fileUploader = fileUploader,
        uploadProgressManager = uploadProgressManager,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId },
        chatIdProvider = { currentChatId },
        onOptimisticMessageAdded = { msg, tempId ->
            messagingDelegate.pendingTempIds.update { it + tempId }
            messagingDelegate._messages.update { current ->
                (current + msg).distinctBy { it.id }.sortedBy { it.createdAt }
            }
        },
        onOptimisticMessageUpdated = { tempId, content ->
            messagingDelegate._messages.update { current ->
                with(messagingDelegate) {
                    current.updateById(tempId) { it.copy(content = content) }
                }
            }
        },
        onOptimisticMessageSuccess = { tempId, actualMessage ->
            messagingDelegate.pendingTempIds.update { it - tempId }
            messagingDelegate._messages.update { current ->
                val hasTempMessage = current.any { it.id == tempId }
                val hasActualMessage = current.any { it.id == actualMessage.id }
                when {
                    hasTempMessage && !hasActualMessage -> {
                        with(messagingDelegate) {
                            current.replaceById(tempId, actualMessage)
                                .sortedBy { msg -> msg.createdAt }
                        }
                    }
                    hasTempMessage && hasActualMessage -> {
                        current.filter { it.id != tempId }
                            .sortedBy { msg -> msg.createdAt }
                    }
                    else -> current
                }
            }
        },
        onOptimisticMessageFailed = { tempId, errorMessage ->
            messagingDelegate.pendingTempIds.update { it - tempId }
            _error.value = errorMessage
            messagingDelegate._messages.update { current -> current.filter { it.id != tempId } }
        },
        onError = { _error.value = it }
    )

    private val reactionDelegate = ChatReactionDelegate(
        toggleMessageReactionUseCase = toggleMessageReactionUseCase,
        viewModelScope = viewModelScope,
        onOptimisticReactionChanged = { messageId, oldMessage, newUserReaction, oldUserReaction ->
            messagingDelegate._messages.update { current ->
                with(messagingDelegate) {
                    current.updateById(messageId) {
                        val oldReactionCounts = oldMessage.reactions.toMutableMap()
                        if (oldUserReaction != null) {
                            oldReactionCounts[oldUserReaction] = (oldReactionCounts[oldUserReaction] ?: 0) - 1
                            if (oldReactionCounts[oldUserReaction] == 0) oldReactionCounts.remove(oldUserReaction)
                        }
                        if (newUserReaction != null) {
                            oldReactionCounts[newUserReaction] = (oldReactionCounts[newUserReaction] ?: 0) + 1
                        }
                        it.copy(
                            userReaction = newUserReaction,
                            reactions = oldReactionCounts
                        )
                    }
                }
            }
        },
        onError = { errorMessage, oldMessage ->
            _error.value = errorMessage
            messagingDelegate._messages.update { current ->
                with(messagingDelegate) {
                    oldMessage.id?.let { id ->
                        current.replaceById(id, oldMessage)
                    } ?: current
                }
            }
        }
    )

    private val initializationDelegate = ChatInitializationDelegate(
        getUserProfileUseCase = getUserProfileUseCase,
        initializeE2EUseCase = initializeE2EUseCase,
        getOrCreateChatUseCase = getOrCreateChatUseCase,
        getMessagesUseCase = getMessagesUseCase,
        getChatInfoUseCase = getChatInfoUseCase,
        getGroupMembersUseCase = getGroupMembersUseCase,
        observeUserActiveStatusUseCase = observeUserActiveStatusUseCase,
        markMessagesAsReadUseCase = markMessagesAsReadUseCase,
        markMessagesAsDeliveredUseCase = markMessagesAsDeliveredUseCase,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId },
        messagingDelegate = messagingDelegate,
        subscriptionDelegate = subscriptionDelegate,
        aiDelegate = aiDelegate,
        _isLoading = _isLoading,
        _error = _error,
        _participantProfile = _participantProfile,
        _isE2EEReady = _isE2EEReady,
        _isGroupChat = _isGroupChat,
        _onlyAdminsCanMessage = _onlyAdminsCanMessage,
        _isCurrentUserAdmin = _isCurrentUserAdmin,
        _isParticipantActive = _isParticipantActive,
        onChatIdResolved = { currentChatId = it }
    )

    private val inputDelegate = ChatInputDelegate(
        broadcastTypingStatusUseCase = broadcastTypingStatusUseCase,
        viewModelScope = viewModelScope,
        messagingDelegate = messagingDelegate,
        _inputText = _inputText,
        _editingMessage = _editingMessage,
        _error = _error,
        _replyingToMessage = _replyingToMessage,
        _toastMessage = _toastMessage,
        _selectedMessageIds = _selectedMessageIds,
        chatMaxMessageChunkSizeProvider = { chatMaxMessageChunkSize.value },
        currentChatIdProvider = { currentChatId },
        isE2EEReadyProvider = { _isE2EEReady.value },
        disappearingModeProvider = { _disappearingMode.value },
        onChatRefreshRequired = { currentChatId?.let { initialize(it) } }
    )

    private val settingsDelegate = ChatSettingsDelegate(
        chatLockManager = chatLockManager,
        _disappearingMode = _disappearingMode,
        currentChatIdProvider = { currentChatId }
    )

    // Public delegated properties
    val messages: StateFlow<List<Message>> = messagingDelegate.messages
    val chatItems: StateFlow<List<ChatListItem>> = messagingDelegate.chatItems
    val typingStatus: StateFlow<TypingStatus?> = subscriptionDelegate.typingStatus
    val smartReplies: StateFlow<List<String>> = aiDelegate.smartReplies
    val chatSummary: StateFlow<String?> = aiDelegate.chatSummary
    val messageSummary: StateFlow<String?> = aiDelegate.messageSummary
    val isSummarizingMessage: StateFlow<Boolean> = aiDelegate.isSummarizingMessage

    fun initialize(chatId: String, participantId: String? = null) {
        if (currentChatId == chatId && chatId != "new") return
        
        cleanup()
        initializationDelegate.initialize(chatId, participantId, currentChatId)
    }

    /** Re-fetches messages for the current chat (e.g. after screen resumes from off). */
    fun refreshMessages() {
        val chatId = currentChatId ?: return
        _hasMoreMessages.value = true
        viewModelScope.launch {
            getMessagesUseCase(chatId).onSuccess { messages ->
                messagingDelegate.setMessages(messages)
            }
        }
    }

    fun loadMoreMessages() {
        val chatId = currentChatId ?: return
        if (_isLoadingMore.value || !_hasMoreMessages.value) return
        val oldest = messagingDelegate.messages.value.firstOrNull()?.createdAt ?: return
        _isLoadingMore.value = true
        viewModelScope.launch {
            getMessagesUseCase(chatId, before = oldest).onSuccess { older ->
                if (older.isEmpty()) {
                    _hasMoreMessages.value = false
                } else {
                    messagingDelegate._messages.update { current ->
                        (older + current).distinctBy { it.id }.sortedBy { it.createdAt }
                    }
                }
            }
            _isLoadingMore.value = false
        }
    }

    private fun handleIncomingMessage(newMessage: Message) {
        val encryptedPlaceholders = ChatMessagingDelegate.ENCRYPTED_PLACEHOLDERS
        messagingDelegate._messages.update { current ->
            val existing = current.find { it.id == newMessage.id }
            if (existing != null) {
                val mergedMessage = if (
                    existing.content !in encryptedPlaceholders &&
                    newMessage.content in encryptedPlaceholders
                ) {
                    existing.copy(
                        deliveryStatus = newMessage.deliveryStatus,
                        readBy = newMessage.readBy
                    )
                } else {
                    val contentChanged = existing.content != newMessage.content
                    newMessage.copy(
                        isEdited = if (contentChanged) newMessage.isEdited else existing.isEdited
                    )
                }
                with(messagingDelegate) { current.replaceById(newMessage.id, mergedMessage) }
            } else if (newMessage.senderId == currentUserId && messagingDelegate.pendingTempIds.value.isNotEmpty()) {
                val tempId = messagingDelegate.pendingTempIds.value.firstOrNull { id ->
                    current.any { it.id == id }
                }
                if (tempId != null) {
                    messagingDelegate.pendingTempIds.update { it - tempId }
                    val tempMsg = current.find { it.id == tempId }
                    val finalMessage = if (
                        tempMsg != null &&
                        tempMsg.content !in encryptedPlaceholders &&
                        newMessage.content in encryptedPlaceholders
                    ) {
                        newMessage.copy(content = tempMsg.content)
                    } else {
                        newMessage
                    }
                    with(messagingDelegate) {
                        current.replaceById(tempId, finalMessage)
                            .distinctBy { it.id }
                            .sortedBy { it.createdAt }
                    }
                } else {
                    (current + newMessage).distinctBy { it.id }.sortedBy { it.createdAt }
                }
            } else {
                (current + newMessage).distinctBy { it.id }.sortedBy { it.createdAt }
            }
        }
        aiDelegate.generateSmartReplies(messages.value)

        // Race condition: realtime message arrived before Signal session was ready → retry decryption
        if (newMessage.senderId != currentUserId && newMessage.content in encryptedPlaceholders) {
            viewModelScope.launch {
                delay(2.seconds)
                getMessageByIdUseCase(newMessage.id).onSuccess { refreshed ->
                    if (refreshed != null && refreshed.content !in encryptedPlaceholders) {
                        messagingDelegate._messages.update { current ->
                            with(messagingDelegate) { current.replaceById(newMessage.id, refreshed) }
                        }
                    }
                }
            }
        }
    }

    fun summarizeChat() {
        aiDelegate.summarizeChat(messages.value)
    }

    fun clearSummary() {
        aiDelegate.clearSummary()
    }

    fun summarizeMessage(content: String) {
        aiDelegate.summarizeMessage(content)
    }

    fun clearMessageSummary() {
        aiDelegate.clearMessageSummary()
    }

    fun onInputTextChange(newText: String) {
        inputDelegate.onInputTextChange(newText)
    }

    fun sendMessage() {
        inputDelegate.sendMessage()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun toggleMessageReaction(messageId: String, reactionType: com.synapse.social.studioasinc.shared.domain.model.ReactionType) {
        reactionDelegate.toggleMessageReaction(messageId, reactionType, messages.value)
    }

    fun startEditing(message: Message) {
        inputDelegate.startEditing(message)
    }

    fun cancelEditing() {
        inputDelegate.cancelEditing()
    }

    fun toggleMessageSelection(messageId: String) {
        inputDelegate.toggleMessageSelection(messageId)
    }

    fun clearSelection() {
        inputDelegate.clearSelection()
    }

    fun deleteSelectedMessages() {
        inputDelegate.deleteSelectedMessages()
    }

    fun setReplyingToMessage(message: Message) {
        inputDelegate.setReplyingToMessage(message)
    }

    fun cancelReply() {
        inputDelegate.cancelReply()
    }

    fun editMessage(messageId: String, newContent: String) {
        messagingDelegate.editMessage(messageId, newContent)
    }

    fun deleteMessage(messageId: String) {
        messagingDelegate.deleteMessage(messageId)
    }

    fun deleteMessageForMe(messageId: String) {
        messagingDelegate.deleteMessageForMe(messageId)
    }

    fun isChatLocked(): Boolean {
        return settingsDelegate.isChatLocked()
    }

    fun lockCurrentChat() {
        settingsDelegate.lockCurrentChat()
    }

    fun unlockCurrentChat() {
        settingsDelegate.unlockCurrentChat()
    }

    fun setDisappearingMode(mode: DisappearingMode) {
        settingsDelegate.setDisappearingMode(mode)
    }

    fun uploadAndSendMedia(filePath: String, fileName: String, contentType: String, messageType: String, caption: String? = null) {
        mediaDelegate.uploadAndSendMedia(filePath, fileName, contentType, messageType, caption)
    }

    fun sendVoiceMessage(mediaUrl: String, durationMs: Long) {
        val chatId = currentChatId ?: return
        val tempId = java.util.UUID.randomUUID().toString()
        val optimisticMessage = com.synapse.social.studioasinc.shared.domain.model.chat.Message(
            id = tempId,
            chatId = chatId,
            senderId = currentUserId ?: "",
            content = "Voice Message (${durationMs / 1000}s)",
            messageType = com.synapse.social.studioasinc.shared.domain.model.chat.MessageType.AUDIO,
            mediaUrl = mediaUrl,
            deliveryStatus = com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.SENT,
            createdAt = java.time.Instant.now().toString()
        )
        messagingDelegate.pendingTempIds.update { it + tempId }
        messagingDelegate._messages.update { current ->
            (current + optimisticMessage).sortedBy { it.createdAt }
        }
        viewModelScope.launch {
            sendMessageUseCase(
                chatId = chatId,
                content = optimisticMessage.content,
                mediaUrl = mediaUrl,
                messageType = "audio"
            ).onSuccess { actualMessage ->
                messagingDelegate.pendingTempIds.update { it - tempId }
                messagingDelegate._messages.update { current ->
                    current.map { if (it.id == tempId) actualMessage else it }
                }
            }.onFailure { e ->
                messagingDelegate.pendingTempIds.update { it - tempId }
                messagingDelegate._messages.update { current -> current.filter { it.id != tempId } }
                _error.value = "Failed to send voice message: ${e.message}"
            }
        }
    }

    fun getFormattedTimestamp(timestamp: String?): String = TimestampFormatter.formatRelative(timestamp)

    private fun cleanup() {
        subscriptionDelegate.cleanup()
        inputDelegate.cleanup()
        messagingDelegate.pendingTempIds.value = emptySet()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
