package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatInfoUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetGroupMembersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetOrCreateChatUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.InitializeE2EUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserActiveStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceDetailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
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
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?,
    private val messagingDelegate: ChatMessagingDelegate,
    private val subscriptionDelegate: ChatSubscriptionDelegate,
    private val aiDelegate: ChatAiDelegate,
    private val _isLoading: MutableStateFlow<Boolean>,
    private val _isRefreshing: MutableStateFlow<Boolean>,
    private val _error: MutableStateFlow<String?>,
    private val _participantProfile: MutableStateFlow<User?>,
    private val _isE2EEReady: MutableStateFlow<Boolean>,
    private val _isGroupChat: MutableStateFlow<Boolean>,
    private val _onlyAdminsCanMessage: MutableStateFlow<Boolean>,
    private val _isCurrentUserAdmin: MutableStateFlow<Boolean>,
    private val _isParticipantActive: MutableStateFlow<Boolean>,
    private val _disappearingMode: MutableStateFlow<com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode>,
    private val getDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase,
    private val onChatIdResolved: (String) -> Unit,
    private val _groupMemberNames: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())
) {

    fun initialize(chatId: String, participantId: String?, currentChatId: String?) {
        // If re-entering the same chat, restart subscriptions and reload messages
        // (cleanup() clears messages before this runs, so they must be reloaded).
        if (chatId != "new" && (chatId.equals(currentChatId, ignoreCase = true) || (participantId != null && currentChatId != null))) {
            val resolvedId = currentChatId ?: chatId
            subscriptionDelegate.startSubscriptions(resolvedId)
            viewModelScope.launch {
                try {
                    // Cache first for instant display; background refresh follows when cache exists.
                    getMessagesUseCase.cacheFirst(resolvedId, refreshInBackground = false).onSuccess { messages ->
                        messagingDelegate.setMessages(messages)
                        if (messages.isEmpty()) {
                            _isLoading.value = true
                        } else {
                            _isRefreshing.value = true
                        }
                    }.onFailure { e ->
                        _error.value = e.message
                        _isLoading.value = true
                    }
                    getMessagesUseCase(resolvedId, forceNetwork = true).onSuccess { fresh ->
                        messagingDelegate.setMessages(fresh)
                    }.onFailure { e ->
                        if (messagingDelegate.messages.value.isEmpty()) _error.value = e.message
                        Napier.w("Network refresh failed on re-entry for chatId=" + resolvedId + ": " + e.message, tag = "ChatInit")
                    }
                } finally {
                    _isLoading.value = false
                    _isRefreshing.value = false
                }
                markMessagesAsReadUseCase(resolvedId)
            }
            if (participantId != null) {
                viewModelScope.launch {
                    observeUserPresenceDetailUseCase(participantId).collect { presence ->
                        _isParticipantActive.value = presence.isOnline
                        if (!presence.lastSeen.isNullOrBlank()) {
                            _participantProfile.update { current ->
                                current?.copy(lastSeen = presence.lastSeen)
                            }
                        }
                    }
                }
            }
            return
        }

        _groupMemberNames.value = emptyMap()
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

        viewModelScope.launch {
            val actualChatId = if ((chatId == "new" || chatId == participantId) && participantId != null) {
                getOrCreateChatUseCase(participantId).getOrElse { error ->
                    val detail = error.message?.takeIf { it.isNotBlank() }
                    _error.value = detail?.let { "Failed to create chat: $it" } ?: "Failed to create chat"
                    _isLoading.value = false
                    return@launch
                }
            } else {
                chatId
            }

            onChatIdResolved(actualChatId)

            // Start subscription immediately — do NOT wait for E2EE init, messages fetch,
            // or any other network call. Any delay here is a window where messages are lost.
            subscriptionDelegate.startSubscriptions(actualChatId)

            // All remaining work runs in parallel so none of it blocks the subscription.
            launch {
                initializeE2EUseCase().onSuccess {
                    _isE2EEReady.value = true
                    Napier.d("E2EE initialization successful", tag = "E2EE")
                }.onFailure { e ->
                    _isE2EEReady.value = false
                    Napier.e("E2EE initialization failed: ${e.message}", e, tag = "E2EE")
                }
            }

            launch {
                try {
                    // Cache first for instant display; background refresh follows when cache exists.
                    getMessagesUseCase.cacheFirst(actualChatId, refreshInBackground = false).onSuccess { messages ->
                        messagingDelegate.setMessages(messages)
                        if (messages.isEmpty()) {
                            _isLoading.value = true
                        } else {
                            _isRefreshing.value = true
                        }
                    }.onFailure { error ->
                        _error.value = error.message
                        _isLoading.value = true
                    }
                    getMessagesUseCase(actualChatId, forceNetwork = true).onSuccess { fresh ->
                        messagingDelegate.setMessages(fresh)
                    }.onFailure { error ->
                        if (messagingDelegate.messages.value.isEmpty()) _error.value = error.message
                        Napier.w("Network refresh failed on init for chatId=" + actualChatId + ": " + error.message, tag = "ChatInit")
                    }
                } finally {
                    _isLoading.value = false
                    _isRefreshing.value = false
                }
                aiDelegate.generateSmartReplies(messagingDelegate.messages.value)
                markMessagesAsReadUseCase(actualChatId)
            }

            launch {
                getChatInfoUseCase(actualChatId).onSuccess { chatDto ->
                    _isGroupChat.value = chatDto?.isGroup == true
                    if (chatDto?.isGroup == true) {
                        _onlyAdminsCanMessage.value = chatDto.onlyAdminsCanMessage
                        getGroupMembersUseCase(actualChatId).onSuccess { members ->
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
                    _disappearingMode.value = mode
                }
            }

            if (participantId != null) {
                launch {
                    observeUserPresenceDetailUseCase(participantId).collect { presence ->
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
