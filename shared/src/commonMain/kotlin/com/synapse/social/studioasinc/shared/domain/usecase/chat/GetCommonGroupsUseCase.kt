package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

/** Finds the signed-in user's group conversations that also include the supplied contact. */
class GetCommonGroupsUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(otherUserId: String): Result<List<Conversation>> {
        if (otherUserId.isBlank()) return Result.success(emptyList())

        val conversationsResult = repository.getConversations()
        val conversations = conversationsResult.getOrNull()
            ?: return Result.failure(
                conversationsResult.exceptionOrNull() ?: IllegalStateException("Conversations could not be loaded")
            )

        val commonGroups = mutableListOf<Conversation>()
        conversations.filter { it.isGroup }.forEach { conversation ->
            val participantsResult = repository.getParticipantIds(conversation.chatId)
            val participants = participantsResult.getOrNull()
                ?: return Result.failure(
                    participantsResult.exceptionOrNull() ?: IllegalStateException("Group participants could not be loaded")
                )
            if (otherUserId in participants) commonGroups += conversation
        }
        return Result.success(commonGroups)
    }
}
