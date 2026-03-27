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
    private val _onlyAdminsCanMessage: MutableStateFlow<Boolean>,
    private val _isCurrentUserAdmin: MutableStateFlow<Boolean>,
    private val _isParticipantActive: MutableStateFlow<Boolean>,
    private val onChatIdResolved: (String) -> Unit
) {

    fun initialize(chatId: String, participantId: String?, currentChatId: String?) {
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
            initializeE2EUseCase().onSuccess {
                _isE2EEReady.value = true
                Napier.d("E2EE initialization successful", tag = "E2EE")
            }.onFailure { e ->
                _isE2EEReady.value = false
                Napier.e("E2EE initialization failed: ${e.message}", e, tag = "E2EE")
            }

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

            getMessagesUseCase(actualChatId).onSuccess { messages ->
                messagingDelegate.setMessages(messages)
                _isLoading.value = false
            }.onFailure { e ->
                _error.value = e.message
                _isLoading.value = false
            }

            getChatInfoUseCase(actualChatId).onSuccess { chatDto ->
                if (chatDto?.isGroup == true) {
                    _onlyAdminsCanMessage.value = chatDto.onlyAdminsCanMessage
                    getGroupMembersUseCase(actualChatId).onSuccess { members ->
                        _isCurrentUserAdmin.value = members.find { it.first.uid == currentUserIdProvider() }?.second == true
                    }
                }
            }

            subscriptionDelegate.startSubscriptions(actualChatId)

            viewModelScope.launch {
                if (participantId != null) {
                    observeUserActiveStatusUseCase(participantId).collect { isActive ->
                        _isParticipantActive.value = isActive
                    }
                }
            }

            markMessagesAsReadUseCase(actualChatId)
            markMessagesAsDeliveredUseCase(actualChatId)

            aiDelegate.generateSmartReplies(messagingDelegate.messages.value)
        }
    }
}
