package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatInfoUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationMuteStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetGroupMembersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetOrCreateChatUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.InitializeE2EUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserActiveStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceDetailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatInitializationDelegate(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val initializeE2EUseCase: InitializeE2EUseCase,
    private val getOrCreateChatUseCase: GetOrCreateChatUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getChatInfoUseCase: GetChatInfoUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val observeUserActiveStatusUseCase: ObserveUserActiveStatusUseCase,
    private val observeUserPresenceDetailUseCase: ObserveUserPresenceDetailUseCase,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?,
    private val currentChatIdProvider: () -> String?,
    private val messagingDelegate: ChatMessagingDelegate,
    private val subscriptionDelegate: ChatSubscriptionDelegate,
    private val aiDelegate: ChatAiDelegate,
    private val _isLoading: MutableStateFlow<Boolean>,
    private val _isRefreshing: MutableStateFlow<Boolean>,
    private val _error: MutableStateFlow<String?>,
    private val _participantProfile: MutableStateFlow<User?>,
    private val _isE2EEReady: MutableStateFlow<Boolean>,
    private val _isGroupChat: MutableStateFlow<Boolean>,
    private val _groupTitle: MutableStateFlow<String?>,
    private val _groupAvatarUrl: MutableStateFlow<String?>,
    private val _onlyAdminsCanMessage: MutableStateFlow<Boolean>,
    private val _isCurrentUserAdmin: MutableStateFlow<Boolean>,
    private val _isParticipantActive: MutableStateFlow<Boolean>,
    private val _disappearingMode: MutableStateFlow<com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode>,
    private val getDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase,
    private val _isMuted: MutableStateFlow<Boolean>,
    private val getConversationMuteStatusUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationMuteStatusUseCase,
    private val onChatIdResolved: (String) -> Unit,
    private val _groupMemberNames: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())
) {

    private var activeJob: Job? = null
    private var initializationToken = 0L

    /** Cancels any in-flight initialization so stale results cannot land in the new chat. */
    fun cancelInitialization() {
        initializationToken++
        activeJob?.cancel()
        activeJob = null
    }

    /** Full cleanup: cancel in-flight job and reset all state this delegate manages. */
    fun cleanup() {
        cancelInitialization()
        _groupMemberNames.value = emptyMap()
        _participantProfile.value = null
        _isParticipantActive.value = false
        _isGroupChat.value = false
        _groupTitle.value = null
        _groupAvatarUrl.value = null
        _onlyAdminsCanMessage.value = false
        _isCurrentUserAdmin.value = false
        _disappearingMode.value = com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode.OFF
        _isMuted.value = false
        _isE2EEReady.value = false
        _error.value = null
    }

    private fun isCurrent(token: Long, chatId: String): Boolean =
        token == initializationToken &&
            currentChatIdProvider()?.equals(chatId, ignoreCase = true) == true

    fun initialize(chatId: String, participantId: String?) {
        cancelInitialization()
        val token = initializationToken

        // Switching chats must not show the previous conversation's profile, group flags,
        // presence, or disappearing mode while the new metadata loads.
        _groupMemberNames.value = emptyMap()
        _participantProfile.value = null
        _isParticipantActive.value = false
        _isGroupChat.value = false
        _groupTitle.value = null
        _groupAvatarUrl.value = null
        _onlyAdminsCanMessage.value = false
        _isCurrentUserAdmin.value = false
        _disappearingMode.value = com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode.OFF
        _isMuted.value = false
        _isLoading.value = true
        _error.value = null

        activeJob = viewModelScope.launch {
            val actualChatId = if (chatId == "new" && participantId != null) {
                getOrCreateChatUseCase(participantId).getOrElse { error ->
                    if (token == initializationToken) {
                        val detail = error.message?.takeIf { it.isNotBlank() }
                        _error.value = detail?.let { "Failed to create chat: $it" } ?: "Failed to create chat"
                        _isLoading.value = false
                    }
                    return@launch
                }
            } else {
                chatId
            }

            if (token != initializationToken) return@launch

            onChatIdResolved(actualChatId)

            // Start subscription immediately — do NOT wait for E2EE init, messages fetch,
            // or any other network call. Any delay here is a window where messages are lost.
            subscriptionDelegate.startSubscriptions(actualChatId)

            if (participantId != null) {
                launch {
                    getUserProfileUseCase(participantId).onSuccess { user ->
                        if (!isCurrent(token, actualChatId)) return@onSuccess
                        Napier.d("Loaded participant profile: ${user?.username}, avatar: ${user?.avatar}", tag = "ChatViewModel")
                        _participantProfile.value = user
                    }.onFailure { e ->
                        Napier.e("Failed to load participant profile", e)
                    }
                }
            }

            // All remaining work runs in parallel so none of it blocks the subscription.
            launch {
                initializeE2EUseCase().onSuccess {
                    if (!isCurrent(token, actualChatId)) return@onSuccess
                    _isE2EEReady.value = true
                    Napier.d("E2EE initialization successful", tag = "E2EE")
                }.onFailure { e ->
                    if (!isCurrent(token, actualChatId)) return@onFailure
                    _isE2EEReady.value = false
                    Napier.e("E2EE initialization failed: ${e.message}", e, tag = "E2EE")
                }
            }

            launch {
                try {
                    // Cache first for instant display; background refresh follows when cache exists.
                    getMessagesUseCase.cacheFirst(actualChatId, refreshInBackground = false).onSuccess { messages ->
                        if (!isCurrent(token, actualChatId)) return@onSuccess
                        messagingDelegate.setMessages(messages)
                        if (messages.isEmpty()) {
                            _isLoading.value = true
                        } else {
                            _isRefreshing.value = true
                        }
                    }.onFailure { error ->
                        if (!isCurrent(token, actualChatId)) return@onFailure
                        _error.value = error.message
                        _isLoading.value = true
                    }
                    getMessagesUseCase(actualChatId, forceNetwork = true).onSuccess { fresh ->
                        if (!isCurrent(token, actualChatId)) return@onSuccess
                        messagingDelegate.setMessages(fresh)
                    }.onFailure { error ->
                        if (!isCurrent(token, actualChatId)) return@onFailure
                        if (messagingDelegate.messages.value.isEmpty()) _error.value = error.message
                        Napier.w("Network refresh failed on init for chatId=" + actualChatId + ": " + error.message, tag = "ChatInit")
                    }
                } finally {
                    if (isCurrent(token, actualChatId)) {
                        _isLoading.value = false
                        _isRefreshing.value = false
                    }
                }
                if (isCurrent(token, actualChatId)) {
                    subscriptionDelegate.enqueueIncomingReceipt(actualChatId)
                    aiDelegate.generateSmartReplies(messagingDelegate.messages.value)
                }
            }

            launch {
                getChatInfoUseCase(actualChatId).onSuccess { chatDto ->
                    if (!isCurrent(token, actualChatId)) return@onSuccess
                    _isGroupChat.value = chatDto?.isGroup == true
                    if (chatDto?.isGroup == true) {
                        _groupTitle.value = chatDto.name
                        _groupAvatarUrl.value = chatDto.avatarUrl
                        _onlyAdminsCanMessage.value = chatDto.onlyAdminsCanMessage
                        getGroupMembersUseCase(actualChatId).onSuccess { members ->
                            if (!isCurrent(token, actualChatId)) return@onSuccess
                            _groupMemberNames.value = members.associate { (member, _) ->
                                val displayName = member.displayName?.takeIf(String::isNotBlank)
                                    ?: member.nickname?.takeIf(String::isNotBlank)
                                    ?: member.name?.takeIf(String::isNotBlank)
                                    ?: member.username?.takeIf(String::isNotBlank)
                                member.uid to displayName
                            }.filterValues { !it.isNullOrBlank() }.mapValues { it.value.orEmpty() }
                            _isCurrentUserAdmin.value = members.find { it.first.uid == currentUserIdProvider() }?.second == true
                        }
                    }
                }
            }

            launch {
                getDisappearingModeUseCase(actualChatId).onSuccess { mode ->
                    if (!isCurrent(token, actualChatId)) return@onSuccess
                    _disappearingMode.value = mode
                }
            }

            launch {
                getConversationMuteStatusUseCase(actualChatId).onSuccess { muted ->
                    if (!isCurrent(token, actualChatId)) return@onSuccess
                    _isMuted.value = muted
                }
            }

            if (participantId != null) {
                launch {
                    observeUserPresenceDetailUseCase(participantId).collect { presence ->
                        if (!isCurrent(token, actualChatId)) return@collect
                        _isParticipantActive.value = presence.isOnline
                        if (!presence.lastSeen.isNullOrBlank()) {
                            _participantProfile.update { current ->
                                current?.copy(lastSeen = presence.lastSeen)
                            }
                        }
                    }
                }
            }
        }
    }

}
