package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.datasource.ChatReactionDataSource
import com.synapse.social.studioasinc.shared.data.datasource.NotAuthenticatedException
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseUserDataSource
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.local.database.CachedConversationDao
import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.data.local.database.MessageReactionDao
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.MessageChronologicalComparator
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.mergeMonotonic
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDomain
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDto
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.util.EncryptedPayloadDetector
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageMetadataContainer
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import com.synapse.social.studioasinc.shared.util.Logger
import com.synapse.social.studioasinc.shared.util.TimeProvider
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import com.synapse.social.studioasinc.shared.util.UUIDUtils
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Repository implementation for managing chat data using Supabase as the remote backend.
 */
class SupabaseChatRepository(
    private val dataSource: SupabaseChatDataSource = SupabaseChatDataSource(),
    private val client: SupabaseClientLib = SupabaseClient.client,
    private val signalProtocolManager: SignalProtocolManager? = null,
    private val mediaUploadRepository: MediaUploadRepository,
    private val presenceRepository: PresenceRepository? = null,
    private val offlineActionRepository: OfflineActionRepository? = null,
    private val cachedMessageDao: CachedMessageDao? = null,
    private val cachedConversationDao: CachedConversationDao? = null,
    private val reactionDao: MessageReactionDao? = null,
    private val externalScope: CoroutineScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + AppDispatchers.IO)
) : ChatRepository {

    private val encryptionHelper = ChatEncryptionHelper(signalProtocolManager, dataSource, cachedMessageDao)
    private val groupRepository = ChatGroupRepository(dataSource)
    private val reactionDataSource = ChatReactionDataSource(client)
    private val conversationMutex = Mutex()

    // Per-chat mutexes — serializes concurrent cache writes for the same chat ID.
    // Prevents race conditions when real-time WebSocket delivery and inbox refresh
    // both attempt to write to the same chat's message cache simultaneously.
    // chatCacheMutexLock guards the map itself so new entries are created atomically.
    private val chatCacheMutexLock = Mutex()
    private val chatCacheMutexes = mutableMapOf<String, Mutex>()

    private suspend fun getCacheMutexForChat(chatId: String): Mutex =
        chatCacheMutexLock.withLock {
            chatCacheMutexes.getOrPut(chatId) { Mutex() }
        }

    // In-memory last-sync timestamps per chat — avoids redundant network fetches for
    // the active chat when messages were just written via a real-time event.
    // Uses epoch milliseconds; entries absent from the map mean "never synced".
    private val chatLastSyncedAt = mutableMapOf<String, Long>()
    private val chatLastSyncedAtLock = Mutex()

    private suspend fun recordCacheSyncTime(chatId: String) {
        chatLastSyncedAtLock.withLock {
            chatLastSyncedAt[chatId] = TimeProvider.nowMillis()
        }
    }

    private suspend fun isCacheFresh(chatId: String, maxAgeMs: Long = 30_000L): Boolean =
        chatLastSyncedAtLock.withLock {
            val lastSync = chatLastSyncedAt[chatId] ?: return@withLock false
            (TimeProvider.nowMillis() - lastSync) < maxAgeMs
        }

    // Device ID management - will be injected via constructor in future
    // For now, placeholder implementation
    private var currentDeviceId: String? = null
    
    override fun setCurrentDeviceId(deviceId: String?) {
        currentDeviceId = deviceId
    }
    
    override fun getCurrentDeviceId(): String? = currentDeviceId

    /** Raw envelopes and failed decryptions must never replace a readable cached message. */
    private fun Message.hasReadableContent(): Boolean =
        encryptionFailureReason == null &&
            !EncryptedPayloadDetector.isPlaceholder(content) &&
            !EncryptedPayloadDetector.isEncryptedPayload(content)

    private fun conversationPreview(message: Message): String =
        if (message.hasReadableContent()) message.content else EncryptedPayloadDetector.PLACEHOLDER

    // Receipt UPDATEs replay older rows, so only a genuinely newer message may move a conversation forward.
    private fun isMoreRecent(createdAt: String?, reference: String?): Boolean {
        val current = ChatMapper.parseInstant(createdAt)
        val existing = ChatMapper.parseInstant(reference)
        return when {
            current != null && existing != null -> current.compareTo(existing) > 0
            current != null -> true
            existing != null -> false
            else -> (createdAt ?: "").compareTo(reference ?: "") > 0
        }
    }

    // Active chat registry — tracks which chat screen is currently open.
    // Used by inbox refresh to avoid overwriting an active chat's real-time message cache.
    private val _activeChatId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun setActiveChatId(chatId: String?) {
        _activeChatId.value = chatId
    }

    override fun getActiveChatId(): String? = _activeChatId.value

    override suspend fun reconcileRealtimeMessage(
        chatId: String,
        message: Message
    ): Message = withContext(AppDispatchers.IO) {
        try {
            getCacheMutexForChat(chatId).withLock {
                val cached = cachedMessageDao?.getMessageById(message.id)
                val merged = cached?.mergeMonotonic(message) ?: message
                if (merged.hasReadableContent()) {
                    cachedMessageDao?.upsert(merged)
                    recordCacheSyncTime(chatId)
                }
                merged
            }
        } catch (e: Exception) {
            Logger.e("Failed to reconcile real-time message ${message.id}", throwable = e)
            message
        }
    }

    override suspend fun getMessagesByIds(chatId: String, messageIds: List<String>): Result<List<Message>> =
        withContext(AppDispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
                val messages = dataSource.getMessagesByIds(chatId, messageIds).map { dto ->
                    val decrypted = encryptionHelper.run { dto.decryptIfNecessary(userId).toDomain() }
                    reconcileRealtimeMessage(chatId, decrypted)
                }
                Result.success(messages)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Result.failure(e)
            }
        }

    override suspend fun searchMessages(chatId: String, query: String): Result<List<Message>> = withContext(AppDispatchers.IO) {
        try {
            val allMessages = getMessages(chatId, limit = 200, forceNetwork = false).getOrElse { emptyList() }
            val lowerQuery = query.lowercase()
            val filtered = allMessages.filter { message ->
                message.content?.lowercase()?.contains(lowerQuery) == true ||
                message.attachments?.any { it.type.name.lowercase().contains(lowerQuery) } == true
            }
            Result.success(filtered)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun ensureSession(userId: String) = withContext(AppDispatchers.IO) {
        encryptionHelper.ensureSession(userId)
    }

    private suspend fun fetchConversationsFromNetwork(archivedOnly: Boolean = false): List<Conversation> = withContext(AppDispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        dataSource.getInboxSummaries(archivedOnly).map { summary ->
            val lastMessageText = summary.lastMessage?.let { last ->
                try {
                    val decrypted = encryptionHelper.run { last.decryptIfNecessary(currentUserId) }
                    if (decrypted.encryptionFailureReason != null ||
                        EncryptedPayloadDetector.isPlaceholder(decrypted.content) ||
                        EncryptedPayloadDetector.isEncryptedPayload(decrypted.content)
                    ) {
                        EncryptedPayloadDetector.PLACEHOLDER
                    } else {
                        decrypted.content
                    }
                } catch (_: Exception) {
                    EncryptedPayloadDetector.PLACEHOLDER
                }
            } ?: "No messages yet"
            val isOnline = summary.participantStatus?.equals("ONLINE", ignoreCase = true) == true &&
                TimestampFormatter.isWithinActiveWindow(summary.participantLastSeen, windowMinutes = 2)
            Conversation(
                chatId = summary.chatId,
                participantId = summary.participantId,
                participantName = summary.participantName?.takeIf { it.isNotBlank() }
                    ?: if (summary.isGroup) "Group Chat" else {
                        val userDataSource = SupabaseUserDataSource(client)
                        userDataSource.getUserProfile(summary.participantId).getOrNull()?.let { it.displayName ?: it.name ?: it.nickname ?: it.username }?.takeIf { it.isNotBlank() } ?: "Unknown User"
                    },
                participantAvatar = summary.participantAvatar,
                lastMessage = lastMessageText,
                lastMessageTime = summary.lastMessage?.createdAt,
                unreadCount = summary.unreadCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                isOnline = isOnline,
                isGroup = summary.isGroup,
                isPinned = summary.isPinned,
                isMuted = summary.isMuted
            )
        }
    }

    override suspend fun getConversations(forceNetwork: Boolean, limit: Int, before: String?, beforeId: String?): Result<List<Conversation>> = withContext(AppDispatchers.IO) {
        try {
            val cached = cachedConversationDao?.getAll() ?: emptyList()
            if (before == null && beforeId == null) {
                if (forceNetwork) {
                    try {
                        val fresh = fetchConversationsFromNetwork()
                        cachedConversationDao?.upsertAll(fresh)
                        Result.success(fresh.take(limit))
                    } catch (throwable: Throwable) {
                        if (cached.isNotEmpty()) Result.success(cached.take(limit)) else Result.failure(throwable)
                    }
                } else if (cached.isNotEmpty()) {
                    externalScope.launch {
                        try {
                            val fresh = fetchConversationsFromNetwork()
                            cachedConversationDao?.upsertAll(fresh)
                        } catch (e: Exception) {
                            Logger.e("Failed to sync fresh conversations", throwable = e)
                        }
                    }
                    Result.success(cached.take(limit))
                } else {
                    val conversations = fetchConversationsFromNetwork()
                    cachedConversationDao?.upsertAll(conversations)
                    Result.success(conversations.take(limit))
                }
            } else {
                try {
                    val fresh = fetchConversationsFromNetwork()
                    Result.success(fresh.take(limit))
                } catch (throwable: Throwable) {
                    if (cached.isNotEmpty()) Result.success(cached.take(limit)) else Result.failure(throwable)
                }
            }
        } catch (throwable: Throwable) {
            val cached = cachedConversationDao?.getAll() ?: emptyList()
            if (cached.isNotEmpty()) Result.success(cached.take(limit))
            else {
                Logger.e("Error getting conversations", throwable = throwable)
                Result.failure(throwable)
            }
        }
    }

    override suspend fun getArchivedConversations(limit: Int, before: String?, beforeId: String?): Result<List<Conversation>> = withContext(AppDispatchers.IO) {
        try {
            Result.success(fetchConversationsFromNetwork(archivedOnly = true).take(limit))
        } catch (throwable: Throwable) {
            Logger.e("Error getting archived conversations", throwable = throwable)
            Result.failure(throwable)
        }
    }

    override suspend fun updateConversationPinnedStatus(chatId: String, isPinned: Boolean): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.updateConversationPinnedStatus(chatId, isPinned)
            Result.success(Unit)
        } catch (throwable: Throwable) {
            Logger.e("Error updating conversation pin status", throwable = throwable)
            Result.failure(throwable)
        }
    }

    override suspend fun updateConversationMutedStatus(chatId: String, isMuted: Boolean): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.updateConversationMutedStatus(chatId, isMuted)
            Result.success(Unit)
        } catch (throwable: Throwable) {
            Logger.e("Error updating conversation mute status", throwable = throwable)
            Result.failure(throwable)
        }
    }

    override suspend fun getMessages(chatId: String, limit: Int, before: String?, beforeId: String?, forceNetwork: Boolean): Result<List<Message>> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")

            if (before != null && beforeId != null && !forceNetwork) {
                val cachedBefore = cachedMessageDao?.getMessagesBefore(chatId, before, beforeId, limit) ?: emptyList()
                if (cachedBefore.isNotEmpty()) {
                    val userDeletedIds = dataSource.getUserDeletedMessageIds(cachedBefore.map { it.id })
                    if (userDeletedIds.isNotEmpty()) {
                        cachedMessageDao?.delete(userDeletedIds.toList())
                    }
                    val visibleCached = cachedBefore.filterNot { it.id in userDeletedIds }
                    if (visibleCached.isNotEmpty()) {
                        return@withContext Result.success(visibleCached)
                    }
                }
            }

            if (before == null && !forceNetwork) {
                val cached = cachedMessageDao?.getMessages(chatId, limit) ?: emptyList()
                val userDeletedIds = if (cached.isEmpty()) {
                    emptySet()
                } else {
                    dataSource.getUserDeletedMessageIds(cached.map { it.id })
                }
                if (userDeletedIds.isNotEmpty()) {
                    cachedMessageDao?.delete(userDeletedIds.toList())
                }
                val visibleCached = cached.filterNot { it.id in userDeletedIds }
                val hasUndecryptable = visibleCached.any { !it.hasReadableContent() }
                if (visibleCached.isNotEmpty() && !hasUndecryptable) {
                    syncMessagesInBackground(chatId, limit, currentUserId, visibleCached)
                    return@withContext Result.success(visibleCached)
                }
            }

            val messageDtos = dataSource.getMessages(chatId, limit, before, beforeId)
            val decrypted = messageDtos.map { encryptionHelper.run { it.decryptIfNecessary(currentUserId).toDomain() } }
            val sortedDecrypted = decrypted.sortedWith(MessageChronologicalComparator)

            if (before == null) {
                val activeChatId = getActiveChatId()
                if (activeChatId != null && activeChatId.equals(chatId, ignoreCase = true)) {
                    getCacheMutexForChat(chatId).withLock {
                        val existingCached = cachedMessageDao?.getMessages(chatId, limit) ?: emptyList()
                        val toDelete = reconcileServerMessagesWithCache(existingCached, sortedDecrypted, isRealTimeSource = false)
                        if (toDelete.isNotEmpty()) {
                            cachedMessageDao?.markDeleted(toDelete)
                        }
                        val decryptableForCache = sortedDecrypted.filter { it.hasReadableContent() }
                        if (decryptableForCache.isNotEmpty()) {
                            cachedMessageDao?.upsertAll(decryptableForCache)
                            recordCacheSyncTime(chatId)
                        }
                    }
                } else {
                    val decryptableForCache = sortedDecrypted.filter { it.hasReadableContent() }
                    if (decryptableForCache.isNotEmpty()) {
                        cachedMessageDao?.upsertAll(decryptableForCache)
                    }
                }
                cachedMessageDao?.trimToLimit(chatId, limit)
            }
            Result.success(sortedDecrypted)
        } catch (t: Throwable) {
            val cached = cachedMessageDao?.getMessages(chatId, limit) ?: emptyList()
            if (cached.isNotEmpty()) Result.success(cached)
            else {
                Logger.e("Error getting messages", throwable = t)
                Result.failure(t)
            }
        }
    }

    override suspend fun getMessagesAfter(chatId: String, after: String, afterId: String, limit: Int): Result<List<Message>> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
            val messageDtos = dataSource.getMessagesAfter(chatId, after, afterId, limit)
            val decrypted = messageDtos
                .map { encryptionHelper.run { it.decryptIfNecessary(currentUserId).toDomain() } }
                .sortedWith(MessageChronologicalComparator)
            val cacheable = decrypted.filter { it.hasReadableContent() }
            if (cacheable.isNotEmpty()) {
                cachedMessageDao?.upsertAll(cacheable)
                cachedMessageDao?.trimToLimit(chatId, limit)
            }
            Result.success(decrypted)
        } catch (t: Throwable) {
            Logger.e("Error syncing messages after cursor", throwable = t)
            Result.failure(t)
        }
    }

    override suspend fun getMessagesCacheFirst(chatId: String, limit: Int, refreshInBackground: Boolean, before: String?, beforeId: String?): Result<List<Message>> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
            if (before != null && beforeId != null) {
                val cachedBefore = cachedMessageDao?.getMessagesBefore(chatId, before, beforeId, limit) ?: emptyList()
                if (cachedBefore.isNotEmpty()) {
                    val userDeletedIds = dataSource.getUserDeletedMessageIds(cachedBefore.map { it.id })
                    if (userDeletedIds.isNotEmpty()) cachedMessageDao?.delete(userDeletedIds.toList())
                    val visibleCached = cachedBefore.filterNot { it.id in userDeletedIds }
                    if (visibleCached.isNotEmpty()) return@withContext Result.success(visibleCached)
                }
            }
            val cached = cachedMessageDao?.getMessages(chatId, limit) ?: emptyList()
            val userDeletedIds = if (cached.isEmpty()) emptySet() else dataSource.getUserDeletedMessageIds(cached.map { it.id })
            if (userDeletedIds.isNotEmpty()) cachedMessageDao?.delete(userDeletedIds.toList())
            val visibleCached = cached.filterNot { it.id in userDeletedIds }
            // Refresh whenever the cache is stale — including when it still holds undecryptable
            // rows, which is exactly when a network pass is most likely to repair the display.
            if (refreshInBackground && visibleCached.isNotEmpty() && !isCacheFresh(chatId)) {
                syncMessagesInBackground(chatId, limit, currentUserId, visibleCached)
            }
            Result.success(visibleCached)
        } catch (t: Throwable) {
            val cached = cachedMessageDao?.getMessages(chatId, limit) ?: emptyList()
            if (cached.isNotEmpty()) Result.success(cached) else Result.failure(t)
        }
    }

    private fun parseInstantEpochOrNull(timestamp: String?): Long? {
        if (timestamp.isNullOrBlank()) return null
        return try {
            kotlinx.datetime.Instant.parse(timestamp).toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }
    }

    internal fun reconcileServerMessagesWithCache(
        cached: List<Message>,
        fresh: List<Message>,
        isRealTimeSource: Boolean = false
    ): List<String> {
        // When isRealTimeSource=true, the caller is a WebSocket event handler that has higher authority
        // over delivery/read status than background sync. Deletion detection logic is the same regardless
        // of source — both paths correctly detect server-side deletes.
        val deletedIds = mutableListOf<String>()

        fresh.filter { it.isDeleted || it.contentStatus == com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus.DELETED }
            .forEach { deletedIds.add(it.id) }

        val freshInstants = fresh.mapNotNull { parseInstantEpochOrNull(it.sentAt ?: it.createdAt) }
        val oldestFreshEpoch = freshInstants.minOrNull()

        if (oldestFreshEpoch != null) {
            val freshIds = fresh.map { it.id }.toSet()
            val deletedInWindow = cached.filter { cachedMsg ->
                val cachedEpoch = parseInstantEpochOrNull(cachedMsg.sentAt ?: cachedMsg.createdAt)
                cachedMsg.id !in freshIds &&
                cachedMsg.deliveryStatus != com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.SENDING &&
                cachedMsg.deliveryStatus != com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.FAILED &&
                cachedEpoch != null && cachedEpoch >= oldestFreshEpoch
            }
            deletedIds.addAll(deletedInWindow.map { it.id })
        }

        return deletedIds.distinct()
    }

    private fun syncMessagesInBackground(chatId: String, limit: Int, currentUserId: String, cached: List<Message>) {
        externalScope.launch {
            try {
                val fresh = dataSource.getMessages(chatId, limit, null, null)
                val decrypted = fresh
                    .map { encryptionHelper.run { it.decryptIfNecessary(currentUserId).toDomain() } }
                    .sortedWith(MessageChronologicalComparator)

                val toDelete = reconcileServerMessagesWithCache(cached, decrypted, isRealTimeSource = false)
                if (toDelete.isNotEmpty()) {
                    cachedMessageDao?.markDeleted(toDelete)
                }

                val mergedMessages = decrypted.map { freshMsg ->
                    if (!freshMsg.hasReadableContent()) {
                        cached.firstOrNull { it.id == freshMsg.id && it.hasReadableContent() }?.mergeMonotonic(freshMsg) ?: freshMsg
                    } else {
                        freshMsg
                    }
                }
                val validToCache = mergedMessages.filter { it.hasReadableContent() }
                if (validToCache.isNotEmpty()) {
                    cachedMessageDao?.upsertAll(validToCache)
                    recordCacheSyncTime(chatId)
                }
                cachedMessageDao?.trimToLimit(chatId, limit)
            } catch (e: Exception) {
                Logger.e("Failed to sync fresh messages", throwable = e)
            }
        }
    }

    override suspend fun getMessageById(messageId: String): Result<Message?> = withContext(AppDispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.success(null)
            val dto = dataSource.getMessageById(messageId) ?: return@withContext Result.success(null)
            val decrypted = encryptionHelper.run { dto.decryptIfNecessary(userId) }
            Result.success(decrypted.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(
        chatId: String,
        content: String,
        mediaUrl: String?,
        messageType: String,
        expiresAt: String?,
        replyToId: String?,
        senderPlaintext: String?,
        attachments: List<MessageAttachment>?,
        metadataContainer: MessageMetadataContainer?
    ): Result<Message> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
            val dtoAttachments = attachments?.map { it.toDto() }

            val serializedMetadata = metadataContainer?.let {
                try {
                    Json.encodeToString(MessageMetadataContainer.serializer(), it)
                } catch (e: Exception) {
                    null
                }
            }

            // Calculate expiresAt based on chat's disappearing mode if not provided
            val calculatedExpiresAt = if (expiresAt == null && messageType != "system") {
                try {
                    val mode = dataSource.getDisappearingMode(chatId)
                    mode?.let { modeString ->
                        val modeEnum = try { DisappearingMode.valueOf(modeString) } catch (e: Exception) { DisappearingMode.OFF }
                        if (modeEnum != DisappearingMode.OFF && modeEnum.seconds != null) {
                            val now = com.synapse.social.studioasinc.shared.util.TimeProvider.nowInstant()
                            val expiresAtInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + (modeEnum.seconds * 1000))
                            expiresAtInstant.toString()
                        } else null
                    }
                } catch (e: Exception) {
                    Logger.w("Failed to get disappearing mode for message expiration", throwable = e)
                    null
                }
            } else expiresAt

            // Get current device ID from secure storage if available
            val deviceId = getCurrentDeviceId()

            val messageDto = try {
                dataSource.sendMessage(chatId, content, mediaUrl, messageType, calculatedExpiresAt, replyToId, dtoAttachments, serializedMetadata, deviceId)
            } catch (e: Exception) {
                if (offlineActionRepository != null) {
                    queueMessageOffline(chatId, content, mediaUrl, messageType, calculatedExpiresAt, replyToId, currentUserId, attachments, metadataContainer)
                } else throw e
            }

            val domainMessage = messageDto.toDomain()
            val finalMessage = if (senderPlaintext != null) {
                val displayContent = try {
                    Json.parseToJsonElement(senderPlaintext).jsonObject["content"]?.jsonPrimitive?.contentOrNull ?: senderPlaintext
                } catch (_: Exception) { senderPlaintext }

                val decryptedMediaUrl = try {
                    Json.parseToJsonElement(senderPlaintext).jsonObject["mediaUrl"]?.jsonPrimitive?.contentOrNull
                } catch (_: Exception) { null }
                val cachedMsg = domainMessage.copy(
                    content = displayContent,
                    mediaUrl = decryptedMediaUrl ?: domainMessage.mediaUrl,
                    metadataContainer = metadataContainer
                )
                cachedMessageDao?.upsert(cachedMsg)
                encryptionHelper.cacheDecrypted(messageDto.id ?: "", senderPlaintext)
                cachedMsg
            } else {
                val cachedMsg = domainMessage.copy(metadataContainer = metadataContainer)
                cachedMessageDao?.upsert(cachedMsg)
                cachedMsg
            }
            Result.success(finalMessage)
        } catch (e: Exception) {
            Logger.e("Error sending message", throwable = e)
            Result.failure(e)
        }
    }

    private suspend fun queueMessageOffline(
        chatId: String, content: String, mediaUrl: String?,
        messageType: String, expiresAt: String?, replyToId: String?,
        currentUserId: String, attachments: List<MessageAttachment>?,
        metadataContainer: MessageMetadataContainer? = null
    ): MessageDto {
        val actionId = UUIDUtils.randomUUID()
        val dtoAttachments = attachments?.map { it.toDto() }
        val attachmentsJson = if (dtoAttachments != null) {
            Json.encodeToJsonElement(
                kotlinx.serialization.builtins.ListSerializer(com.synapse.social.studioasinc.shared.data.dto.chat.MessageAttachmentDto.serializer()),
                dtoAttachments
            )
        } else null

        val serializedMetadata = metadataContainer?.let {
            try {
                Json.encodeToString(MessageMetadataContainer.serializer(), it)
            } catch (e: Exception) { null }
        }

        offlineActionRepository?.addAction(
            PendingAction(
                id = actionId,
                actionType = PendingAction.ActionType.SEND_MESSAGE,
                targetId = chatId,
                payload = buildJsonObject {
                    put("content", content)
                    if (mediaUrl != null) put("mediaUrl", mediaUrl)
                    put("messageType", messageType)
                    if (expiresAt != null) put("expiresAt", expiresAt)
                    if (replyToId != null) put("replyToId", replyToId)
                    if (attachmentsJson != null) put("attachments", attachmentsJson)
                    if (serializedMetadata != null) put("metadata", serializedMetadata)
                }.toString()
            )
        )
        return MessageDto(
            id = actionId,
            chatId = chatId,
            senderId = currentUserId,
            content = content,
            messageType = messageType,
            mediaUrl = mediaUrl,
            createdAt = TimeProvider.nowInstant().toString(),
            attachments = dtoAttachments,
            metadata = serializedMetadata
        )
    }

    override suspend fun getOrCreateChat(otherUserId: String): Result<String> = withContext(AppDispatchers.IO) {
        try {
            val chatId = dataSource.getOrCreateChat(otherUserId)
            Result.success(chatId)
        } catch (e: Exception) {
            Logger.e("Error creating chat", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun updateConversationArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.updateConversationArchiveStatus(chatId, isArchived)
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error updating conversation archive status", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun getConversationMuteStatus(chatId: String): Result<Boolean> = withContext(AppDispatchers.IO) {
        try {
            Result.success(dataSource.getConversationMuteStatus(chatId))
        } catch (e: Exception) {
            Logger.e("Error reading conversation mute status", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun setConversationMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.setConversationMuteStatus(chatId, isMuted)
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error updating conversation mute status", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun deleteConversation(chatId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.deleteConversation(chatId)
            cachedConversationDao?.deleteByChatId(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error deleting conversation", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun markMessagesAsRead(chatId: String, shareReceipt: Boolean): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            val deviceId = getCurrentDeviceId()
            dataSource.markMessagesAsRead(chatId, deviceId, shareReceipt)
            val currentUserId = getCurrentUserId()
            if (currentUserId != null) {
                cachedMessageDao?.markRead(chatId, currentUserId)
            }
            conversationMutex.withLock {
                val dao = cachedConversationDao
                if (dao != null) {
                    val existing = dao.getAll().find { it.chatId == chatId }
                    if (existing != null) {
                        dao.upsertAll(listOf(existing.copy(unreadCount = 0)))
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Logger.e("Error marking messages as read", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun markMessagesAsDelivered(chatId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            val deviceId = getCurrentDeviceId()
            dataSource.markMessagesAsDelivered(chatId, deviceId)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Logger.e("Error marking messages as delivered", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.deleteMessage(messageId)
            cachedMessageDao?.markDeleted(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error deleting message", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun deleteMessages(messageIds: List<String>): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.deleteMessages(messageIds)
            cachedMessageDao?.markDeleted(messageIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error deleting messages", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun setDisappearingMode(chatId: String, mode: DisappearingMode): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.setDisappearingMode(chatId, mode.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error setting disappearing mode", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun getDisappearingMode(chatId: String): Result<DisappearingMode> = withContext(AppDispatchers.IO) {
        try {
            val modeString = dataSource.getDisappearingMode(chatId)
            val mode = try {
                modeString?.let { DisappearingMode.valueOf(it) } ?: DisappearingMode.OFF
            } catch (e: Exception) { DisappearingMode.OFF }
            Result.success(mode)
        } catch (e: Exception) {
            Logger.e("Error getting disappearing mode", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun voteInPoll(messageId: String, optionId: String, pollId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
            val dto = dataSource.getMessageById(messageId) ?: throw Exception("Poll message not found")
            val domainMessage = dto.toDomain()
            val poll = domainMessage.metadataContainer?.poll ?: throw Exception("Poll metadata not found")

            if (poll.isClosed) {
                throw Exception("Poll is closed")
            }

            val updatedOptions = poll.options.map { option ->
                if (option.id == optionId) {
                    val currentVoters = option.voterUserIds.toSet()
                    val newVoters = if (currentUserId in currentVoters) currentVoters - currentUserId else currentVoters + currentUserId
                    option.copy(
                        voteCount = newVoters.size,
                        voterUserIds = newVoters.toList()
                    )
                } else if (!poll.allowMultipleAnswers) {
                    val currentVoters = option.voterUserIds.toSet() - currentUserId
                    option.copy(
                        voteCount = currentVoters.size,
                        voterUserIds = currentVoters.toList()
                    )
                } else option
            }

            val updatedContainer = domainMessage.metadataContainer?.copy(poll = poll.copy(options = updatedOptions))
            val serializedMetadata = updatedContainer?.let {
                try {
                    Json.encodeToString(MessageMetadataContainer.serializer(), it)
                } catch (e: Exception) { null }
            }

            dataSource.editMessageMetadata(messageId, serializedMetadata)

            val cachedMsg = domainMessage.copy(metadataContainer = updatedContainer)
            cachedMessageDao?.upsert(cachedMsg)

            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error voting in poll", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun markViewOnceConsumed(messageId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
            val dto = dataSource.getMessageById(messageId) ?: throw Exception("Message not found")
            val domainMessage = dto.toDomain()
            val viewOnce = domainMessage.metadataContainer?.viewOnce ?: throw Exception("View-once metadata not found")

            val updatedViewOnce = viewOnce.copy(isConsumed = true, remainingViews = 0)
            val updatedContainer = domainMessage.metadataContainer?.copy(viewOnce = updatedViewOnce)
            val serializedMetadata = updatedContainer?.let {
                try {
                    Json.encodeToString(MessageMetadataContainer.serializer(), it)
                } catch (e: Exception) { null }
            }

            dataSource.editMessageMetadata(messageId, serializedMetadata)

            val cachedMsg = domainMessage.copy(metadataContainer = updatedContainer)
            cachedMessageDao?.upsert(cachedMsg)

            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error marking view-once consumed", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun deleteMessageForMe(messageId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.deleteMessageForMe(messageId)
            cachedMessageDao?.delete(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessagesForMe(messageIds: List<String>): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.deleteMessagesForMe(messageIds)
            cachedMessageDao?.delete(messageIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error deleting messages for me", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
            val originalMessage = dataSource.getMessageById(messageId) ?: throw Exception("Message not found")

            val groupMembers = dataSource.getGroupMembers(originalMessage.chatId)
            val otherParticipants = groupMembers.map { it.first.uid }.filter { it != currentUserId }

            val encryptedPayload = if (signalProtocolManager != null && otherParticipants.isNotEmpty()) {
                encryptEditPayload(newContent, currentUserId, otherParticipants)
            } else newContent

            dataSource.editMessage(messageId, encryptedPayload)
            cachedMessageDao?.updateContent(messageId, newContent)
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error editing message", throwable = e)
            Result.failure(e)
        }
    }

    private suspend fun encryptEditPayload(newContent: String, currentUserId: String, otherParticipants: List<String>): String = coroutineScope {
        val jsonPayload = buildJsonObject { put("content", newContent) }.toString()
        val contentBytes = jsonPayload.encodeToByteArray()

        val encryptedPayloads = otherParticipants.map { otherUserId ->
            async {
                ensureSession(otherUserId)
                val encrypted = signalProtocolManager!!.encryptMessage(otherUserId, contentBytes)
                otherUserId to Json.encodeToJsonElement(EncryptedMessage.serializer(), encrypted)
            }
        }.awaitAll()

        buildJsonObject {
            encryptedPayloads.forEach { (userId, element) -> put(userId, element) }
            put(currentUserId, jsonPayload)
        }.toString()
    }

    override suspend fun uploadMedia(chatId: String, filePath: String, fileName: String, contentType: String, provider: StorageProvider?, config: StorageConfig?, onProgress: ((Int) -> Unit)?): Result<String> = withContext(AppDispatchers.IO) {
        mediaUploadRepository.upload(
            filePath = filePath,
            provider = provider ?: StorageProvider.PRIMARY,
            config = config ?: StorageConfig(),
            bucketName = "chat-attachments",
            onProgress = { floatProgress -> onProgress?.invoke((floatProgress * 100).toInt()) }
        ).map { it.url }.onFailure { Logger.e("Error uploading media", throwable = it) }
    }

    override suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.broadcastTypingStatus(chatId, isTyping)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Logger.e("Error broadcasting typing status", throwable = e)
            Result.failure(e)
        }
    }

    override fun subscribeToMessages(chatId: String, onConnected: suspend () -> Unit): Flow<Message> =
        dataSource.subscribeToMessages(chatId, onConnected).map { dto ->
            val userId = getCurrentUserId() ?: ""
            val decrypted = if (userId.isNotBlank()) encryptionHelper.run { dto.decryptIfNecessary(userId).toDomain() } else dto.toDomain()
            val domainMessage = reconcileRealtimeMessage(chatId, decrypted)

            externalScope.launch {
                conversationMutex.withLock {
                    val dao = cachedConversationDao
                    if (dao != null) {
                        val existing = dao.getAll().find { it.chatId == domainMessage.chatId }
                        if (existing != null) {
                            val isNewest = isMoreRecent(domainMessage.createdAt, existing.lastMessageTime)
                            dao.upsertAll(listOf(existing.copy(
                                lastMessage = if (isNewest) conversationPreview(domainMessage) else existing.lastMessage,
                                lastMessageTime = if (isNewest) domainMessage.createdAt else existing.lastMessageTime,
                                unreadCount = 0
                            )))
                        }
                    }
                }
            }
            domainMessage
        }

    override fun subscribeToInboxUpdates(chatIds: List<String>): Flow<Message> =
        dataSource.subscribeToInboxUpdates(chatIds).map { dto ->
            val userId = getCurrentUserId() ?: ""
            val domainMessage = if (userId.isNotBlank()) encryptionHelper.run { dto.decryptIfNecessary(userId).toDomain() } else dto.toDomain()

            externalScope.launch {
                if (domainMessage.encryptionFailureReason == null && !EncryptedPayloadDetector.isEncryptedPayload(domainMessage.content) && !encryptionHelper.isPlaceholder(domainMessage.content)) {
                    getCacheMutexForChat(domainMessage.chatId).withLock {
                        cachedMessageDao?.upsert(domainMessage)
                        recordCacheSyncTime(domainMessage.chatId)
                    }
                }
                val existing = cachedConversationDao?.getAll()?.find { it.chatId == domainMessage.chatId }
                val chatInfo = if (existing == null) dataSource.getChatInfo(domainMessage.chatId) else null

                conversationMutex.withLock {
                    val currentExisting = cachedConversationDao?.getAll()?.find { it.chatId == domainMessage.chatId }
                    val updated = if (currentExisting != null) {
                        val isNewest = isMoreRecent(domainMessage.createdAt, currentExisting.lastMessageTime)
                        currentExisting.copy(
                            lastMessage = if (isNewest) conversationPreview(domainMessage) else currentExisting.lastMessage,
                            lastMessageTime = if (isNewest) domainMessage.createdAt else currentExisting.lastMessageTime,
                            unreadCount = when {
                                !isNewest || domainMessage.senderId == userId -> currentExisting.unreadCount
                                // The open chat already zeroes its badge via subscribeToMessages;
                                // incrementing here would race and resurrect a phantom badge.
                                domainMessage.chatId == _activeChatId.value -> currentExisting.unreadCount
                                else -> currentExisting.unreadCount + 1
                            }
                        )
                    } else {
                        val isGroup = chatInfo?.isGroup ?: false
                        Conversation(
                            chatId = domainMessage.chatId,
                            participantId = if (isGroup) domainMessage.chatId else domainMessage.senderId,
                            participantName = if (isGroup) chatInfo?.name ?: "Group Chat" else try { SupabaseUserDataSource(client).getUserProfile(domainMessage.senderId).getOrNull()?.let { it.displayName ?: it.name ?: it.nickname ?: it.username } ?: "Unknown User" } catch (_: Exception) { "Unknown User" },
                            participantAvatar = if (isGroup) chatInfo?.avatarUrl else null,
                            lastMessage = conversationPreview(domainMessage),
                            lastMessageTime = domainMessage.createdAt,
                            unreadCount = if (domainMessage.senderId == userId) 0 else 1,
                            isOnline = false,
                            isGroup = isGroup
                        )
                    }
                    cachedConversationDao?.upsertAll(listOf(updated))
                }
            }
            domainMessage
        }

    override fun subscribeToTypingStatus(chatId: String): Flow<TypingStatus> =
        dataSource.subscribeToTypingStatus(chatId).map { data ->
            TypingStatus(userId = data["user_id"] as? String ?: "", chatId = chatId, isTyping = data["is_typing"] as? Boolean ?: false)
        }

    override fun subscribeToReadReceipts(chatId: String): Flow<Message> =
        subscribeToMessages(chatId).filter { it.deliveryStatus == com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.DELIVERED || it.deliveryStatus == com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.READ }

    override suspend fun initializeE2EE(): Result<Unit> = withContext(AppDispatchers.IO) {
        encryptionHelper.initializeE2EE(getCurrentUserId())
    }

    override fun getCurrentUserId(): String? = dataSource.getCurrentUserId()

    override suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String?) = withContext(AppDispatchers.IO) { groupRepository.createGroupChat(name, participantIds, avatarUrl) }
    override suspend fun getParticipantIds(chatId: String) = withContext(AppDispatchers.IO) { groupRepository.getParticipantIds(chatId) }
    override suspend fun getGroupMembers(chatId: String) = withContext(AppDispatchers.IO) { groupRepository.getGroupMembers(chatId) }
    override suspend fun addGroupMembers(chatId: String, userIds: List<String>) = withContext(AppDispatchers.IO) { groupRepository.addGroupMembers(chatId, userIds) }
    override suspend fun removeGroupMember(chatId: String, userId: String) = withContext(AppDispatchers.IO) { groupRepository.removeGroupMember(chatId, userId) }
    override suspend fun promoteToAdmin(chatId: String, userId: String) = withContext(AppDispatchers.IO) { groupRepository.promoteToAdmin(chatId, userId) }
    override suspend fun demoteAdmin(chatId: String, userId: String) = withContext(AppDispatchers.IO) { groupRepository.demoteAdmin(chatId, userId) }
    override suspend fun leaveGroup(chatId: String) = withContext(AppDispatchers.IO) { groupRepository.leaveGroup(chatId) }
    override suspend fun toggleOnlyAdminsCanMessage(chatId: String, enabled: Boolean) = withContext(AppDispatchers.IO) { groupRepository.toggleOnlyAdminsCanMessage(chatId, enabled) }
    override suspend fun getChatInfo(chatId: String) = withContext(AppDispatchers.IO) { groupRepository.getChatInfo(chatId) }

    override suspend fun toggleMessageReaction(messageId: String, emoji: String, chatId: String?): Result<Unit> = withContext(AppDispatchers.IO) {
        reactionDataSource.toggleReaction(messageId, emoji, chatId).onSuccess {
            getCurrentUserId()?.let { userId ->
                val existing = reactionDao?.getByMessageId(messageId)?.find { it.userId == userId && it.reactionEmoji == emoji }
                if (existing != null) reactionDao?.delete(messageId, userId, emoji)
                else reactionDao?.insert(MessageReaction(messageId, userId, emoji, 0L))
            }
        }
    }

    override suspend fun getReactionsForMessage(messageId: String): Result<List<MessageReaction>> = withContext(AppDispatchers.IO) {
        try {
            val cached = reactionDao?.getByMessageId(messageId) ?: emptyList()
            val networkReactions = try {
                reactionDataSource.getReactionsForMessage(messageId).map { dto ->
                    MessageReaction(dto.messageId, dto.userId, dto.reactionEmoji, 0L, dto.isDeleteEvent)
                }.also {
                    reactionDao?.deleteAllByMessageId(messageId)
                    reactionDao?.insertAll(it)
                }
            } catch (e: Exception) { null }
            Result.success(networkReactions ?: cached)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun clearLocalCache(): Unit = withContext(AppDispatchers.IO) {
        cachedConversationDao?.deleteAll()
        cachedMessageDao?.deleteAll()
        reactionDao?.deleteAll()
    }

    override suspend fun getReactionsForMessages(messages: List<Message>): List<Message> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            val messageIds = messages.map { it.id }
            val allReactions = try {
                reactionDataSource.getReactionsForMessages(messageIds).map { dto ->
                    MessageReaction(dto.messageId, dto.userId, dto.reactionEmoji, 0L, dto.isDeleteEvent)
                }.also {
                    reactionDao?.deleteAllByMessageIds(messageIds)
                    reactionDao?.insertAll(it)
                }
            } catch (e: Exception) { reactionDao?.getByMessageIds(messageIds) ?: emptyList() }

            messages.map { message ->
                val messageReactions = allReactions.filter { it.messageId == message.id }
                val userReactionsMap = messageReactions.mapNotNull { reaction ->
                    val type = ReactionType.entries.find { it.emoji == reaction.reactionEmoji }
                    if (type != null) reaction.userId to type else null
                }.toMap()
                message.withUserReactions(userReactionsMap, currentUserId)
            }
        } catch (e: Exception) { messages }
    }

    override fun subscribeToMessageReactions(chatId: String): Flow<MessageReaction> =
        dataSource.subscribeToMessageReactions(chatId).map { dto ->
            val reaction = MessageReaction(dto.messageId, dto.userId, dto.reactionEmoji, 0L, dto.isDeleteEvent)
            externalScope.launch {
                if (reaction.isDelete) reactionDao?.delete(reaction.messageId, reaction.userId, reaction.reactionEmoji)
                else reactionDao?.insert(reaction)
            }
            reaction
        }

    override suspend fun releaseTypingChannel(chatId: String) {
        dataSource.releaseTypingChannel(chatId)
    }

    override suspend fun addSystemMessage(chatId: String, content: String): Result<Message> = withContext(AppDispatchers.IO) {
        try {
            val dto = dataSource.insertSystemMessage(chatId, content)
            val domainMessage = dto.toDomain()
            cachedMessageDao?.upsert(domainMessage)
            Result.success(domainMessage)
        } catch (e: Exception) {
            Logger.e("Error adding system message", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun deleteExpiredMessages(chatId: String): Result<Int> = withContext(AppDispatchers.IO) {
        try {
            val deletedCount = dataSource.deleteExpiredMessages(chatId)
            // Also delete from local cache
            val now = com.synapse.social.studioasinc.shared.util.TimeProvider.nowInstant().toString()
            cachedMessageDao?.deleteExpired(chatId, now)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Logger.e("Error deleting expired messages", throwable = e)
            Result.failure(e)
        }
    }
}
