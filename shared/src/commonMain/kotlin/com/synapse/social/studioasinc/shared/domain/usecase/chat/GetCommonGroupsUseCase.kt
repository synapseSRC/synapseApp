package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Finds the signed-in user's group conversations that also include the supplied contact. */
class GetCommonGroupsUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(otherUserId: String): Result<List<Conversation>> {
        if (otherUserId.isBlank()) return Result.success(emptyList())

        val conversationsResult = repository.getConversations()
        val conversations = conversationsResult.getOrNull()
            ?: return Result.failure(
                conversationsResult.exceptionOrNull() ?: IllegalStateException("Conversations could not be loaded")
            )

        val groupParticipants = coroutineScope {
            conversations
                .filter { it.isGroup }
                .map { conversation ->
                    async { conversation to repository.getParticipantIds(conversation.chatId) }
                }
                .awaitAll()
        }

        val commonGroups = mutableListOf<Conversation>()
        for ((conversation, participantsResult) in groupParticipants) {
            val participants = participantsResult.getOrNull()
                ?: return Result.failure(
                    participantsResult.exceptionOrNull()
                        ?: IllegalStateException("Group participants could not be loaded")
                )
            if (otherUserId in participants) commonGroups += conversation
        }
        return Result.success(commonGroups)
    }
}
