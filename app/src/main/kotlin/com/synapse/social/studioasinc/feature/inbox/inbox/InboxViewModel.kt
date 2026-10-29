package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.feature.inbox.inbox.models.InboxFilter
import com.synapse.social.studioasinc.feature.inbox.inbox.models.InboxSearchCategory
import com.synapse.social.studioasinc.feature.inbox.inbox.models.InboxSearchResult
import com.synapse.social.studioasinc.feature.inbox.inbox.models.InboxSearchState
import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatFolder
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundEffect
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.InitializeE2EUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkIncomingMessageAsDeliveredUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsDeliveredUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToInboxUpdatesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.sound.PlaySoundEffectUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.SearchUsersUseCase
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

private val InboxUrlPattern = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val subscribeToInboxUpdatesUseCase: SubscribeToInboxUpdatesUseCase,
    private val markIncomingMessageAsDeliveredUseCase: MarkIncomingMessageAsDeliveredUseCase,
    private val markMessagesAsDeliveredUseCase: MarkMessagesAsDeliveredUseCase,
    private val initializeE2EUseCase: InitializeE2EUseCase,
    private val chatLockManager: com.synapse.social.studioasinc.core.util.ChatLockManager,
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val playSoundEffect: PlaySoundEffectUseCase
) : ViewModel() {
    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // A paused inbox means either a chat is open on top of it or the app is backgrounded; in the
    // first case the chat plays the arrival, in the second the push notification does.
    private var isScreenVisible = false

    private val _selectedFilter = MutableStateFlow(InboxFilter.ALL)
    val selectedFilter: StateFlow<InboxFilter> = _selectedFilter.asStateFlow()

    val chatListLayout: StateFlow<ChatListLayout> = settingsRepository.chatListLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatListLayout.DOUBLE_LINE)

    val chatSwipeGesture: StateFlow<ChatSwipeGesture> = settingsRepository.chatSwipeGesture
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatSwipeGesture.ARCHIVE)

    val chatFolders: StateFlow<List<ChatFolder>> = settingsRepository.chatFoldersJson
        .map { json ->
            if (json.isNullOrBlank()) emptyList()
            else try { Json.decodeFromString<List<ChatFolder>>(json) } catch (_: Exception) { emptyList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    val filteredConversations: StateFlow<List<Conversation>> = combine(
        _conversations,
        _selectedFolderId,
        chatFolders,
        _selectedFilter
    ) { conversations, folderId, folders, filter ->
        val inFolder = if (folderId == null) conversations else {
            folders.find { it.id == folderId }?.let { folder ->
                conversations.filter { it.chatId in folder.includedChatIds && it.chatId !in folder.excludedChatIds }
            } ?: conversations
        }
        inFolder.filter { conversation ->
            when (filter) {
                InboxFilter.ALL -> true
                InboxFilter.UNREAD -> conversation.unreadCount > 0
                // Supabase stores the pin flag but has no separate favorite field.
                InboxFilter.FAVORITES -> conversation.isPinned
                InboxFilter.GROUPS -> conversation.isGroup
            }
        }.sortedWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.lastMessageTime ?: "" })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _archivedConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val archivedConversations: StateFlow<List<Conversation>> = _archivedConversations.asStateFlow()

    private val _isLoadingArchived = MutableStateFlow(false)
    val isLoadingArchived: StateFlow<Boolean> = _isLoadingArchived.asStateFlow()

    private val _searchState = MutableStateFlow(InboxSearchState())
    val searchState: StateFlow<InboxSearchState> = _searchState.asStateFlow()

    private var inboxSubscriptionJob: Job? = null
    private var subscribedChatIds: List<String> = emptyList()
    private var searchJob: Job? = null

    init {
        observeAuthStatus()
        viewModelScope.launch {
            initializeE2EUseCase().onFailure { throwable ->
                _error.value = "Failed to initialize encrypted chat: ${throwable.message.orEmpty()}"
            }
            loadCurrentUserProfile()
            loadConversationsCacheFirst()
        }
    }

    private suspend fun loadCurrentUserProfile() {
        try {
            _currentUserProfile.value = UserProfileManager.getCurrentUserProfile()
        } catch (_: Exception) {
            _currentUserProfile.value = null
        }
    }

    fun loadConversations() {
        viewModelScope.launch { refreshConversations() }
    }

    private suspend fun loadConversationsCacheFirst() {
        val cached = chatRepository.getConversations(forceNetwork = false)
            .getOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            _conversations.value = cached.distinctBy { it.chatId }
                .sortedWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.lastMessageTime ?: "" })
            subscribeToInboxUpdates()
        }
        refreshConversations()
    }

    private suspend fun refreshConversations() {
        val hasCachedData = _conversations.value.isNotEmpty()
        _error.value = null
        if (!hasCachedData) {
            _isLoading.value = true
        } else {
            _isRefreshing.value = true
        }
        getConversationsUseCase(forceNetwork = true)
            .onSuccess { conversationList ->
                _conversations.value = conversationList.distinctBy { it.chatId }
                    .sortedWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.lastMessageTime ?: "" })
                _isLoading.value = false
                _isRefreshing.value = false
                subscribeToInboxUpdates()
                backfillDeliveryAcks(_conversations.value)
            }
            .onFailure { throwable ->
                _error.value = "Failed to load conversations: ${throwable.message.orEmpty()}"
                _isLoading.value = false
                _isRefreshing.value = false
            }
    }

    private fun observeAuthStatus() {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { status ->
                if (status == AuthSessionStatus.NOT_AUTHENTICATED) {
                    inboxSubscriptionJob?.cancel()
                    subscribedChatIds = emptyList()
                    _conversations.value = emptyList()
                    _archivedConversations.value = emptyList()
                    _currentUserProfile.value = null
                }
            }
        }
    }

    private fun subscribeToInboxUpdates() {
        val chatIds = _conversations.value.map { it.chatId }.distinct()
        if (chatIds == subscribedChatIds) return
        inboxSubscriptionJob?.cancel()
        subscribedChatIds = chatIds
        if (chatIds.isEmpty()) return
        inboxSubscriptionJob = viewModelScope.launch {
            while (isActive) {
                try {
                    subscribeToInboxUpdatesUseCase(chatIds)
                        .onEach { message ->
                            playArrivalCue(message)
                            // Acknowledge every arrival; the refresh debounce only coalesces the list reload.
                            acknowledgeDelivery(message)
                        }
                        .debounce(300L)
                        .collect {
                            refreshConversations()
                        }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    io.github.aakira.napier.Napier.w("Inbox realtime subscription failed, reconnecting in 2s: ${e.message}", tag = "InboxRealtime")
                    delay(2000)
                }
            }
        }
    }

    // Delivery marks every undelivered message of a chat, so one in-flight acknowledgement per chat is enough.
    private val pendingDeliveryAcks = mutableSetOf<String>()

    private fun acknowledgeDelivery(message: Message) {
        if (!pendingDeliveryAcks.add(message.chatId)) return
        viewModelScope.launch {
            try {
                markIncomingMessageAsDeliveredUseCase(message)
                    .onFailure {
                        io.github.aakira.napier.Napier.w("Inbox delivery acknowledgement failed: ${it.message}", tag = "InboxRealtime")
                    }
            } finally {
                pendingDeliveryAcks.remove(message.chatId)
            }
        }
    }

    // Listing a chat with unread incoming messages proves this device has them, even when the
    // realtime arrival event was missed because the app was closed. One ack per chat per session.
    private val backfilledDeliveryChats = mutableSetOf<String>()

    private fun backfillDeliveryAcks(conversations: List<Conversation>) {
        conversations.filter { it.unreadCount > 0 && it.chatId !in backfilledDeliveryChats }
            .forEach { conversation ->
                backfilledDeliveryChats.add(conversation.chatId)
                viewModelScope.launch {
                    markMessagesAsDeliveredUseCase(conversation.chatId)
                        .onFailure {
                            backfilledDeliveryChats.remove(conversation.chatId)
                            io.github.aakira.napier.Napier.w("Inbox delivery backfill failed: ${it.message}", tag = "InboxRealtime")
                        }
                }
            }
    }

    // A backgrounded process can lose the realtime socket without the collector ever seeing an
    // error, so returning to the foreground re-subscribes from scratch instead of trusting the
    // live job, and the refresh that follows re-acks arrivals the screen missed while away.
    fun onAppResumed() {
        inboxSubscriptionJob?.cancel()
        subscribedChatIds = emptyList()
        backfilledDeliveryChats.clear()
        viewModelScope.launch { refreshConversations() }
    }

    fun onVisibilityChanged(visible: Boolean) {
        isScreenVisible = visible
    }

    /**
     * Tones an arrival the user can actually see, if they have message tones on.
     *
     * Call log rows are excluded because ending a call already plays its own cue, and the
     * per-effect cooldown in [PlaySoundEffectUseCase] collapses a burst of arrivals — and the
     * same message an open chat is also hearing — into one sound.
     */
    private fun playArrivalCue(message: Message) {
        if (!isScreenVisible) return
        if (message.messageType == MessageType.CALL) return
        if (message.senderId == chatRepository.getCurrentUserId()) return
        viewModelScope.launch { playSoundEffect(SoundEffect.MessageIncoming) }
    }

    fun isChatLocked(chatId: String): Boolean = chatLockManager.isChatLocked(chatId)

    fun authenticateChat(
        activity: androidx.fragment.app.FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = chatLockManager.authenticate(activity, onSuccess, onError)

    fun selectFolder(folderId: String?) { _selectedFolderId.value = folderId }

    fun selectFilter(filter: InboxFilter) { _selectedFilter.value = filter }

    fun assignConversationsToFolder(chatIds: Set<String>, folderId: String) {
        viewModelScope.launch {
            val updated = chatFolders.value.map { folder ->
                if (folder.id == folderId) {
                    folder.copy(
                        includedChatIds = (folder.includedChatIds + chatIds).distinct(),
                        excludedChatIds = folder.excludedChatIds - chatIds
                    )
                } else {
                    folder.copy(
                        includedChatIds = folder.includedChatIds - chatIds,
                        excludedChatIds = (folder.excludedChatIds + chatIds).distinct()
                    )
                }
            }
            persistFolders(updated)
        }
    }

    fun createFolderAndAssign(name: String, chatIds: Set<String>) {
        val cleanName = name.trim()
        if (cleanName.isBlank() || chatIds.isEmpty()) return
        viewModelScope.launch {
            val newFolder = ChatFolder(id = UUID.randomUUID().toString(), name = cleanName, includedChatIds = chatIds.toList())
            persistFolders(chatFolders.value + newFolder)
            _selectedFolderId.value = newFolder.id
        }
    }

    private suspend fun persistFolders(folders: List<ChatFolder>) {
        try {
            settingsRepository.setChatFoldersJson(Json.encodeToString(folders))
        } catch (throwable: Exception) {
            _error.value = "Failed to save folder: ${throwable.message.orEmpty()}"
        }
    }

    fun setConversationsPinned(chatIds: Set<String>, isPinned: Boolean) {
        if (chatIds.isEmpty()) return
        viewModelScope.launch {
            var failures = 0
            chatIds.forEach { chatId ->
                chatRepository.updateConversationPinnedStatus(chatId, isPinned)
                    .onSuccess {
                        _conversations.value = _conversations.value.map { if (it.chatId == chatId) it.copy(isPinned = isPinned) else it }
                    }
                    .onFailure { failures++ }
            }
            if (failures > 0) _error.value = "Failed to update $failures conversation(s)."
        }
    }

    fun setConversationsMuted(chatIds: Set<String>, isMuted: Boolean) {
        if (chatIds.isEmpty()) return
        viewModelScope.launch {
            var failures = 0
            chatIds.forEach { chatId ->
                chatRepository.updateConversationMutedStatus(chatId, isMuted)
                    .onSuccess {
                        _conversations.value = _conversations.value.map { if (it.chatId == chatId) it.copy(isMuted = isMuted) else it }
                    }
                    .onFailure { failures++ }
            }
            if (failures > 0) _error.value = "Failed to update $failures conversation(s)."
        }
    }

    fun archiveConversations(chatIds: Set<String>, onArchived: (Set<String>) -> Unit = {}) {
        if (chatIds.isEmpty()) return
        viewModelScope.launch {
            val archived = mutableSetOf<String>()
            chatIds.forEach { chatId ->
                chatRepository.updateConversationArchiveStatus(chatId, true)
                    .onSuccess {
                        archived += chatId
                        _conversations.value = _conversations.value.filterNot { it.chatId == chatId }
                    }
            }
            val failures = chatIds.size - archived.size
            if (failures > 0) _error.value = "Failed to archive $failures conversation(s)."
            if (archived.isNotEmpty()) {
                loadArchivedConversations()
                onArchived(archived)
            }
        }
    }

    fun archiveConversation(chatId: String) = archiveConversations(setOf(chatId))

    fun deleteConversation(chatId: String) = deleteConversations(setOf(chatId))

    fun deleteConversations(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        viewModelScope.launch {
            var failures = 0
            chatIds.forEach { chatId ->
                chatRepository.deleteConversation(chatId)
                    .onSuccess { _conversations.value = _conversations.value.filterNot { it.chatId == chatId } }
                    .onFailure { failures++ }
            }
            if (failures > 0) {
                _error.value = "Failed to delete $failures conversation(s)."
            }
        }
    }

    fun loadArchivedConversations() {
        viewModelScope.launch {
            _isLoadingArchived.value = true
            chatRepository.getArchivedConversations()
                .onSuccess { _archivedConversations.value = it.sortedByDescending { item -> item.lastMessageTime ?: "" } }
                .onFailure { _error.value = "Failed to load archived conversations: ${it.message.orEmpty()}" }
            _isLoadingArchived.value = false
        }
    }

    fun unarchiveConversations(chatIds: Set<String>) {
        if (chatIds.isEmpty()) return
        viewModelScope.launch {
            val restored = mutableSetOf<String>()
            chatIds.forEach { chatId ->
                chatRepository.updateConversationArchiveStatus(chatId, false)
                    .onSuccess { restored += chatId }
            }
            val failures = chatIds.size - restored.size
            if (failures > 0) _error.value = "Failed to unarchive $failures conversation(s)."
            if (restored.isNotEmpty()) {
                _archivedConversations.value = _archivedConversations.value.filterNot { it.chatId in restored }
                refreshConversations()
            }
        }
    }

    fun clearError() { _error.value = null }

    fun searchInbox(query: String, category: InboxSearchCategory = _searchState.value.category) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.value = InboxSearchState(query = query, category = category)
            return
        }
        _searchState.value = InboxSearchState(query = query, category = category, isLoading = true)
        searchJob = viewModelScope.launch {
            delay(250)
            val matches = mutableListOf<InboxSearchResult>()
            var failedConversations = 0
            val currentUserId = chatRepository.getCurrentUserId()
            val searchTerm = query.trim()
            _conversations.value.forEach { conversation ->
                getMessagesUseCase(conversation.chatId, limit = 80)
                    .onSuccess { messages ->
                        messages.asSequence()
                            .filterNot { it.isDeleted }
                            .filter { message ->
                                val searchableText = buildString {
                                    append(message.content)
                                    message.mediaUrl?.let { append(' ').append(it) }
                                    message.attachments.forEach { append(' ').append(it.url).append(' ').append(it.type) }
                                }
                                searchableText.contains(searchTerm, ignoreCase = true) && matchesCategory(conversation, message, category, currentUserId)
                            }
                            .forEach { matches += InboxSearchResult(conversation, it) }
                    }
                    .onFailure { failedConversations++ }
            }
            val peopleResult = searchUsersUseCase(searchTerm)
            _searchState.value = InboxSearchState(
                query = query,
                category = category,
                isLoading = false,
                messages = matches.sortedByDescending { it.message.createdAt },
                people = peopleResult.getOrElse { emptyList() },
                failedConversationCount = failedConversations,
                contactsSearchFailed = peopleResult.isFailure
            )
        }
    }

    private fun matchesCategory(
        conversation: Conversation,
        message: Message,
        category: InboxSearchCategory,
        currentUserId: String?
    ): Boolean = when (category) {
        InboxSearchCategory.UNREAD -> conversation.unreadCount > 0 && (currentUserId == null || currentUserId !in message.readBy)
        InboxSearchCategory.PHOTOS -> message.messageType.name.contains("IMAGE", true) || message.messageType.name.contains("PHOTO", true) || message.attachments.any { it.type.name.contains("IMAGE", ignoreCase = true) }
        InboxSearchCategory.LINKS -> message.linkPreview != null || InboxUrlPattern.containsMatchIn(message.content)
        InboxSearchCategory.DOCS -> message.messageType.name.contains("FILE", true) || message.messageType.name.contains("DOCUMENT", true) || message.attachments.any { it.type.name.contains("FILE", ignoreCase = true) }
    }
}
