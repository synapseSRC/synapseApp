package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.database.MessageReaction as CachedReaction
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightMessageReactionDao(
    private val db: StorageDatabase
) : MessageReactionDao {

    override suspend fun insert(reaction: MessageReaction) {
        withContext(Dispatchers.Default) {
            db.messageReactionQueries.insertReaction(toCachedReaction(reaction))
        }
    }

    override suspend fun insertAll(reactions: List<MessageReaction>) {
        withContext(Dispatchers.Default) {
            db.transaction {
                reactions.forEach { reaction ->
                    db.messageReactionQueries.insertReaction(toCachedReaction(reaction))
                }
            }
        }
    }

    override suspend fun getByMessageId(messageId: String): List<MessageReaction> = withContext(Dispatchers.Default) {
        db.messageReactionQueries.selectByMessageId(messageId).executeAsList().map { toDomainReaction(it) }
    }

    override suspend fun delete(messageId: String, userId: String, emoji: String) {
        withContext(Dispatchers.Default) {
            db.messageReactionQueries.deleteReaction(messageId, userId, emoji)
        }
    }

    override suspend fun deleteAllByMessageId(messageId: String) {
        withContext(Dispatchers.Default) {
            db.messageReactionQueries.deleteAllByMessageId(messageId)
        }
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.Default) {
            db.messageReactionQueries.deleteAll()
        }
    }

    private fun toCachedReaction(domain: MessageReaction): CachedReaction {
        return CachedReaction(
            message_id = domain.messageId,
            user_id = domain.userId,
            reaction_emoji = domain.reactionEmoji,
            timestamp = domain.timestamp
        )
    }

    private fun toDomainReaction(cached: CachedReaction): MessageReaction {
        return MessageReaction(
            messageId = cached.message_id,
            userId = cached.user_id,
            reactionEmoji = cached.reaction_emoji,
            timestamp = cached.timestamp
        )
    }
}
