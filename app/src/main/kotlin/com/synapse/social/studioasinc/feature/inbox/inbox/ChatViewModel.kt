package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.util.UploadProgressManager
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.mergeMonotonic
import com.synapse.social.studioasinc.shared.domain.usecase.chat.PopulateMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ToggleMessageReactionUseCase
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.usecase.chat.*
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserActiveStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.GetStorageConfigUseCase
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
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
    private val generateSmartRepliesUseCase: com.synapse.social.studioasinc.shared.domain.usecase.ai.GenerateSmartRepliesUseCase,
    private val summarizeChatUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizeChatUseCase,
    private val summarizeMessageUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizeMessageUseCase,
    private val uploadProgressManager: UploadProgressManager,
    private val fileUploader: FileUploader,
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase,
    private val populateMessageReactionsUseCase: PopulateMessageReactionsUseCase,
    private val subscribeToMessageReactionsUseCase: SubscribeToMessageReactionsUseCase,
    private val getChatSettingsUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatSettingsUseCase,
    private val observeUserActiveStatusUseCase: ObserveUserActiveStatusUseCase,
    private val setDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.SetDisappearingModeUseCase,
    private val getDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase,
    private val voiceUploadService: VoiceUploadService,
    private val getStorageConfigUseCase: GetStorageConfigUseCase,
    private val voteInPollUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.VoteInPollUseCase,
    private val markViewOnceConsumedUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkViewOnceConsumedUseCase,
    private val mediaCompressor: com.synapse.social.studioasinc.shared.domain.service.MediaCompressor? = null
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

    private val _groupMemberNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val groupMemberNames: StateFlow<Map<String, String>> = _groupMemberNames.asStateFlow()

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

    private val _isScreenVisible = MutableStateFlow(false)
    val isScreenVisible: StateFlow<Boolean> = _isScreenVisible.asStateFlow()

    private var currentChatId: String? = null
    private val loadingQuotedMessageIds = ConcurrentHashMap<String, Unit>()
    private val historyPaginationMutex = Mutex()

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
        chatIdProvider = { currentChatId },
        participantProfileProvider = { participantProfile.value },
        messageSuggestionEnabledProvider = { messageSuggestionEnabled.value },
        onError = { _error.value = it },
        onLoading = { _isLoading.value = it }
    )

    private val subscriptionDelegate = ChatSubscriptionDelegate(
        subscribeToMessagesUseCase = subscribeToMessagesUseCase,
        subscribeToTypingStatusUseCase = subscribeToTypingStatusUseCase,
        subscribeToMessageReactionsUseCase = subscribeToMessageReactionsUseCase,
        markMessagesAsReadUseCase = markMessagesAsReadUseCase,
        markMessagesAsDeliveredUseCase = markMessagesAsDeliveredUseCase,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId },
        isScreenVisibleProvider = { _isScreenVisible.value },
        onNewMessage = { newMessage ->
            handleIncomingMessage(newMessage)
        },
        onReactionEvent = { reaction ->
            handleIncomingReaction(reaction)
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
            messagingDelegate._messages.update { current ->
                with(messagingDelegate) {
                    current.updateById(tempId) {
                        it.copy(
                            deliveryStatus = com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.FAILED,
                            failureReason = errorMessage
                        )
                    }
                }
            }
        },
        onError = { _error.value = it },
        mediaCompressor = mediaCompressor,
        replyToMessageIdProvider = { _replyingToMessage.value?.id },
        onReplyConsumed = { sentReplyToId ->
            if (_replyingToMessage.value?.id == sentReplyToId) _replyingToMessage.value = null
        }
    )

    private val reactionDelegate = ChatReactionDelegate(
        toggleMessageReactionUseCase = toggleMessageReactionUseCase,
        viewModelScope = viewModelScope,
        onOptimisticReactionChanged = { messageId, oldMessage, newUserReaction, oldUserReaction ->
            messagingDelegate._messages.update { current ->
                with(messagingDelegate) {
                    current.updateById(messageId) { msg ->
                        val userId = currentUserId ?: ""
                        val newUserReactions = msg.userReactions.toMutableMap()
                        if (newUserReaction != null && userId.isNotBlank()) {
                            newUserReactions[userId] = newUserReaction
                        } else if (userId.isNotBlank()) {
                            newUserReactions.remove(userId)
                        }
                        msg.withUserReactions(newUserReactions, currentUserId)
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
        _groupMemberNames = _groupMemberNames,
        _onlyAdminsCanMessage = _onlyAdminsCanMessage,
        _isCurrentUserAdmin = _isCurrentUserAdmin,
        _isParticipantActive = _isParticipantActive,
        _disappearingMode = _disappearingMode,
        getDisappearingModeUseCase = getDisappearingModeUseCase,
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
        setDisappearingModeUseCase = setDisappearingModeUseCase,
        viewModelScope = viewModelScope,
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

    fun onVisibilityChanged(visible: Boolean) {
        _isScreenVisible.value = visible
        if (visible) {
            currentChatId?.let {
                viewModelScope.launch {
                    markMessagesAsReadUseCase(it)
                }
            }
        }
    }

    fun initialize(chatId: String, participantId: String? = null) {
        cleanup()
        initializationDelegate.initialize(chatId, participantId, currentChatId)
    }

    fun refreshMessages() {
        val chatId = currentChatId ?: return
        _hasMoreMessages.value = true
        viewModelScope.launch {
            getMessagesUseCase(chatId, forceNetwork = true).onSuccess { messages ->
                messagingDelegate.setMessages(messages)
            }
        }
    }

    fun loadQuotedMessage(messageId: String) {
        if (messageId.isBlank()) return
        if (loadingQuotedMessageIds.putIfAbsent(messageId, Unit) != null) return
        viewModelScope.launch {
            try {
                if (messagingDelegate.messages.value.any { it.id == messageId }) return@launch
                getMessageByIdUseCase(messageId).onSuccess { original ->
                    if (original != null && original.chatId == currentChatId) {
                        messagingDelegate.addQuotedMessageToTimeline(original)
                    }
                }.onFailure { error ->
                    Napier.w("Failed to locate quoted chat message: ${error.message}", tag = "ChatViewModel")
                }
            } finally {
                loadingQuotedMessageIds.remove(messageId)
            }
        }
    }
    fun voteInPoll(messageId: String, optionId: String) {
        val uid = currentUserId ?: return
        val currentMessage = messagingDelegate.messages.value.find { it.id == messageId } ?: return
        val poll = currentMessage.metadataContainer?.poll ?: return
        if (poll.isClosed) {
            _error.value = "Poll is closed"
            return
        }

        viewModelScope.launch {
            val result = voteInPollUseCase(messageId, optionId, poll.pollId)
            if (result.isSuccess) {
                messagingDelegate._messages.update { current ->
                    with(messagingDelegate) {
                        current.updateById(messageId) { msg ->
                            val updatedPoll = msg.metadataContainer?.poll ?: return@updateById msg
                            val updatedOptions = updatedPoll.options.map { option ->
                                if (option.id == optionId) {
                                    val currentVoters = option.voterUserIds.toSet()
                                    val newVoters = if (uid in currentVoters) currentVoters - uid else currentVoters + uid
                                    option.copy(
                                        voteCount = newVoters.size,
                                        voterUserIds = newVoters.toList()
                                    )
                                } else if (!updatedPoll.allowMultipleAnswers) {
                                    val currentVoters = option.voterUserIds.toSet() - uid
                                    option.copy(
                                        voteCount = currentVoters.size,
                                        voterUserIds = currentVoters.toList()
                                    )
                                } else option
                            }
                            val container = msg.metadataContainer ?: return@updateById msg
                            val updatedContainer = container.copy(poll = updatedPoll.copy(options = updatedOptions))
                            msg.copy(metadataContainer = updatedContainer)
                        }
                    }
                }
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to vote"
            }
        }
    }

    fun restartSubscriptions() {
        val chatId = currentChatId ?: return
        subscriptionDelegate.restartSubscriptions(chatId)
    }

    fun markViewOnceConsumed(messageId: String) {
        viewModelScope.launch {
            markViewOnceConsumedUseCase(messageId).onSuccess {
                messagingDelegate._messages.update { current ->
                    with(messagingDelegate) {
                        current.updateById(messageId) { msg ->
                            val container = msg.metadataContainer ?: return@updateById msg
                            val updatedViewOnce = container.viewOnce?.copy(isConsumed = true, remainingViews = 0)
                            msg.copy(metadataContainer = container.copy(viewOnce = updatedViewOnce))
                        }
                    }
                }
            }
        }
    }

    fun loadMoreMessages() {
        val chatId = currentChatId ?: return
        if (_isLoadingMore.value || !_hasMoreMessages.value || !historyPaginationMutex.tryLock()) return
        val oldestMessage = messagingDelegate.oldestLoadedPageMessage
        if (oldestMessage == null) {
            historyPaginationMutex.unlock()
            return
        }
        val oldest = oldestMessage.createdAt
        val oldestId = oldestMessage.id
        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                getMessagesUseCase(chatId, before = oldest, beforeId = oldestId).onSuccess { older ->
                    if (currentChatId != chatId) return@onSuccess
                    if (older.isEmpty()) {
                        _hasMoreMessages.value = false
                    } else {
                        messagingDelegate.recordFetchedPage(older)
                        messagingDelegate._messages.update { current ->
                            (older + current).distinctBy { it.id }.sortedBy { it.createdAt }
                        }
                    }
                }.onFailure { e ->
                    if (currentChatId == chatId) _error.value = "Failed to load older messages: ${e.message}"
                }
            } finally {
                _isLoadingMore.value = false
                historyPaginationMutex.unlock()
            }
        }
    }
    private fun handleIncomingMessage(newMessage: Message) {
        Napier.d("Incoming realtime message: ${newMessage.id} from ${newMessage.senderId} in chat ${newMessage.chatId}. Content: ${newMessage.content.take(20)}...", tag = "ChatViewModel")

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
                        readBy = newMessage.readBy,
                        updatedAt = newMessage.updatedAt
                    )
                } else {
                    val contentChanged = existing.content != newMessage.content
                    newMessage.copy(
                        isEdited = if (contentChanged) newMessage.isEdited else existing.isEdited,
                        reactions = if (newMessage.reactions.isEmpty()) existing.reactions else newMessage.reactions,
                        userReaction = if (newMessage.reactions.isEmpty()) existing.userReaction else newMessage.userReaction
                    )
                }
                with(messagingDelegate) { current.replaceById(newMessage.id, mergedMessage) }
            } else {
                val tempId = messagingDelegate.pendingTempIds.value.firstOrNull { id ->
                    val tempMsg = current.find { it.id == id } ?: return@firstOrNull false
                    tempMsg.senderId == newMessage.senderId &&
                        (tempMsg.content == newMessage.content ||
                            newMessage.content in encryptedPlaceholders)
                }

                if (tempId != null && newMessage.senderId == currentUserId) {
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
                    (current + newMessage)
                        .distinctBy { it.id }
                        .sortedBy { it.createdAt }
                }
            }
        }
        aiDelegate.generateSmartReplies(messages.value)

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

    private fun handleIncomingReaction(reaction: MessageReaction) {
        val reactionType = ReactionType.values()
            .find { it.emoji == reaction.reactionEmoji } ?: return
        messagingDelegate._messages.update { current ->
            with(messagingDelegate) {
                current.updateById(reaction.messageId) { msg ->
                    val newUserReactions = msg.userReactions.toMutableMap()
                    if (reaction.isDelete) {
                        if (newUserReactions[reaction.userId] == reactionType) {
                            newUserReactions.remove(reaction.userId)
                        }
                    } else {
                        newUserReactions[reaction.userId] = reactionType
                    }
                    msg.withUserReactions(newUserReactions, currentUserId)
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

    fun retrySendMessage(message: Message) {
        messagingDelegate.retrySendMessage(message) { err ->
            _error.value = err
        }
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

    fun uploadVoiceMessage(
        audioFile: File,
        durationMs: Long = 0L,
        onResult: ((Boolean, String?) -> Unit)? = null
    ): Job {
        val replyToId = _replyingToMessage.value?.id
        return viewModelScope.launch {
            ensureActive()
            val storageConfig: com.synapse.social.studioasinc.shared.domain.model.StorageConfig
            try {
                storageConfig = getStorageConfigUseCase().first()
            } catch (e: Exception) {
                _error.value = "Storage config unavailable: ${e.message}"
                onResult?.invoke(false, "Storage config unavailable: ${e.message}")
                return@launch
            }
            mediaDelegate.uploadAndSendVoiceMessageSuspend(
                audioFile = audioFile,
                durationMs = durationMs,
                storageConfig = storageConfig,
                voiceUploadService = voiceUploadService,
                sendMessageUseCase = sendMessageUseCase,
                currentUserIdProvider = { currentUserId },
                chatIdProvider = { currentChatId },
                replyToId = replyToId,
                clearReplySelection = true,
                onResult = onResult
            )
        }
    }

    fun sendMediaMessage(mediaUrl: String, fileName: String, contentType: String, messageType: String, caption: String? = null) {
        mediaDelegate.sendMediaMessage(mediaUrl, fileName, contentType, messageType, caption)
    }

    fun uploadAndSendMedia(
        filePath: String,
        fileName: String,
        contentType: String,
        messageType: String,
        caption: String? = null,
        quality: com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality = com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality.STANDARD
    ) {
        mediaDelegate.uploadAndSendMedia(filePath, fileName, contentType, messageType, caption, quality)
    }

    fun uploadAndSendMultipleMedia(
        files: List<Pair<String, com.synapse.social.studioasinc.feature.shared.components.picker.PickedFile>>,
        caption: String? = null,
        quality: com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality = com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality.STANDARD
    ) {
        mediaDelegate.uploadAndSendMultipleMedia(files, caption, quality)
    }

    fun getFormattedTimestamp(timestamp: String?): String = TimestampFormatter.formatRelative(timestamp)

    private fun cleanup() {
        subscriptionDelegate.cleanup()
        inputDelegate.cleanup()
        messagingDelegate.pendingTempIds.value = emptySet()
        messagingDelegate.clearMessages()
        _hasMoreMessages.value = true
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
