package com.synapse.social.studioasinc.shared.data.local.database
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.database.CachedMessage
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class SqlDelightCachedMessageDao(
    private val db: StorageDatabase
) : CachedMessageDao {

    override suspend fun upsertAll(messages: List<Message>) {
        withContext(AppDispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
            db.transaction {
                messages.forEach { message ->
                    db.cachedMessageQueries.upsertMessage(toCachedMessage(message, now))
                }
            }
        }
    }

    override suspend fun getMessages(chatId: String, limit: Int): List<Message> = withContext(AppDispatchers.IO) {
        db.cachedMessageQueries.selectByChatId(
            chatId = chatId,
            limit = limit.toLong()
        ).executeAsList().map { cached ->
            val domain = toDomainMessage(cached)
            try {
                val reactions = db.messageReactionQueries.selectByMessageId(domain.id).executeAsList()
                val summary = reactions
                    .groupBy { it.reaction_emoji }
                    .mapKeys { ReactionType.values().find { rt -> rt.emoji == it.key } ?: ReactionType.LIKE }
                    .mapValues { it.value.size }
                domain.copy(reactions = summary)
            } catch (e: Exception) {
                domain
            }
        }
    }

    override suspend fun upsert(message: Message) {
        withContext(AppDispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
            db.cachedMessageQueries.upsertMessage(toCachedMessage(message, now))
        }
    }

    override suspend fun updateContent(id: String, content: String) {
        withContext(AppDispatchers.IO) {
            db.cachedMessageQueries.updateContent(
                content = content,
                id = id
            )
        }
    }

    override suspend fun markDeleted(id: String) {
        withContext(AppDispatchers.IO) {
            db.cachedMessageQueries.markDeleted(id)
        }
    }

    override suspend fun markDeleted(ids: List<String>) {
        withContext(AppDispatchers.IO) {
            db.transaction {
                ids.forEach { id ->
                    db.cachedMessageQueries.markDeleted(id)
                }
            }
        }
    }

    override suspend fun trimToLimit(chatId: String, limit: Int) {
        withContext(AppDispatchers.IO) {
            db.cachedMessageQueries.deleteOldestBeyondLimit(
                chatId = chatId,
                limit = limit.toLong()
            )
        }
    }

    override suspend fun deleteAll() {
        withContext(AppDispatchers.IO) {
            db.cachedMessageQueries.deleteAll()
        }
    }

    override suspend fun updateReactions(messageId: String, reactions: Map<ReactionType, Int>, userReaction: ReactionType?) {
    }

    private fun toCachedMessage(domain: Message, cachedAt: Long): CachedMessage {
        return CachedMessage(
            id = domain.id,
            chat_id = domain.chatId,
            sender_id = domain.senderId,
            content = domain.content,
            message_type = domain.messageType.name,
            media_url = domain.mediaUrl,
            delivery_status = domain.deliveryStatus.name,
            read_by = domain.readBy.joinToString(separator = ",").takeIf { it.isNotEmpty() },
            created_at = domain.createdAt,
            expires_at = domain.expiresAt,
            reply_to_id = domain.replyToId,
            is_deleted = if (domain.isDeleted) 1L else 0L,
            is_edited = if (domain.isEdited) 1L else 0L,
            cached_at = cachedAt
        )
    }

    private fun toDomainMessage(cached: CachedMessage): Message {
        val messageType = try {
            MessageType.valueOf(cached.message_type)
        } catch (e: Exception) {
            MessageType.TEXT // fallback
        }

        val deliveryStatus = try {
            DeliveryStatus.valueOf(cached.delivery_status)
        } catch (e: Exception) {
            DeliveryStatus.SENT // fallback
        }

        return Message(
            id = cached.id,
            chatId = cached.chat_id,
            senderId = cached.sender_id,
            content = cached.content,
            messageType = messageType,
            mediaUrl = cached.media_url,
            deliveryStatus = deliveryStatus,
            isDeleted = cached.is_deleted == 1L,
            isEdited = cached.is_edited == 1L,
            replyToId = cached.reply_to_id,
            createdAt = cached.created_at,
            expiresAt = cached.expires_at,
            readBy = cached.read_by?.split(",") ?: emptyList(),
            reactions = emptyMap(),
            userReaction = null
        )
    }
}
