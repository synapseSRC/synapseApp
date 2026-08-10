package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatInfoUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetGroupMembersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetOrCreateChatUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.InitializeE2EUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsDeliveredUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserActiveStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ChatInitializationDelegate(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val initializeE2EUseCase: InitializeE2EUseCase,
    private val getOrCreateChatUseCase: GetOrCreateChatUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getChatInfoUseCase: GetChatInfoUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val observeUserActiveStatusUseCase: ObserveUserActiveStatusUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val markMessagesAsDeliveredUseCase: MarkMessagesAsDeliveredUseCase,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?,
    private val messagingDelegate: ChatMessagingDelegate,
    private val subscriptionDelegate: ChatSubscriptionDelegate,
    private val aiDelegate: ChatAiDelegate,
    private val _isLoading: MutableStateFlow<Boolean>,
    private val _error: MutableStateFlow<String?>,
    private val _participantProfile: MutableStateFlow<User?>,
    private val _isE2EEReady: MutableStateFlow<Boolean>,
    private val _isGroupChat: MutableStateFlow<Boolean>,
    private val _onlyAdminsCanMessage: MutableStateFlow<Boolean>,
    private val _isCurrentUserAdmin: MutableStateFlow<Boolean>,
    private val _isParticipantActive: MutableStateFlow<Boolean>,
    private val _disappearingMode: MutableStateFlow<com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode>,
    private val getDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase,
    private val onChatIdResolved: (String) -> Unit
) {

    fun initialize(chatId: String, participantId: String?, currentChatId: String?) {
        // If re-entering the same chat, restart subscriptions and reload messages
        // (cleanup() clears messages before this runs, so they must be reloaded).
        if (chatId != "new" && chatId == currentChatId) {
            subscriptionDelegate.startSubscriptions(chatId)
            viewModelScope.launch {
                // Fast path: show cached messages immediately
                getMessagesUseCase(chatId).onSuccess { messages ->
                    messagingDelegate.setMessages(messages)
                    _isLoading.value = false
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                // Network refresh: always fetch fresh to catch any messages missed while away
                getMessagesUseCase(chatId, forceNetwork = true).onSuccess { fresh ->
                    messagingDelegate.setMessages(fresh)
                }.onFailure { e ->
                    Napier.w("Network refresh failed on re-entry for chatId=$chatId: ${e.message}", tag = "ChatInit")
                }
                markMessagesAsReadUseCase(chatId)
                markMessagesAsDeliveredUseCase(chatId)
            }
            return
        }

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
            val actualChatId = if (chatId == "new" && participantId != null) {
                getOrCreateChatUseCase(participantId).getOrElse {
                    _error.value = "Failed to create chat"
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
                // Fast path: cache-first for instant display
                getMessagesUseCase(actualChatId).onSuccess { messages ->
                    messagingDelegate.setMessages(messages)
                    _isLoading.value = false
                }.onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                // Network refresh: always fetch fresh to catch any messages missed while away
                getMessagesUseCase(actualChatId, forceNetwork = true).onSuccess { fresh ->
                    messagingDelegate.setMessages(fresh)
                }.onFailure { e ->
                    Napier.w("Network refresh failed on init for chatId=$actualChatId: ${e.message}", tag = "ChatInit")
                }
                aiDelegate.generateSmartReplies(messagingDelegate.messages.value)
                markMessagesAsReadUseCase(actualChatId)
                markMessagesAsDeliveredUseCase(actualChatId)
            }

            launch {
                getChatInfoUseCase(actualChatId).onSuccess { chatDto ->
                    _isGroupChat.value = chatDto?.isGroup == true
                    if (chatDto?.isGroup == true) {
                        _onlyAdminsCanMessage.value = chatDto.onlyAdminsCanMessage
                        getGroupMembersUseCase(actualChatId).onSuccess { members ->
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
                    observeUserActiveStatusUseCase(participantId).collect { isActive ->
                        _isParticipantActive.value = isActive
                    }
                }
            }
        }
    }
}
