package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.usecase.chat.ToggleMessageReactionUseCase
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ChatReactionDelegate(
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase,
    private val viewModelScope: CoroutineScope,
    private val onOptimisticReactionChanged: (String, Message, ReactionType?, ReactionType?) -> Unit,
    private val onError: (String?, Message) -> Unit
) {

    fun toggleMessageReaction(
        messageId: String,
        reactionType: ReactionType,
        messages: List<Message>
    ) {
        viewModelScope.launch {
            val oldMessage = messages.find { it.id == messageId } ?: return@launch
            val oldUserReaction = oldMessage.userReaction

            val isSame = oldUserReaction == reactionType
            val newUserReaction = if (isSame) null else reactionType

            // Optimistic update
            onOptimisticReactionChanged(messageId, oldMessage, newUserReaction, oldUserReaction)

            toggleMessageReactionUseCase(messageId, reactionType.emoji).onFailure { e ->
                // Revert
                onError("Failed to toggle reaction: ${e.message}", oldMessage)
            }
        }
    }
}
