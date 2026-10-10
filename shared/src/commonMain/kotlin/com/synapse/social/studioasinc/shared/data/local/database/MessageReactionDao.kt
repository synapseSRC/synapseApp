package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction

interface MessageReactionDao {
    suspend fun insert(reaction: MessageReaction)
    suspend fun insertAll(reactions: List<MessageReaction>)
    suspend fun getByMessageId(messageId: String): List<MessageReaction>
    suspend fun getByMessageIds(messageIds: List<String>): List<MessageReaction>
    suspend fun delete(messageId: String, userId: String, emoji: String)
    suspend fun deleteAllByMessageId(messageId: String)
    suspend fun deleteAllByMessageIds(messageIds: List<String>)
    suspend fun deleteAll()
}
