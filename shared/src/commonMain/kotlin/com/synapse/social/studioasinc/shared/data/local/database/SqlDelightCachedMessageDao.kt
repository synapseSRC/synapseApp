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
import kotlinx.datetime.Clock
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
            metadata = metadataString
        )
    }

    private fun toDomainMessage(cached: CachedMessage): Message {
        val messageType = try {
            MessageType.valueOf(cached.message_type)
        } catch (e: Exception) {
            MessageType.TEXT
        }

        val deliveryStatus = try {
            DeliveryStatus.valueOf(cached.delivery_status)
        } catch (e: Exception) {
            DeliveryStatus.SENT
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

        return Message(
            id = cached.id,
            chatId = cached.chat_id,
            senderId = cached.sender_id,
            content = cached.content,
            messageType = finalMessageType,
            mediaUrl = resolvedMediaUrl,
            deliveryStatus = deliveryStatus,
            contentStatus = contentStatus,
            isDeleted = cached.is_deleted == 1L || contentStatus == ContentStatus.DELETED,
            isEdited = cached.is_edited == 1L || contentStatus == ContentStatus.EDITED,
            isForwarded = isForwarded,
            replyToId = cached.reply_to_id,
            forwardedFromMessageId = forwardedFromMessageId,
            sentAt = cached.sent_at ?: cached.created_at,
            deliveredAt = cached.delivered_at,
            readAt = cached.read_at,
            editedAt = cached.edited_at,
            deletedAt = cached.deleted_at,
            failureReason = cached.failure_reason,
            createdAt = cached.created_at,
            expiresAt = cached.expires_at,
            readBy = cached.read_by?.split(",") ?: emptyList(),
            reactions = emptyMap(),
            userReaction = null,
            attachments = attachmentsList,
            mediaGroupId = attachmentsList.firstOrNull()?.mediaGroupId,
            metadataContainer = parsedMetadata
        )
    }
}
