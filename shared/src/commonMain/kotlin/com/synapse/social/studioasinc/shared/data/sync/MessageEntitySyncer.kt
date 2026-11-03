package com.synapse.social.studioasinc.shared.data.sync

import com.synapse.social.studioasinc.shared.data.datasource.ChatMessageDataSource
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageAttachmentDto
import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDomain
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.util.TimeProvider
import io.github.aakira.napier.Napier
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Entity syncer for chat messages.
 *
 * Key rules:
 * - Supabase is authoritative: remote wins unless the local copy has an unsynced pending change.
 * - Deleted records are soft-deleted locally — they never reappear from stale cache.
 * - A network failure returns [Result.failure] so the pending action stays in the queue and is
 *   never misinterpreted as a successful deletion.
 *
 * [ChatMessageDataSource] is `internal`, so this syncer is also internal.
 * It is created inside the shared module and exposed to the DI graph via the factory in
 * [MessageEntitySyncerFactory].
 */
internal class MessageEntitySyncer(
    private val messageDataSource: ChatMessageDataSource,
    private val cachedMessageDao: CachedMessageDao
) : BaseEntitySyncer<MessageSyncEntity>("message", "messages") {

    private companion object {
        private const val TAG = "MessageEntitySyncer"
    }

    // ── BaseEntitySyncer contract ─────────────────────────────────────────

    override suspend fun fetchFromRemote(sinceTimestamp: Long): List<MessageSyncEntity> =
        // Messages are always fetched per-chat in SupabaseChatRepository.
        // Global fetch is not required here; realtime handles live updates.
        emptyList()

    override suspend fun getLocalItems(): List<MessageSyncEntity> = emptyList()

    override suspend fun saveLocal(items: List<MessageSyncEntity>) {
        if (items.isEmpty()) return
        cachedMessageDao.upsertAll(items.map { it.message })
    }

    override suspend fun deleteLocal(id: String) {
        cachedMessageDao.delete(id)
    }

    override suspend fun markAsDeletedLocally(ids: List<String>) {
        if (ids.isNotEmpty()) cachedMessageDao.markDeleted(ids)
    }

    override suspend fun shouldPushToRemote(local: MessageSyncEntity): Boolean =
        local.message.deliveryStatus == DeliveryStatus.SENDING

    override fun mapFromRealtimeRecord(record: Map<String, Any?>): MessageSyncEntity? =
        runCatching {
            val id       = record["id"]?.toString()           ?: return@runCatching null
            val chatId   = record["chat_id"]?.toString()      ?: return@runCatching null
            val senderId = record["sender_id"]?.toString()    ?: return@runCatching null
            val content  = record["content"]?.toString()      ?: ""
            val typeStr  = record["message_type"]?.toString() ?: "TEXT"
            val createdAt = record["created_at"]?.toString() ?: TimeProvider.nowInstant().toString()
            val updatedMs = record["updated_at"]?.toString()?.toLongOrNull()
                ?: TimeProvider.nowMillis()
            val isDeleted = record["is_deleted"]?.toString()?.toBoolean() == true

            val message = Message(
                id             = id,
                chatId         = chatId,
                senderId       = senderId,
                content        = if (isDeleted) "" else content,
                messageType    = runCatching { MessageType.valueOf(typeStr.uppercase()) }
                    .getOrDefault(MessageType.TEXT),
                mediaUrl       = if (isDeleted) null else record["media_url"]?.toString(),
                deliveryStatus = DeliveryStatus.SENT,
                contentStatus  = if (isDeleted) ContentStatus.DELETED else ContentStatus.ACTIVE,
                isDeleted      = isDeleted,
                isEdited       = record["is_edited"]?.toString()?.toBoolean() == true,
                createdAt      = createdAt,
                sentAt         = record["sent_at"]?.toString() ?: createdAt,
                editedAt       = record["edited_at"]?.toString(),
                deletedAt      = record["deleted_at"]?.toString(),
                expiresAt      = record["expires_at"]?.toString(),
                replyToId      = record["reply_to_id"]?.toString()
            )
            MessageSyncEntity(message, updatedMs)
        }.getOrElse { e ->
            Napier.e("Failed to map realtime record to MessageSyncEntity", e, TAG)
            null
        }

    // ── Push ──────────────────────────────────────────────────────────────

    override suspend fun pushChange(action: PendingAction): Result<Unit> = runCatching {
        when (action.actionType) {
            PendingAction.ActionType.SEND_MESSAGE ->
                sendPendingMessage(action)
            PendingAction.ActionType.DELETE ->
                messageDataSource.deleteMessage(action.targetId)
            PendingAction.ActionType.EDIT ->
                messageDataSource.editMessage(action.targetId, action.payload ?: return@runCatching)
            else -> Unit
        }
    }

    /**
     * Replays a message queued while offline. The optimistic local row is stored under the
     * pending action id, so it is replaced with the persisted row to avoid a duplicate bubble.
     */
    private suspend fun sendPendingMessage(action: PendingAction) {
        val payload = Json.parseToJsonElement(action.payload ?: "{}").jsonObject
        val attachments = payload["attachments"]
            ?.takeIf { it !is JsonNull }
            ?.let { Json.decodeFromJsonElement(ListSerializer(MessageAttachmentDto.serializer()), it) }

        val sent = messageDataSource.sendMessage(
            chatId = action.targetId,
            content = payload["content"]?.jsonPrimitive?.contentOrNull ?: "",
            mediaUrl = payload["mediaUrl"]?.jsonPrimitive?.contentOrNull,
            messageType = payload["messageType"]?.jsonPrimitive?.contentOrNull ?: "text",
            expiresAt = payload["expiresAt"]?.jsonPrimitive?.contentOrNull,
            replyToId = payload["replyToId"]?.jsonPrimitive?.contentOrNull,
            attachments = attachments,
            metadata = payload["metadata"]?.jsonPrimitive?.contentOrNull
        )

        // The message is already on the server: a cache failure must not fail the push, or the
        // retry would send it a second time.
        runCatching {
            cachedMessageDao.delete(action.id)
            cachedMessageDao.upsert(sent.toDomain())
        }.onFailure { Napier.w("Sent message ${action.id} but could not update the cache", it, TAG) }
    }

    override suspend fun clearLocal() {
        cachedMessageDao.deleteAll()
        Napier.d("Cleared all cached messages", tag = TAG)
    }
}

/**
 * Factory that creates a [MessageEntitySyncer] — the only public surface needed outside
 * the module, since the syncer itself is `internal`.
 * The factory is also `internal` because [ChatMessageDataSource] is `internal`.
 * The [SyncModule] (inside the same module) calls this directly.
 */
internal object MessageEntitySyncerFactory {
    fun create(
        messageDataSource: ChatMessageDataSource,
        cachedMessageDao: CachedMessageDao
    ): EntitySyncer<MessageSyncEntity> =
        MessageEntitySyncer(messageDataSource, cachedMessageDao)
}

// ── Wrapper ───────────────────────────────────────────────────────────────────

/**
 * Wraps [Message] to satisfy [SyncableEntity] without modifying the domain model.
 */
data class MessageSyncEntity(
    val message: Message,
    override val updatedAt: Long
) : SyncableEntity {
    override val id: String         get() = message.id
    override val isDeleted: Boolean get() = message.isDeleted ||
        message.contentStatus == ContentStatus.DELETED
    override val syncVersion: Int   get() = 1
}
