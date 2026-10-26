package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.util.UploadProgressManager
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper
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
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.usecase.chat.*
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserActiveStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceDetailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.UpdateCurrentChatUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.GetStorageConfigUseCase
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import com.synapse.social.studioasinc.shared.util.TimeProvider
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private const val MESSAGE_SYNC_PAGE_SIZE = 50

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
    private val observeUserPresenceDetailUseCase: ObserveUserPresenceDetailUseCase,
    private val updateCurrentChatUseCase: UpdateCurrentChatUseCase,
    private val setDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.SetDisappearingModeUseCase,
    private val getDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase,
    private val deleteExpiredMessagesUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteExpiredMessagesUseCase,
    private val voiceUploadService: VoiceUploadService,
    private val getStorageConfigUseCase: GetStorageConfigUseCase,
    private val voteInPollUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.VoteInPollUseCase,
    private val markViewOnceConsumedUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkViewOnceConsumedUseCase,
    private val mediaCompressor: com.synapse.social.studioasinc.shared.domain.service.MediaCompressor? = null,
    private val reconcileRealtimeMessageUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.ReconcileRealtimeMessageUseCase,
    private val chatRepository: ChatRepository,
    private val authRepository: com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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

    val typingIndicatorsEnabled = getChatSettingsUseCase.typingIndicatorsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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

    private val _resolvedChatId = MutableStateFlow<String?>(null)
    val resolvedChatId: StateFlow<String?> = _resolvedChatId.asStateFlow()
    private val loadingQuotedMessageIds = ConcurrentHashMap<String, Unit>()
    private val historyPaginationMutex = Mutex()
    private val activeMessageSyncs = ConcurrentHashMap<String, Deferred<Unit>>()
    private val emptyChatSyncCursors = ConcurrentHashMap<String, Pair<String, String>>()

    private val _currentUserId = MutableStateFlow<String?>(getCurrentUserIdUseCase())
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { status ->
                if (status == com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus.AUTHENTICATED) {
                    _currentUserId.value = getCurrentUserIdUseCase()
                }
            }
        }
    }

    private val messagingDelegate = ChatMessagingDelegate(
        sendMessageUseCase = sendMessageUseCase,
        editMessageUseCase = editMessageUseCase,
        deleteMessageUseCase = deleteMessageUseCase,
        deleteMessageForMeUseCase = deleteMessageForMeUseCase,
        bulkDeleteMessagesForMeUseCase = bulkDeleteMessagesForMeUseCase,
        populateMessageReactionsUseCase = populateMessageReactionsUseCase,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId.value },
        isScreenVisibleProvider = { _isScreenVisible.value }
    )

    private val aiDelegate = ChatAiDelegate(
        generateSmartRepliesUseCase = generateSmartRepliesUseCase,
        summarizeChatUseCase = summarizeChatUseCase,
        summarizeMessageUseCase = summarizeMessageUseCase,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId.value },
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
        currentUserIdProvider = { currentUserId.value },
        isScreenVisibleProvider = { _isScreenVisible.value },
        typingIndicatorsEnabledProvider = { typingIndicatorsEnabled.value },
        onNewMessage = { newMessage ->
            handleIncomingMessage(newMessage)
        },
        onReactionEvent = { reaction ->
            handleIncomingReaction(reaction)
        },
        onSyncRequired = {
            currentChatId?.let { chatId -> synchronizeMissedMessages(chatId) }
        },
        onCleanupTypingChannel = {
            currentChatId?.let { chatId -> chatRepository.releaseTypingChannel(chatId) }
        }
    )

    private val mediaDelegate = ChatMediaDelegate(
        uploadMediaUseCase = uploadMediaUseCase,
        sendMessageUseCase = sendMessageUseCase,
        fileUploader = fileUploader,
        uploadProgressManager = uploadProgressManager,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId.value },
        chatIdProvider = { currentChatId },
        onOptimisticMessageAdded = { msg, tempId ->
            messagingDelegate.pendingTempIds.update { it + tempId }
            messagingDelegate.markLiveInsert(msg.clientId)
            messagingDelegate._messages.update { current ->
                messagingDelegate.insertChronologically(current, msg)
            }
        },
        onOptimisticMessageUpdated = { tempId, content ->
            messagingDelegate._messages.update { current ->
                with(messagingDelegate) {
                    current.updateById(tempId) { it.copy(content = content) }
                }
            }
        },
        onOptimisticMessageSuccess = messagingDelegate::completeOptimisticSend,
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
                        val userId = currentUserId.value ?: ""
                        val newUserReactions = msg.userReactions.toMutableMap()
                        if (newUserReaction != null && userId.isNotBlank()) {
                            newUserReactions[userId] = newUserReaction
                        } else if (userId.isNotBlank()) {
                            newUserReactions.remove(userId)
                        }
msg.withUserReactions(newUserReactions, currentUserId.value)
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
        observeUserPresenceDetailUseCase = observeUserPresenceDetailUseCase,
        markMessagesAsReadUseCase = markMessagesAsReadUseCase,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId.value },
        messagingDelegate = messagingDelegate,
        subscriptionDelegate = subscriptionDelegate,
        aiDelegate = aiDelegate,
        _isLoading = _isLoading,
        _isRefreshing = _isRefreshing,
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
        currentChatIdProvider = { currentChatId },
        onChatIdResolved = { resolvedChatId ->
            currentChatId = resolvedChatId
            _resolvedChatId.value = resolvedChatId
            chatRepository.setActiveChatId(resolvedChatId)
        }
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
        typingIndicatorsEnabledProvider = { typingIndicatorsEnabled.value },
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
            currentChatId?.let { chatId ->
                subscriptionDelegate.queueReadReceipt(chatId)
                viewModelScope.launch { updateCurrentChatUseCase(chatId) }
                retryAllUndecryptableMessages(chatId)
            }
val uid = currentUserId.value
            if (!uid.isNullOrBlank()) {
                messagingDelegate.markAllIncomingAsRead(uid)
            }
        } else {
            messagingDelegate.clearPendingMessageEntrances()
            viewModelScope.launch {
                updateCurrentChatUseCase(null)
            }
        }
    }

    fun initialize(chatId: String, participantId: String? = null) {
        // Returning to a chat that this ViewModel already initialized keeps the loaded timeline
        // and metadata; only subscriptions and any messages missed while away need repairing.
        // This also avoids tearing down an in-flight E2EE initialization.
        val activeChatId = currentChatId
        if (chatId != "new" && activeChatId != null && chatId.equals(activeChatId, ignoreCase = true)) {
            _error.value = null
            chatRepository.setActiveChatId(chatId)
            subscriptionDelegate.startSubscriptions(chatId)
            // Always attempt E2EE re-initialization on resume to handle cases where the OS
            // tore down the crypto session while the screen was off
            viewModelScope.launch {
                initializeE2EUseCase().onSuccess {
                    if (currentChatId?.equals(chatId, ignoreCase = true) == true) {
                        _isE2EEReady.value = true
                        Napier.d("E2EE re-initialized on resume for chat $chatId", tag = "ChatViewModel")
                    }
                }.onFailure { e ->
                    // If re-init fails, don't unset the flag if it was already true
                    // (the existing session might still be valid)
                    if (!_isE2EEReady.value) {
                        Napier.e("E2EE initialization failed: ${e.message}", e, tag = "E2EE")
                    } else {
                        Napier.w("E2EE re-init failed but existing session may be valid: ${e.message}", tag = "E2EE")
                    }
                }
                // Opportunistic cleanup of expired messages when chat is opened
                deleteExpiredMessagesUseCase(chatId).onSuccess { deletedCount ->
                    if (deletedCount > 0) {
                        Napier.d("Deleted $deletedCount expired messages for chat $chatId", tag = "ChatViewModel")
                    }
                }.onFailure { e ->
                    Napier.w("Failed to delete expired messages: ${e.message}", e, tag = "ChatViewModel")
                }
            }
            refreshMessages()
            return
        }

        cleanup()
        initializationDelegate.initialize(chatId, participantId)
    }

    fun refreshMessages() {
        val chatId = currentChatId ?: return
        viewModelScope.launch { synchronizeMissedMessages(chatId) }
    }

    private suspend fun synchronizeMissedMessages(chatId: String) {
        val hasCachedMessages = !messagingDelegate.messages.value.isEmpty()
        if (hasCachedMessages) {
            _isRefreshing.value = true
        } else {
            _isLoading.value = true
        }
        val candidate = viewModelScope.async(start = CoroutineStart.LAZY) {
            fetchMissedMessages(chatId)
        }
        val existing = activeMessageSyncs.putIfAbsent(chatId, candidate)
        val sync = existing ?: candidate
        if (existing == null) {
            sync.invokeOnCompletion { activeMessageSyncs.remove(chatId, sync) }
        } else {
            candidate.cancel()
        }
        try {
            sync.start()
            sync.await()
        } finally {
            // A stale sync must not clear the loading state of the chat now on screen.
            if (currentChatId?.equals(chatId, ignoreCase = true) == true) {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchMissedMessages(chatId: String) {
        if (currentChatId?.equals(chatId, ignoreCase = true) != true) return
        val latestConfirmed = messagingDelegate.messages.value.lastOrNull {
            it.deliveryStatus != com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.SENDING &&
                it.deliveryStatus != com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.FAILED
        }
        var cursorCreatedAt: String
        var cursorId: String
        if (latestConfirmed != null) {
            emptyChatSyncCursors.remove(chatId)
            cursorCreatedAt = latestConfirmed.createdAt
            cursorId = latestConfirmed.id
        } else {
            val emptyCursor = emptyChatSyncCursors[chatId]
            if (emptyCursor == null) {
                // Start the empty-chat watermark before the fetch so messages that arrive
                // during the request remain inside the next reconciliation window.
                val syncStartedAt = TimeProvider.nowInstant().toString()
                val initialResult = getMessagesUseCase(chatId, forceNetwork = true)
                val initialMessages = initialResult.getOrNull()
                if (initialMessages == null) {
                    Napier.w("Initial delta sync failed: " + initialResult.exceptionOrNull()?.message, tag = "ChatViewModel")
                    return
                }
                if (currentChatId?.equals(chatId, ignoreCase = true) != true) return
                messagingDelegate.setMessages(initialMessages)
                val recoveredLatest = messagingDelegate.messages.value.lastOrNull {
                    it.deliveryStatus != com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.SENDING &&
                        it.deliveryStatus != com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.FAILED
                }
                if (recoveredLatest == null) {
                    emptyChatSyncCursors[chatId] = syncStartedAt to "00000000-0000-0000-0000-000000000000"
                }
                return
            }
            cursorCreatedAt = emptyCursor.first
            cursorId = emptyCursor.second
        }

        while (currentChatId?.equals(chatId, ignoreCase = true) == true) {
            val pageResult = getMessagesUseCase.after(chatId, cursorCreatedAt, cursorId, MESSAGE_SYNC_PAGE_SIZE)
            val page = pageResult.getOrNull()
            if (pageResult.isFailure) {
                Napier.w("Incremental message sync failed: " + pageResult.exceptionOrNull()?.message, tag = "ChatViewModel")
                return
            }
            if (page.isNullOrEmpty()) return
            if (currentChatId?.equals(chatId, ignoreCase = true) != true) return
            val nextCursor = page.last()
            if (compareSyncCursor(nextCursor, cursorCreatedAt, cursorId) <= 0) {
                Napier.w("Incremental message cursor did not advance", tag = "ChatViewModel")
                return
            }
            messagingDelegate.setMessages(page)
            cursorCreatedAt = nextCursor.createdAt
            cursorId = nextCursor.id
        }
    }

    private fun compareSyncCursor(message: Message, createdAt: String, id: String): Int {
        val timestampComparison = try {
            kotlinx.datetime.Instant.parse(message.createdAt).compareTo(kotlinx.datetime.Instant.parse(createdAt))
        } catch (_: Exception) {
            message.createdAt.compareTo(createdAt)
        }
        return if (timestampComparison != 0) timestampComparison else message.id.compareTo(id)
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
        val uid = currentUserId.value ?: return
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
                    if (currentChatId?.equals(chatId, ignoreCase = true) != true) return@onSuccess
                    if (older.isEmpty()) {
                        _hasMoreMessages.value = false
                    } else {
                        messagingDelegate.recordFetchedPage(older)
                        messagingDelegate._messages.update { current ->
                            messagingDelegate.mergeChronologically(current, older)
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

        if (currentChatId != null && !newMessage.chatId.equals(currentChatId, ignoreCase = true)) {
            Napier.d("Ignoring message for chat ${newMessage.chatId} while viewing $currentChatId", tag = "ChatViewModel")
            return
        }

        // Capture empty state BEFORE any updates — used below to trigger a full sync
        // when the first real-time message arrives in a chat with no loaded messages.
        val wasEmpty = messagingDelegate.messages.value.isEmpty()

        val incomingUndecryptable = ChatMessagingDelegate.isUndecryptable(newMessage)
        val currentSnapshot = messagingDelegate._messages.value
        val existing = currentSnapshot.firstOrNull { it.id == newMessage.id }

        // Only pair with a pending optimistic message if this incoming server message is NOT already present in the list.
        // If the server message is already present (e.g. completeOptimisticSend already reconciled it),
        // matching a pendingTempId would incorrectly grab and delete a SUBSEQUENT pending message.
        val matchingOptimisticMessage = if (newMessage.senderId == currentUserId.value && existing == null) {
            val pendingIds = messagingDelegate.pendingTempIds.value
            currentSnapshot.asSequence()
                .filter { it.id in pendingIds && it.id != newMessage.id }
                .filter { it.chatId.equals(newMessage.chatId, ignoreCase = true) && it.senderId == newMessage.senderId }
                .filter { it.messageType == newMessage.messageType && it.replyToId == newMessage.replyToId }
                .filter {
                    incomingUndecryptable ||
                        it.content.trim() == newMessage.content.trim() ||
                        ChatMessagingDelegate.isUndecryptable(it)
                }
                .minByOrNull { it.createdAt }
        } else {
            null
        }

        matchingOptimisticMessage?.let { optimistic ->
            messagingDelegate.pendingTempIds.update { it - optimistic.id }
        }

        val effectiveReadBy = if (newMessage.senderId != currentUserId.value && _isScreenVisible.value) {
            val uid = currentUserId.value ?: ""
            if (uid.isNotBlank() && uid !in newMessage.readBy) newMessage.readBy + uid else newMessage.readBy
        } else {
            newMessage.readBy
        }

        messagingDelegate._messages.update { current ->
            val existingInUpdate = current.firstOrNull { it.id == newMessage.id }
            val stableClientId = matchingOptimisticMessage?.clientId
                ?: existingInUpdate?.clientId
                ?: newMessage.clientId
            // A decrypted copy must never be replaced by an unreadable realtime payload.
            val existingReadable = existingInUpdate != null &&
                !ChatMessagingDelegate.isUndecryptable(existingInUpdate)
            val optimisticReadable = matchingOptimisticMessage != null &&
                !ChatMessagingDelegate.isUndecryptable(matchingOptimisticMessage)
            val keepExistingContent = incomingUndecryptable && existingReadable
            val keepOptimisticContent = incomingUndecryptable && !keepExistingContent && optimisticReadable
            val contentChanged = !incomingUndecryptable && existingInUpdate != null &&
                existingInUpdate.content != newMessage.content

            val mergedMessage = if (keepExistingContent) {
                existingInUpdate.copy(
                    deliveryStatus = newMessage.deliveryStatus,
                    readBy = effectiveReadBy,
                    updatedAt = newMessage.updatedAt,
                    clientId = stableClientId
                )
            } else {
                newMessage.copy(
                    content = if (keepOptimisticContent) {
                        matchingOptimisticMessage.content
                    } else {
                        newMessage.content
                    },
                    readBy = effectiveReadBy,
                    clientId = stableClientId,
                    isEdited = if (contentChanged) {
                        newMessage.isEdited
                    } else {
                        existingInUpdate?.isEdited ?: newMessage.isEdited
                    },
                    reactions = if (newMessage.reactions.isEmpty()) {
                        existingInUpdate?.reactions ?: newMessage.reactions
                    } else {
                        newMessage.reactions
                    },
                    userReaction = if (newMessage.reactions.isEmpty()) {
                        existingInUpdate?.userReaction ?: newMessage.userReaction
                    } else {
                        newMessage.userReaction
                    }
                )
            }

            val reconciledMessage = (existingInUpdate ?: matchingOptimisticMessage)
                ?.mergeMonotonic(mergedMessage)
                ?: mergedMessage
            val currentWithoutOptimistic = if (matchingOptimisticMessage != null && matchingOptimisticMessage.id != newMessage.id) {
                current.filterNot { it.id == matchingOptimisticMessage.id }
            } else {
                current
            }
            when {
                existingInUpdate != null -> messagingDelegate.replaceChronologically(currentWithoutOptimistic, newMessage.id, reconciledMessage)
                matchingOptimisticMessage != null -> messagingDelegate.replaceChronologically(currentWithoutOptimistic, matchingOptimisticMessage.id, reconciledMessage)
                else -> {
                    messagingDelegate.markLiveInsert(reconciledMessage.clientId)
                    messagingDelegate.insertChronologically(currentWithoutOptimistic, reconciledMessage)
                }
            }
        }
        aiDelegate.generateSmartReplies(messages.value)

        // If the chat was empty before this message arrived, trigger a full sync to establish
        // the synchronization cursor and fetch any history missed before the subscription started.
        // Fire-and-forget: message is already visible in the UI while the sync runs in the background.
        if (wasEmpty) {
            val chatId = currentChatId ?: newMessage.chatId
            viewModelScope.launch {
                try {
                    synchronizeMissedMessages(chatId)
                } catch (e: Exception) {
                    Napier.w("Initial sync on first empty-chat message failed: ${e.message}", tag = "ChatViewModel")
                }
            }
        }

        // Persist to cache with real-time priority so inbox refresh cannot overwrite this message.
        // Skip while the payload is unreadable — the decryption retry below reconciles once a
        // decrypted version is available.
        if (!incomingUndecryptable) {
            val messageForReconciliation = messagingDelegate._messages.value.firstOrNull { it.id == newMessage.id }
            if (messageForReconciliation != null) {
                val chatIdToReconcile = currentChatId ?: newMessage.chatId
                viewModelScope.launch {
                    try {
                        reconcileRealtimeMessageUseCase(chatIdToReconcile, messageForReconciliation)
                    } catch (e: Exception) {
                        Napier.w("Cache reconciliation failed for realtime message ${newMessage.id}: ${e.message}", tag = "ChatViewModel")
                    }
                }
            }
        }

        if (newMessage.senderId != currentUserId.value && incomingUndecryptable) {
            retryDecryptionWhenReady(newMessage)
        }
    }

    /**
     * Re-reads a message that arrived before its E2EE session was usable. Waiting on the
     * initialization signal (instead of fixed retry delays) repairs the message as soon as
     * decryption becomes possible, and leaves the fallback notice in place if it never does.
     */
    private fun retryDecryptionWhenReady(message: Message) {
        val chatId = currentChatId ?: message.chatId
        viewModelScope.launch {
            val ready = _isE2EEReady.value ||
                withTimeoutOrNull(10.seconds) { _isE2EEReady.first { it } } == true
            if (!ready) return@launch
            if (currentChatId?.equals(chatId, ignoreCase = true) != true) return@launch

            getMessageByIdUseCase(message.id).onSuccess { refreshed ->
                if (refreshed == null || ChatMessagingDelegate.isUndecryptable(refreshed)) return@onSuccess
                if (currentChatId?.equals(chatId, ignoreCase = true) != true) return@onSuccess
                messagingDelegate._messages.update { current ->
                    val existing = current.firstOrNull { it.id == message.id }
                    val toWrite = refreshed.copy(clientId = existing?.clientId ?: refreshed.clientId)
                    val merged = existing?.mergeMonotonic(toWrite) ?: toWrite
                    with(messagingDelegate) { current.replaceById(message.id, merged) }
                }
                val decrypted = messagingDelegate._messages.value.firstOrNull { it.id == message.id }
                    ?: return@onSuccess
                try {
                    reconcileRealtimeMessageUseCase(chatId, decrypted)
                } catch (e: Exception) {
                    Napier.w("Cache reconciliation failed for decrypted message ${message.id}: ${e.message}", tag = "ChatViewModel")
                }
            }.onFailure { error ->
                Napier.w("Decryption retry failed for message ${message.id}: ${error.message}", tag = "ChatViewModel")
            }
        }
    }

    /**
     * Re-attempts decryption for all currently undecryptable messages in the timeline.
     * This handles the case where messages arrived while the screen was off and
     * E2EE was not ready, leaving them stuck in an undecryptable state.
     */
    private fun retryAllUndecryptableMessages(chatId: String) {
        viewModelScope.launch {
            val ready = _isE2EEReady.value ||
                withTimeoutOrNull(15.seconds) { _isE2EEReady.first { it } } == true

            if (!ready) {
                Napier.w("E2EE not ready after 15s on resume, skipping decryption retry", tag = "ChatViewModel")
                return@launch
            }

            if (currentChatId?.equals(chatId, ignoreCase = true) != true) return@launch

            val undecryptableMessages = messagingDelegate.messages.value
                .filter { ChatMessagingDelegate.isUndecryptable(it) && it.senderId != currentUserId.value }
                .takeLast(10)

            if (undecryptableMessages.isEmpty()) return@launch

            Napier.d("Retrying decryption for ${undecryptableMessages.size} messages on resume", tag = "ChatViewModel")

            undecryptableMessages.forEach { message ->
                getMessageByIdUseCase(message.id).onSuccess { refreshed ->
                    if (refreshed == null || ChatMessagingDelegate.isUndecryptable(refreshed)) return@onSuccess
                    if (currentChatId?.equals(chatId, ignoreCase = true) != true) return@onSuccess

                    messagingDelegate._messages.update { current ->
                        val existing = current.firstOrNull { it.id == message.id }
                        val toWrite = refreshed.copy(clientId = existing?.clientId ?: refreshed.clientId)
                        val merged = existing?.mergeMonotonic(toWrite) ?: toWrite
                        with(messagingDelegate) { current.replaceById(message.id, merged) }
                    }

                    val decrypted = messagingDelegate._messages.value.firstOrNull { it.id == message.id }
                        ?: return@onSuccess
                    try {
                        reconcileRealtimeMessageUseCase(chatId, decrypted)
                    } catch (e: Exception) {
                        Napier.w("Cache reconciliation failed for decrypted message ${message.id}: ${e.message}", tag = "ChatViewModel")
                    }
                }.onFailure { error ->
                    Napier.w("Decryption retry failed for message ${message.id}: ${error.message}", tag = "ChatViewModel")
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
                    msg.withUserReactions(newUserReactions, currentUserId.value)
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

    fun deleteMessage(message: Message) {
        messagingDelegate.deleteMessage(message.id)
    }

    fun deleteMessageForMe(messageId: String) {
        messagingDelegate.deleteMessageForMe(messageId)
    }

    fun deleteMessageForMe(message: Message) {
        messagingDelegate.deleteMessageForMe(message.id)
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
currentUserIdProvider = { currentUserId.value },
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
        // Invalidate initialization first so any suspended fetch that resumes later cannot
        // write its result into the chat that is about to be opened.
        initializationDelegate.cancelInitialization()
        subscriptionDelegate.cleanup()
        aiDelegate.cancelPendingSmartReplies()
        inputDelegate.cleanup()
        activeMessageSyncs.values.forEach { it.cancel() }
        activeMessageSyncs.clear()
        emptyChatSyncCursors.clear()
        messagingDelegate.pendingTempIds.value = emptySet()
        messagingDelegate.clearMessages()
        currentChatId = null
        _resolvedChatId.value = null
        _hasMoreMessages.value = true
        _isLoadingMore.value = false
        _isLoading.value = false
        _isRefreshing.value = false
        _error.value = null
        chatRepository.setActiveChatId(null)
        viewModelScope.launch {
            updateCurrentChatUseCase(null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
