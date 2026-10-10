package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.core.util.ChatLockManager
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import kotlinx.coroutines.flow.MutableStateFlow

import com.synapse.social.studioasinc.shared.domain.usecase.chat.SetDisappearingModeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ChatSettingsDelegate(
    private val chatLockManager: ChatLockManager,
    private val setDisappearingModeUseCase: SetDisappearingModeUseCase,
    private val viewModelScope: CoroutineScope,
    private val _disappearingMode: MutableStateFlow<DisappearingMode>,
    private val currentChatIdProvider: () -> String?
) {

    fun isChatLocked(): Boolean {
        return currentChatIdProvider()?.let { chatLockManager.isChatLocked(it) } ?: false
    }

    fun lockCurrentChat() {
        currentChatIdProvider()?.let { chatLockManager.lockChat(it) }
    }

    fun unlockCurrentChat() {
        currentChatIdProvider()?.let { chatLockManager.unlockChat(it) }
    }

    fun setDisappearingMode(mode: DisappearingMode) {
        _disappearingMode.value = mode
        val chatId = currentChatIdProvider() ?: return
        viewModelScope.launch {
            setDisappearingModeUseCase(chatId, mode)
        }
    }
}
