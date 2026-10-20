package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** The common groups found and whether any participant lookups failed. */
data class CommonGroupsResult(
    val groups: List<Conversation>,
    val hasIncompleteResults: Boolean
)

/** Finds the signed-in user's group conversations that also include the supplied contact. */
class GetCommonGroupsUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(otherUserId: String): Result<CommonGroupsResult> {
        if (otherUserId.isBlank()) return Result.success(CommonGroupsResult(emptyList(), false))

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
        var hasIncompleteResults = false
        for ((conversation, participantsResult) in groupParticipants) {
            val participants = participantsResult.getOrNull()
            if (participants == null) {
                hasIncompleteResults = true
                continue
            }
            if (otherUserId in participants) commonGroups += conversation
        }
        return Result.success(CommonGroupsResult(commonGroups, hasIncompleteResults))
    }
}
