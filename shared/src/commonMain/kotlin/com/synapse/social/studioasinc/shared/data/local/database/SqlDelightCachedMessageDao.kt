package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.database.CachedMessage
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageAttachmentDto
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageMetadataContainer
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.util.TimeProvider
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.serialization.json.Json

class SqlDelightCachedMessageDao(
    private val db: StorageDatabase
) : CachedMessageDao {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun upsertAll(messages: List<Message>) {
        withContext(AppDispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
            db.transaction {
                messages.forEach { message -> upsertMerged(message, now) }
            }
        }
    }

    // Call inside a transaction so the read/merge/write cannot race another cache update.
    private fun upsertMerged(message: Message, cachedAt: Long) {
        val existing = db.cachedMessageQueries.selectActiveById(message.id).executeAsOneOrNull()
        val merged = existing?.let { cached ->
            with(ChatMapper) { toDomainMessage(cached).mergeMonotonic(message) }
        } ?: message
        db.cachedMessageQueries.upsertMessage(toCachedMessage(merged, cachedAt))
    }

    override suspend fun getMessageById(id: String): Message? = withContext(AppDispatchers.IO) {
        db.cachedMessageQueries.selectActiveById(id).executeAsOneOrNull()?.let { toDomainMessage(it) }
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

    override suspend fun getMessagesBefore(chatId: String, beforeCreatedAt: String, beforeId: String, limit: Int): List<Message> = withContext(AppDispatchers.IO) {
        db.cachedMessageQueries.selectActiveByChatIdBefore(
            chatId = chatId,
            beforeCreatedAt = beforeCreatedAt,
            beforeId = beforeId,
            limit = limit.toLong()
        ).executeAsList().reversed().map { cached ->
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

    override suspend fun getMessagesAfter(chatId: String, afterCreatedAt: String, afterId: String, limit: Int): List<Message> = withContext(AppDispatchers.IO) {
        db.cachedMessageQueries.selectActiveByChatIdAfter(
            chatId = chatId,
            afterCreatedAt = afterCreatedAt,
            afterId = afterId,
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
            db.transaction {
                upsertMerged(message, now)
            }
        }
    }

    override suspend fun updateContent(id: String, content: String) {
        withContext(AppDispatchers.IO) {
            val nowStr = TimeProvider.nowInstant().toString()
            db.cachedMessageQueries.updateContent(
                content = content,
                editedAt = nowStr,
                id = id
            )
        }
    }

    override suspend fun markDeleted(id: String) {
        withContext(AppDispatchers.IO) {
            val nowStr = TimeProvider.nowInstant().toString()
            db.cachedMessageQueries.markDeleted(
                deletedAt = nowStr,
                id = id
            )
        }
    }

    override suspend fun markDeleted(ids: List<String>) {
        withContext(AppDispatchers.IO) {
            val nowStr = TimeProvider.nowInstant().toString()
            db.transaction {
                ids.forEach { id ->
                    db.cachedMessageQueries.markDeleted(
                        deletedAt = nowStr,
                        id = id
                    )
                }
            }
        }
    }

    override suspend fun delete(id: String) {
        withContext(AppDispatchers.IO) {
            db.cachedMessageQueries.deleteById(id)
        }
    }

    override suspend fun delete(ids: List<String>) {
        withContext(AppDispatchers.IO) {
            db.transaction {
                ids.forEach { id -> db.cachedMessageQueries.deleteById(id) }
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

    override suspend fun markRead(chatId: String, userId: String) {
        withContext(AppDispatchers.IO) {
            val nowStr = TimeProvider.nowInstant().toString()
            val allMessages = db.cachedMessageQueries.selectByChatId(chatId, 500).executeAsList()
            db.transaction {
                allMessages.forEach { cached ->
                    if (cached.sender_id != userId) {
                        val readBy = cached.read_by?.split(",")?.toMutableSet() ?: mutableSetOf()
                        if (userId !in readBy) {
                            readBy.add(userId)
                            db.cachedMessageQueries.updateReadBy(
                                read_by = readBy.joinToString(","),
                                readAt = nowStr,
                                id = cached.id
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toCachedMessage(domain: Message, cachedAt: Long): CachedMessage {
        val mediaUrlValue = if (domain.attachments.isNotEmpty()) {
            try {
                val dtoList = domain.attachments.map { with(ChatMapper) { it.toDto() } }
                json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(MessageAttachmentDto.serializer()),
                    dtoList
                )
            } catch (e: Exception) {
                domain.mediaUrl
            }
        } else {
            domain.mediaUrl
        }

        val metadataString = domain.metadataContainer?.let {
            try {
                json.encodeToString(MessageMetadataContainer.serializer(), it)
            } catch (e: Exception) {
                null
            }
        }

        return CachedMessage(
            id = domain.id,
            chat_id = domain.chatId,
            sender_id = domain.senderId,
            content = domain.content,
            message_type = domain.messageType.name,
            media_url = mediaUrlValue,
            delivery_status = domain.deliveryStatus.name,
            content_status = domain.contentStatus.name,
            read_by = domain.readBy.joinToString(separator = ",").takeIf { it.isNotEmpty() },
            created_at = domain.createdAt,
            sent_at = domain.sentAt ?: domain.createdAt,
            delivered_at = domain.deliveredAt,
            read_at = domain.readAt,
            edited_at = domain.editedAt,
            deleted_at = domain.deletedAt,
            expires_at = domain.expiresAt,
            reply_to_id = domain.replyToId,
            failure_reason = domain.failureReason,
            is_deleted = if (domain.isDeleted || domain.contentStatus == ContentStatus.DELETED) 1L else 0L,
            is_edited = if (domain.isEdited || domain.contentStatus == ContentStatus.EDITED) 1L else 0L,
            cached_at = cachedAt,
            metadata = metadataString,
            // Sync metadata — populated with current time so reconciliation can use it
            updated_at = cachedAt,
            is_locally_deleted = 0L,
            last_synced_at = null,
            sync_version = 1L
        )
    }

    private fun toDomainMessage(cached: CachedMessage): Message {
        val messageType = try {
            MessageType.valueOf(cached.message_type)
        } catch (e: Exception) {
            MessageType.TEXT
        }

        val rawDeliveryStatus = cached.delivery_status
        val readByList = cached.read_by?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val deliveryStatus = when {
            cached.read_at != null || readByList.any { it != cached.sender_id } || rawDeliveryStatus.equals("READ", ignoreCase = true) -> DeliveryStatus.READ
            cached.delivered_at != null || rawDeliveryStatus.equals("DELIVERED", ignoreCase = true) -> DeliveryStatus.DELIVERED
            rawDeliveryStatus.equals("FAILED", ignoreCase = true) -> DeliveryStatus.FAILED
            rawDeliveryStatus.equals("SENDING", ignoreCase = true) -> DeliveryStatus.SENDING
            else -> DeliveryStatus.SENT
        }

        val contentStatus = try {
            ContentStatus.valueOf(cached.content_status)
        } catch (e: Exception) {
            if (cached.is_deleted == 1L) ContentStatus.DELETED
            else if (cached.is_edited == 1L) ContentStatus.EDITED
            else ContentStatus.ACTIVE
        }

        var attachmentsList = emptyList<MessageAttachment>()
        var resolvedMediaUrl = cached.media_url

        if (cached.media_url?.trimStart()?.startsWith("[") == true) {
            try {
                val dtos = json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(MessageAttachmentDto.serializer()),
                    cached.media_url
                )
                attachmentsList = dtos.map { with(ChatMapper) { it.toDomain() } }
                resolvedMediaUrl = attachmentsList.firstOrNull()?.url
            } catch (e: Exception) {
                com.synapse.social.studioasinc.shared.util.Logger.e("Error deserializing attachments for cached message ${cached.id}", throwable = e)
            }
        }

        val finalMessageType = if (attachmentsList.size >= 2 || messageType == MessageType.MEDIA_GROUP) MessageType.MEDIA_GROUP else messageType

        val parsedMetadata = cached.metadata?.let {
            try {
                json.decodeFromString(MessageMetadataContainer.serializer(), it)
            } catch (e: Exception) {
                null
            }
        }

        val isForwarded = parsedMetadata?.forwarded != null
        val forwardedFromMessageId = parsedMetadata?.forwarded?.originalMessageId

        val isDeletedMessage = cached.is_deleted == 1L || contentStatus == ContentStatus.DELETED

        return Message(
            id = cached.id,
            chatId = cached.chat_id,
            senderId = cached.sender_id,
            content = if (isDeletedMessage) "" else cached.content,
            messageType = finalMessageType,
            mediaUrl = if (isDeletedMessage) null else resolvedMediaUrl,
            deliveryStatus = deliveryStatus,
            contentStatus = contentStatus,
            isDeleted = isDeletedMessage,
            isEdited = cached.is_edited == 1L || contentStatus == ContentStatus.EDITED,
            isForwarded = isForwarded,
            replyToId = cached.reply_to_id,
            forwardedFromMessageId = forwardedFromMessageId,
            sentAt = cached.sent_at ?: cached.created_at,
            deliveredAt = cached.delivered_at,
            readAt = cached.read_at,
            editedAt = cached.edited_at,
            deletedAt = cached.deleted_at,
            failureReason = cached.failure_reason.takeIf { deliveryStatus == DeliveryStatus.FAILED },
            createdAt = cached.created_at,
            expiresAt = cached.expires_at,
            readBy = readByList,
            reactions = emptyMap(),
            userReaction = null,
            attachments = if (isDeletedMessage) emptyList() else attachmentsList,
            mediaGroupId = attachmentsList.firstOrNull()?.mediaGroupId,
            metadataContainer = if (isDeletedMessage) null else parsedMetadata
        )
    }

    override suspend fun deleteExpired(chatId: String, now: String) {
        withContext(AppDispatchers.IO) {
            db.cachedMessageQueries.deleteExpired(
                chatId = chatId,
                now = now
            )
        }
    }
}
