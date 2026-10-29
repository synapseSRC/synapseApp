package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.util.UploadProgressManager
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatUiState
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.mergeMonotonic
import com.synapse.social.studioasinc.shared.domain.usecase.chat.PopulateMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ToggleMessageReactionUseCase
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundEffect
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.usecase.chat.*
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserActiveStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.sound.PlaySoundEffectUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceDetailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.UpdateCurrentChatUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.GetStorageConfigUseCase
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import com.synapse.social.studioasinc.shared.util.TimeProvider
import com.synapse.social.studioasinc.shared.domain.util.EncryptedPayloadDetector
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
    private val dependencies: ChatDependencies,
    private val playSoundEffect: PlaySoundEffectUseCase
) : ViewModel() {

    // Extracted individual dependencies from ChatDependencies
    private val getMessagesUseCase: GetMessagesUseCase = dependencies.subscription.getMessagesUseCase
private val messageSyncUseCase: MessageSyncUseCase = MessageSyncUseCase(getMessagesUseCase)
    private val sendMessageUseCase: SendMessageUseCase = dependencies.messaging.sendMessageUseCase
    private val subscribeToMessagesUseCase: SubscribeToMessagesUseCase = dependencies.subscription.subscribeToMessagesUseCase
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase = dependencies.subscription.markMessagesAsReadUseCase
    private val markMessagesAsDeliveredUseCase: MarkMessagesAsDeliveredUseCase = dependencies.subscription.markMessagesAsDeliveredUseCase
    private val broadcastTypingStatusUseCase: BroadcastTypingStatusUseCase = 
        com.synapse.social.studioasinc.shared.domain.usecase.chat.BroadcastTypingStatusUseCase(dependencies.messaging.chatRepository)
    private val subscribeToTypingStatusUseCase: SubscribeToTypingStatusUseCase = dependencies.subscription.subscribeToTypingStatusUseCase
    private val editMessageUseCase: EditMessageUseCase = dependencies.messaging.editMessageUseCase
    private val deleteMessageUseCase: DeleteMessageUseCase = dependencies.messaging.deleteMessageUseCase
    private val deleteMessageForMeUseCase: DeleteMessageForMeUseCase = dependencies.messaging.deleteMessageForMeUseCase
    private val bulkDeleteMessagesUseCase: BulkDeleteMessagesUseCase = dependencies.messaging.bulkDeleteMessagesUseCase
    private val bulkDeleteMessagesForMeUseCase: BulkDeleteMessagesForMeUseCase = dependencies.messaging.bulkDeleteMessagesForMeUseCase
    private val uploadMediaUseCase: UploadMediaUseCase = dependencies.media.uploadMediaUseCase
    private val getUserProfileUseCase: GetUserProfileUseCase = dependencies.ai.getUserProfileUseCase
    private val initializeE2EUseCase: InitializeE2EUseCase = dependencies.initializeE2EUseCase
    private val getCurrentUserIdUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetCurrentUserIdUseCase = dependencies.messaging.currentUserIdUseCase
    private val getOrCreateChatUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetOrCreateChatUseCase = dependencies.getOrCreateChatUseCase
    private val getChatInfoUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatInfoUseCase = dependencies.getChatInfoUseCase
    private val getGroupMembersUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetGroupMembersUseCase = dependencies.getGroupMembersUseCase
    private val getMessageByIdUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessageByIdUseCase = dependencies.getMessageByIdUseCase
    private val chatLockManager: com.synapse.social.studioasinc.core.util.ChatLockManager = dependencies.settings.chatLockManager
    private val generateSmartRepliesUseCase: com.synapse.social.studioasinc.shared.domain.usecase.ai.GenerateSmartRepliesUseCase = dependencies.ai.generateSmartRepliesUseCase
    private val summarizeChatUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizeChatUseCase = dependencies.ai.summarizeChatUseCase
    private val summarizeMessageUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizeMessageUseCase = dependencies.ai.summarizeMessageUseCase
    private val uploadProgressManager: UploadProgressManager = dependencies.media.uploadProgressManager
    private val fileUploader: FileUploader = dependencies.media.fileUploader
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase = dependencies.toggleMessageReactionUseCase
    private val populateMessageReactionsUseCase: PopulateMessageReactionsUseCase = dependencies.messaging.populateMessageReactionsUseCase
    private val subscribeToMessageReactionsUseCase: SubscribeToMessageReactionsUseCase = dependencies.subscription.subscribeToMessageReactionsUseCase
    private val getChatSettingsUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatSettingsUseCase = dependencies.settings.getChatSettingsUseCase
    private val observeUserActiveStatusUseCase: ObserveUserActiveStatusUseCase = dependencies.observeUserActiveStatusUseCase
    private val observeUserPresenceDetailUseCase: ObserveUserPresenceDetailUseCase = dependencies.observeUserPresenceDetailUseCase
    private val updateCurrentChatUseCase: UpdateCurrentChatUseCase = dependencies.updateCurrentChatUseCase
    private val setDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.SetDisappearingModeUseCase = dependencies.settings.setDisappearingModeUseCase
    private val getDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase = dependencies.settings.getDisappearingModeUseCase
    private val deleteExpiredMessagesUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteExpiredMessagesUseCase = dependencies.settings.deleteExpiredMessagesUseCase
    private val voiceUploadService: VoiceUploadService = dependencies.media.voiceUploadService
    private val getStorageConfigUseCase: GetStorageConfigUseCase = dependencies.media.getStorageConfigUseCase
    private val voteInPollUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.VoteInPollUseCase = dependencies.voteInPollUseCase
    private val markViewOnceConsumedUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkViewOnceConsumedUseCase = dependencies.markViewOnceConsumedUseCase
    private val mediaCompressor: com.synapse.social.studioasinc.shared.domain.service.MediaCompressor? = dependencies.media.mediaCompressor
    private val reconcileRealtimeMessageUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.ReconcileRealtimeMessageUseCase = dependencies.messaging.reconcileRealtimeMessageUseCase
    private val chatRepository: ChatRepository = dependencies.messaging.chatRepository
    private val authRepository: com.synapse.social.studioasinc.shared.domain.repository.AuthRepository = dependencies.messaging.authRepository
    private val getConversationMuteStatusUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationMuteStatusUseCase = dependencies.settings.getConversationMuteStatusUseCase
    private val setConversationMuteStatusUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.SetConversationMuteStatusUseCase = dependencies.settings.setConversationMuteStatusUseCase
    private val searchMessagesUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.SearchMessagesUseCase = dependencies.searchMessagesUseCase
    private val exportChatUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.ExportChatUseCase = dependencies.exportChatUseCase

    // ===== ChatUiState: consolidated UI state =====
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
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

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isMuteUpdating = MutableStateFlow(false)
    val isMuteUpdating: StateFlow<Boolean> = _isMuteUpdating.asStateFlow()

    private val _searchResults = MutableStateFlow<List<com.synapse.social.studioasinc.shared.domain.model.chat.Message>>(emptyList())
    val searchResults: StateFlow<List<com.synapse.social.studioasinc.shared.domain.model.chat.Message>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val messageSuggestionEnabled = getChatSettingsUseCase.messageSuggestionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val chatAvatarDisabled = getChatSettingsUseCase.chatAvatarDisabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val typingIndicatorsEnabled = getChatSettingsUseCase.typingIndicatorsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Eagerly shared: the receipt flush reads .value without a collector, so a lazy share would pin the default.
    private val readReceiptsEnabled = getChatSettingsUseCase.readReceiptsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

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

    private val _currentUserId = MutableStateFlow<String?>(getCurrentUserIdUseCase())
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private var currentChatId: String? = null

    private val _resolvedChatId = MutableStateFlow<String?>(null)
    val resolvedChatId: StateFlow<String?> = _resolvedChatId.asStateFlow()
    private val loadingQuotedMessageIds = ConcurrentHashMap<String, Unit>()
    private val historyPaginationMutex = Mutex()
    private val activeMessageSyncs = ConcurrentHashMap<String, Deferred<Unit>>()
    private val emptyChatSyncCursors = ConcurrentHashMap<String, Pair<String, String>>()

    private fun updateUiState() {
        _uiState.value = ChatUiState(
            messages = messagingDelegate.messages.value,
            searchResults = _searchResults.value,
            inputText = _inputText.value,
            isLoading = _isLoading.value,
            isRefreshing = _isRefreshing.value,
            isLoadingMore = _isLoadingMore.value,
            hasMoreMessages = _hasMoreMessages.value,
            isSearching = _isSearching.value,
            editingMessage = _editingMessage.value,
            replyingToMessage = _replyingToMessage.value,
            selectedMessageIds = _selectedMessageIds.value,
            disappearingMode = _disappearingMode.value,
            error = _error.value,
            toastMessage = _toastMessage.value,
            isE2EEReady = _isE2EEReady.value,
            isScreenVisible = _isScreenVisible.value,
            currentUserId = _currentUserId.value,
            resolvedChatId = _resolvedChatId.value,
            participantProfile = _participantProfile.value,
            isGroupChat = _isGroupChat.value,
            onlyAdminsCanMessage = _onlyAdminsCanMessage.value,
            currentUserIsAdmin = _isCurrentUserAdmin.value,
            isParticipantActive = _isParticipantActive.value,
            isMuted = _isMuted.value,
            isMuteUpdating = _isMuteUpdating.value,
            canSendMessage = canSendMessage.value,
            chatWallpaperType = chatWallpaperType.value,
            chatWallpaperValue = chatWallpaperValue.value,
            chatWallpaperBlur = chatWallpaperBlur.value,
            chatFontScale = chatFontScale.value,
            chatThemePreset = chatThemePreset.value,
            chatMessageCornerRadius = chatMessageCornerRadius.value,
            chatMaxMessageChunkSize = chatMaxMessageChunkSize.value,
            messageSuggestionEnabled = messageSuggestionEnabled.value,
            chatAvatarDisabled = chatAvatarDisabled.value,
            typingIndicatorsEnabled = typingIndicatorsEnabled.value,
            isSummarizingMessage = aiDelegate.isSummarizingMessage.value,
        )
    }

    // Delegates
    private val messagingDelegate = ChatMessagingDelegate(
        sendMessageUseCase = sendMessageUseCase,
        editMessageUseCase = editMessageUseCase,
        deleteMessageUseCase = deleteMessageUseCase,
        deleteMessageForMeUseCase = deleteMessageForMeUseCase,
        bulkDeleteMessagesUseCase = bulkDeleteMessagesUseCase,
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

    private val chatE2EDelegate = ChatE2EDelegate(
        getMessageByIdUseCase = getMessageByIdUseCase,
        reconcileRealtimeMessageUseCase = reconcileRealtimeMessageUseCase,
        messagingDelegate = messagingDelegate,
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId.value },
        currentChatIdProvider = { currentChatId },
        isE2EEReady = _isE2EEReady
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
        readReceiptsEnabledProvider = { readReceiptsEnabled.value },
        onNewMessage = { newMessage ->
            handleIncomingMessage(newMessage)
        },
        onReactionEvent = { reaction ->
            handleIncomingReaction(reaction)
        },
        onSyncRequired = { chatId ->
            if (currentChatId?.equals(chatId, ignoreCase = true) == true) {
                synchronizeMissedMessages(chatId)
            }
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
        viewModelScope = viewModelScope,
        currentUserIdProvider = { currentUserId.value },
        currentChatIdProvider = { currentChatId },
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
        _isMuted = _isMuted,
        getConversationMuteStatusUseCase = getConversationMuteStatusUseCase,
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
        isScreenVisibleProvider = { _isScreenVisible.value },
        onSendDispatched = { playMessageCue(SoundEffect.MessageOutgoing) }
    )

    private val settingsDelegate = ChatSettingsDelegate(
        chatLockManager = chatLockManager,
        setDisappearingModeUseCase = setDisappearingModeUseCase,
        viewModelScope = viewModelScope,
        _disappearingMode = _disappearingMode,
        currentChatIdProvider = { currentChatId }
    )

    init {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { status ->
                if (status == com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus.AUTHENTICATED) {
                    _currentUserId.value = getCurrentUserIdUseCase()
                    updateUiState()
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            typingIndicatorsEnabled.collect { enabled ->
                if (!enabled) {
                    inputDelegate.stopTyping()
                    subscriptionDelegate.clearTypingStatus()
                }
                updateUiState()
            }
        }
    }

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
                chatE2EDelegate.retryAllUndecryptableMessages(chatId)
            }
            val uid = currentUserId.value
            if (!uid.isNullOrBlank()) {
                messagingDelegate.markAllIncomingAsRead(uid)
            }
        } else {
            inputDelegate.onVisibilityChanged(false)
            subscriptionDelegate.clearTypingStatus()
            messagingDelegate.clearPendingMessageEntrances()
            viewModelScope.launch {
                updateCurrentChatUseCase(null)
            }
        }
        if (visible) inputDelegate.onVisibilityChanged(true)
        updateUiState()
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
                    Napier.w("Failed to delete expired messages: ${e.message}", tag = "ChatViewModel")
                }
            }
            refreshMessages()
            updateUiState()
            return
        }

        cleanup()
        initializationDelegate.initialize(chatId, participantId)
        updateUiState()
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

        val lastConfirmed = messagingDelegate.messages.value.lastOrNull {
            it.deliveryStatus != com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.SENDING &&
                it.deliveryStatus != com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.FAILED
        }

        val candidate = viewModelScope.async(start = CoroutineStart.LAZY) {
            // Refresh outgoing messages
            val outgoingIds = messagingDelegate.messages.value.filter {
                it.senderId == currentUserId.value &&
                    it.deliveryStatus in setOf(
                        com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.SENT,
                        com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.DELIVERED
                    )
            }.map { it.id }
            if (outgoingIds.isNotEmpty()) {
                getMessagesUseCase.byIds(chatId, outgoingIds).onSuccess { refreshed ->
                    if (currentChatId?.equals(chatId, ignoreCase = true) == true) {
                        messagingDelegate.setMessages(refreshed)
                    }
                }.onFailure { Napier.w("Receipt reconciliation failed", it, tag = "ChatViewModel") }
            }

            // Sync missed messages via use case
            val syncResult = messageSyncUseCase.syncMissedMessages(chatId, lastConfirmed)
            if (syncResult.isSuccess) {
                val newMessages = syncResult.getOrDefault(emptyList())
                if (newMessages.isNotEmpty() && currentChatId?.equals(chatId, ignoreCase = true) == true) {
                    messagingDelegate._messages.update { current ->
                        messagingDelegate.mergeChronologically(current, newMessages)
                    }
                }
            } else {
                Napier.w("Message sync failed: " + syncResult.exceptionOrNull()?.message, tag = "ChatViewModel")
            }

            if (currentChatId?.equals(chatId, ignoreCase = true) == true) {
                subscriptionDelegate.enqueueIncomingReceipt(chatId)
            }
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
            if (currentChatId?.equals(chatId, ignoreCase = true) == true) {
                _isLoading.value = false
                _isRefreshing.value = false
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
        Napier.d("Incoming realtime message: ${newMessage.id} in chat ${newMessage.chatId}, status=${newMessage.deliveryStatus}", tag = "ChatViewModel")

        if (currentChatId != null && !newMessage.chatId.equals(currentChatId, ignoreCase = true)) {
            Napier.d("Ignoring message for chat ${newMessage.chatId} while viewing $currentChatId", tag = "ChatViewModel")
            return
        }

        // Capture empty state BEFORE any updates — used below to trigger a full sync
        // when the first real-time message arrives in a chat with no loaded messages.
        val wasEmpty = messagingDelegate.messages.value.isEmpty()

        val incomingUndecryptable = ChatE2EDelegate.isUndecryptable(newMessage)
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
                        ChatE2EDelegate.isUndecryptable(it)
                }
                .minByOrNull { it.createdAt }
        } else {
            null
        }

        matchingOptimisticMessage?.let { optimistic ->
            messagingDelegate.pendingTempIds.update { it - optimistic.id }
        }

        // A cue only for what genuinely arrived as somebody else's message. Three things would otherwise
        // make noise that describes nothing: the realtime stream reports the same row as an insert and
        // then again as an update; this device's own sends come back down the same stream, so the sender
        // would hear an incoming blip for a message they just heard the outgoing one for; and a call that
        // ended writes its own log row into this chat, which voip_end has already announced.
        val isFreshIncomingMessage = existing == null &&
            matchingOptimisticMessage == null &&
            newMessage.senderId != currentUserId.value &&
            newMessage.messageType != MessageType.CALL &&
            !incomingUndecryptable

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
                !ChatE2EDelegate.isUndecryptable(existingInUpdate)
            val optimisticReadable = matchingOptimisticMessage != null &&
                !ChatE2EDelegate.isUndecryptable(matchingOptimisticMessage)
            val keepExistingContent = incomingUndecryptable && existingReadable
            val keepOptimisticContent = incomingUndecryptable && !keepExistingContent && optimisticReadable
            val contentChanged = !incomingUndecryptable && existingInUpdate != null &&
                existingInUpdate.content != newMessage.content

            val mergedMessage = if (keepExistingContent) {
                existingInUpdate.copy(
                    deliveryStatus = newMessage.deliveryStatus,
                    readBy = effectiveReadBy,
                    sentAt = newMessage.sentAt ?: existingInUpdate.sentAt,
                    deliveredAt = newMessage.deliveredAt ?: existingInUpdate.deliveredAt,
                    readAt = newMessage.readAt ?: existingInUpdate.readAt,
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
        if (isFreshIncomingMessage) playMessageCue(SoundEffect.MessageIncoming)

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
            chatE2EDelegate.retryDecryptionWhenReady(newMessage)
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

    /**
     * Cues are foreground-only: a backgrounded app already carries the message in a push, and a cue
     * for something the user cannot see is a noise nobody asked for. What the sound switches allow
     * is decided in `PlaySoundEffectUseCase`, not here.
     */
    private fun playMessageCue(effect: SoundEffect) {
        if (!_isScreenVisible.value) return
        viewModelScope.launch { playSoundEffect(effect) }
    }

    fun playRecordingCue() = playMessageCue(SoundEffect.RecordingStarted)

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

    fun deleteSelectedMessagesForMe() {
        inputDelegate.deleteSelectedMessagesForMe()
    }

    fun deleteSelectedMessagesForEveryone() {
        inputDelegate.deleteSelectedMessagesForEveryone()
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

    fun toggleMuteNotifications() {
        val chatId = currentChatId ?: return
        if (_isMuteUpdating.value) return
        val newMutedState = !_isMuted.value
        _isMuteUpdating.value = true
        viewModelScope.launch {
            setConversationMuteStatusUseCase(chatId, newMutedState)
                .onSuccess {
                    _isMuted.value = newMutedState
                    _isMuteUpdating.value = false
                }
                .onFailure {
                    _isMuteUpdating.value = false
                    _error.value = "Failed to update mute status"
                }
        }
    }

    fun searchMessages(chatId: String, query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            searchMessagesUseCase(chatId, query)
                .onSuccess { results ->
                    _searchResults.value = results
                    _isSearching.value = false
                }
                .onFailure {
                    _error.value = "Failed to search messages"
                    _isSearching.value = false
                }
        }
    }

    fun exportChat(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            exportChatUseCase(chatId)
                .onSuccess { formattedText ->
                    onSuccess(formattedText)
                }
                .onFailure {
                    onError("Failed to export chat")
                }
        }
    }

    fun createChatShortcut(chatName: String, avatarUrl: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // This will be implemented with platform-specific code in the Activity
        // For now, just call the callbacks
        onSuccess()
    }

    fun addToFolder(chatId: String, folderId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // TODO: Implement folder assignment logic with SettingsRepository
        onSuccess()
    }

    fun uploadVoiceMessage(
        audioFile: File,
        durationMs: Long = 0L,
        onResult: ((Boolean, String?) -> Unit)? = null
    ): Job {
        val replyToId = _replyingToMessage.value?.id
        inputDelegate.stopTyping()
        playMessageCue(SoundEffect.MessageOutgoing)
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
        inputDelegate.stopTyping()
        playMessageCue(SoundEffect.MessageOutgoing)
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
        inputDelegate.stopTyping()
        playMessageCue(SoundEffect.MessageOutgoing)
        mediaDelegate.uploadAndSendMedia(filePath, fileName, contentType, messageType, caption, quality)
    }

    fun uploadAndSendMultipleMedia(
        files: List<Pair<String, com.synapse.social.studioasinc.feature.shared.components.picker.PickedFile>>,
        caption: String? = null,
        quality: com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality = com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality.STANDARD
    ) {
        inputDelegate.stopTyping()
        playMessageCue(SoundEffect.MessageOutgoing)
        mediaDelegate.uploadAndSendMultipleMedia(files, caption, quality)
    }

    fun getFormattedTimestamp(timestamp: String?): String = TimestampFormatter.formatRelative(timestamp)

    private fun cleanup() {
        // Invalidate initialization first so any suspended fetch that resumes later cannot
        // write its result into the chat that is about to be opened.
        initializationDelegate.cleanup()
        subscriptionDelegate.cleanup()
        aiDelegate.cleanup()
        inputDelegate.cleanup()
        messagingDelegate.cleanup()
        mediaDelegate.cleanup()
        chatE2EDelegate.cleanup()
        activeMessageSyncs.values.forEach { it.cancel() }
        activeMessageSyncs.clear()
        emptyChatSyncCursors.clear()
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
        updateUiState()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
