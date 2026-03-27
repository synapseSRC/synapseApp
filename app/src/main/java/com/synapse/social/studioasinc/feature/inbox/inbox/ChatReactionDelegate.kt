package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.domain.usecase.reaction.ToggleMessageReactionUseCase
import com.synapse.social.studioasinc.domain.model.ReactionType as AppReactionType
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ChatReactionDelegate(
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase,
    private val viewModelScope: CoroutineScope,
    private val onOptimisticReactionChanged: (String, Message, com.synapse.social.studioasinc.shared.domain.model.ReactionType?, com.synapse.social.studioasinc.shared.domain.model.ReactionType?) -> Unit,
    private val onError: (String?, Message) -> Unit
) {

    fun toggleMessageReaction(
        messageId: String,
        reactionType: com.synapse.social.studioasinc.shared.domain.model.ReactionType,
        messages: List<Message>
    ) {
        viewModelScope.launch {
            val oldMessage = messages.find { it.id == messageId } ?: return@launch
            val oldUserReaction = oldMessage.userReaction

            val isSame = oldUserReaction == reactionType
            val newUserReaction = if (isSame) null else reactionType

            val appReactionType = AppReactionType.fromString(reactionType.name)
            val oldAppReaction = oldUserReaction?.let { r -> AppReactionType.fromString(r.name) }

            // Optimistic update
            onOptimisticReactionChanged(messageId, oldMessage, newUserReaction, oldUserReaction)

            toggleMessageReactionUseCase(messageId, appReactionType, oldAppReaction).onFailure { e ->
                // Revert
                onError("Failed to toggle reaction: ${e.message}", oldMessage)
            }
        }
    }
}
