package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatFolder
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.InitializeE2EUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsDeliveredUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToInboxUpdatesUseCase
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val subscribeToInboxUpdatesUseCase: SubscribeToInboxUpdatesUseCase,
    private val initializeE2EUseCase: InitializeE2EUseCase,
    private val markMessagesAsDeliveredUseCase: MarkMessagesAsDeliveredUseCase,
    private val chatLockManager: com.synapse.social.studioasinc.core.util.ChatLockManager,
    private val settingsRepository: SettingsRepository,
    private val chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
) : ViewModel() {

    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val chatListLayout: StateFlow<ChatListLayout> = settingsRepository.chatListLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatListLayout.DOUBLE_LINE)

    val chatSwipeGesture: StateFlow<ChatSwipeGesture> = settingsRepository.chatSwipeGesture
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatSwipeGesture.ARCHIVE)

    // --- Folder state ---
    val chatFolders: StateFlow<List<ChatFolder>> = settingsRepository.chatFoldersJson
        .map { json ->
            if (json.isNullOrBlank()) emptyList()
            else try { Json.decodeFromString<List<ChatFolder>>(json) } catch (_: Exception) { emptyList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    val filteredConversations: StateFlow<List<Conversation>> =
        combine(_conversations, _selectedFolderId, chatFolders) { convos, folderId, folders ->
            if (folderId == null) convos
            else folders.find { it.id == folderId }
                ?.let { f -> convos.filter { it.chatId in f.includedChatIds } }
                ?: convos
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFolder(folderId: String?) { _selectedFolderId.value = folderId }

    fun assignConversationToFolder(chatId: String, folderId: String) {
        viewModelScope.launch {
            val updated = chatFolders.value.map { folder ->
                if (folder.id == folderId)
                    folder.copy(includedChatIds = (folder.includedChatIds + chatId).distinct())
                else
                    folder.copy(includedChatIds = folder.includedChatIds - chatId)
            }
            settingsRepository.setChatFoldersJson(Json.encodeToString(updated))
        }
    }

    init {
        viewModelScope.launch {
            initializeE2EUseCase().onFailure { e ->
                _error.value = "Failed to initialize E2EE: ${e.message}"
            }
            loadCurrentUserProfile()
            loadConversations()
            subscribeToInboxUpdates()
        }
    }

    private suspend fun loadCurrentUserProfile() {
        try { _currentUserProfile.value = UserProfileManager.getCurrentUserProfile() } catch (_: Exception) { }
    }

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            getConversationsUseCase().onSuccess { conversationList ->
                _conversations.value = conversationList.distinctBy { it.chatId }
                _isLoading.value = false
                
                // Mark delivered for anything unread
                conversationList.filter { it.unreadCount > 0 }.forEach { convo ->
                    markMessagesAsDeliveredUseCase(convo.chatId)
                }
            }.onFailure { e ->
                _error.value = "Failed to load conversations: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private var inboxSubscriptionJob: kotlinx.coroutines.Job? = null

    private fun subscribeToInboxUpdates() {
        if (inboxSubscriptionJob?.isActive == true) return
        val chatIds = _conversations.value.map { it.chatId }
        inboxSubscriptionJob = viewModelScope.launch {
            subscribeToInboxUpdatesUseCase(chatIds).collect {
                // When an update comes, we just load conversations.
                // loadConversations() no longer calls subscribeToInboxUpdates(), breaking the loop.
                loadConversations()
            }
        }
    }

    fun isChatLocked(chatId: String): Boolean = chatLockManager.isChatLocked(chatId)

    fun authenticateChat(
        activity: androidx.fragment.app.FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = chatLockManager.authenticate(activity, onSuccess, onError)

    fun getFormattedTimestamp(timestamp: String?): String = TimestampFormatter.formatRelative(timestamp)

    fun archiveConversation(chatId: String) {
        viewModelScope.launch {
            val currentConvos = _conversations.value
            _conversations.value = currentConvos.filter { it.chatId != chatId }
            chatRepository.updateConversationArchiveStatus(chatId, true).onFailure {
                _conversations.value = currentConvos
                _error.value = "Failed to archive conversation"
            }
        }
    }

    fun deleteConversation(chatId: String) {
        viewModelScope.launch {
            val currentConvos = _conversations.value
            _conversations.value = currentConvos.filter { it.chatId != chatId }
            chatRepository.deleteConversation(chatId).onFailure {
                _conversations.value = currentConvos
                _error.value = "Failed to delete conversation"
            }
        }
    }

    fun undoArchiveConversation(chatId: String) {
        viewModelScope.launch {
            chatRepository.updateConversationArchiveStatus(chatId, false)
            loadConversations()
        }
    }

}
