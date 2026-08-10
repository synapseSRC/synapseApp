package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.datasource.ChatReactionDataSource
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.local.database.CachedConversationDao
import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.data.local.database.MessageReactionDao
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDomain
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import com.synapse.social.studioasinc.shared.util.Logger
import com.synapse.social.studioasinc.shared.util.TimeProvider
import com.synapse.social.studioasinc.shared.util.UUIDUtils
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
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

    override suspend fun ensureSession(userId: String) = withContext(AppDispatchers.IO) {
        encryptionHelper.ensureSession(userId)
    }

    private suspend fun fetchConversationsFromNetwork(): List<Conversation> = withContext(AppDispatchers.IO) {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        val conversationData = dataSource.getConversations()
        
        coroutineScope {
            conversationData.map { (participation, user, chatInfo) ->
                async {
                    val lastMessage = dataSource.getLastMessage(participation.chatId)
                    val lastMessageText = lastMessage?.let {
                        try {
                            encryptionHelper.run { it.decryptIfNecessary(currentUserId).content }
                        } catch (e: Exception) {
                            "🔒 Encrypted message"
                        }
                    } ?: "No messages yet"
                    
                    val unreadCount = dataSource.getUnreadCount(participation.chatId, participation.lastReadAt)
                    
                    val isGroup = chatInfo?.isGroup ?: false

                    Conversation(
                        chatId = participation.chatId,
                        participantId = user?.uid ?: participation.userId,
                        participantName = if (isGroup) chatInfo?.name ?: "Group Chat" else (user?.displayName ?: user?.username ?: "Unknown"),
                        participantAvatar = if (isGroup) chatInfo?.avatarUrl else user?.avatar,
                        lastMessage = lastMessageText,
                        lastMessageTime = lastMessage?.createdAt,
                        unreadCount = unreadCount,
                        isOnline = if (isGroup) false else (user?.status?.name == "ONLINE"),
                        isGroup = isGroup
                    )
                }
            }.awaitAll()
        }.sortedByDescending { it.lastMessageTime ?: "" }
    }

    override suspend fun getConversations(): Result<List<Conversation>> = withContext(AppDispatchers.IO) {
        try {
            val cached = cachedConversationDao?.getAll() ?: emptyList()
            if (cached.isNotEmpty()) {
                externalScope.launch {
                    try {
                        val fresh = fetchConversationsFromNetwork()
                        cachedConversationDao?.upsertAll(fresh)
                    } catch (e: Exception) {
                        Logger.e("Failed to sync fresh conversations", throwable = e)
                    }
                }
                Result.success(cached)
            } else {
                val conversations = fetchConversationsFromNetwork()
                cachedConversationDao?.upsertAll(conversations)
                Result.success(conversations)
            }
        } catch (e: Exception) {
            val cached = cachedConversationDao?.getAll() ?: emptyList()
            if (cached.isNotEmpty()) Result.success(cached)
            else {
                Logger.e("Error getting conversations", throwable = e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getMessages(chatId: String, limit: Int, before: String?, beforeId: String?, forceNetwork: Boolean): Result<List<Message>> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")

            if (before == null && !forceNetwork) {
                val cached = cachedMessageDao?.getMessages(chatId, limit) ?: emptyList()
                if (cached.isNotEmpty()) {
                    syncMessagesInBackground(chatId, limit, currentUserId, cached)
                    return@withContext Result.success(cached)
                }
            }

            val messageDtos = dataSource.getMessages(chatId, limit, before, beforeId)
            val decrypted = messageDtos.map { encryptionHelper.run { it.decryptIfNecessary(currentUserId).toDomain() } }

            if (before == null) {
                cachedMessageDao?.upsertAll(decrypted)
                cachedMessageDao?.trimToLimit(chatId, limit)
            }
            Result.success(decrypted)
        } catch (e: Exception) {
            val cached = cachedMessageDao?.getMessages(chatId, limit) ?: emptyList()
            if (cached.isNotEmpty()) Result.success(cached)
            else {
                Logger.e("Error getting messages", throwable = e)
                Result.failure(e)
            }
        }
    }

    private fun syncMessagesInBackground(chatId: String, limit: Int, currentUserId: String, cached: List<Message>) {
        externalScope.launch {
            try {
                val fresh = dataSource.getMessages(chatId, limit, null, null)
                val decrypted = fresh.map { encryptionHelper.run { it.decryptIfNecessary(currentUserId).toDomain() } }

                val mergedMessages = decrypted.map { freshMsg ->
                    if (freshMsg.content.startsWith("{")) {
                        cached.find { it.id == freshMsg.id && !it.content.startsWith("{") } ?: freshMsg
                    } else freshMsg
                }
                cachedMessageDao?.upsertAll(mergedMessages)
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
        senderPlaintext: String?
    ): Result<Message> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")

            val messageDto = try {
                dataSource.sendMessage(chatId, content, mediaUrl, messageType, expiresAt, replyToId)
            } catch (e: Exception) {
                if (offlineActionRepository != null) {
                    queueMessageOffline(chatId, content, mediaUrl, messageType, expiresAt, replyToId, currentUserId)
                } else throw e
            }

            val domainMessage = messageDto.toDomain()
            val finalMessage = if (senderPlaintext != null) {
                val displayContent = try {
                    Json.parseToJsonElement(senderPlaintext).jsonObject["content"]?.jsonPrimitive?.content ?: senderPlaintext
                } catch (_: Exception) { senderPlaintext }

                val cachedMsg = domainMessage.copy(content = displayContent)
                cachedMessageDao?.upsert(cachedMsg)
                encryptionHelper.decryptedMessageCache[messageDto.id ?: ""] = senderPlaintext
                cachedMsg
            } else {
                cachedMessageDao?.upsert(domainMessage)
                domainMessage
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
        currentUserId: String
    ): MessageDto {
        val actionId = UUIDUtils.randomUUID()
        offlineActionRepository?.addAction(
            PendingAction(
                id = actionId,
                actionType = PendingAction.ActionType.SEND_MESSAGE,
                targetId = chatId,
                payload = buildJsonObject {
                    put("content", content)
                    put("mediaUrl", mediaUrl)
                    put("messageType", messageType)
                    put("expiresAt", expiresAt)
                    put("replyToId", replyToId)
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
            createdAt = TimeProvider.nowInstant().toString()
        )
    }

    override suspend fun getOrCreateChat(otherUserId: String): Result<String> = withContext(AppDispatchers.IO) {
        try {
            val chatId = dataSource.getOrCreateChat(otherUserId) ?: throw Exception("Failed to create chat")
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

    override suspend fun markMessagesAsRead(chatId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.markMessagesAsRead(chatId)
            val currentUserId = getCurrentUserId()
            if (currentUserId != null) {
                cachedMessageDao?.markRead(chatId, currentUserId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error marking messages as read", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun markMessagesAsDelivered(chatId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.markMessagesAsDelivered(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
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

    override suspend fun deleteMessageForMe(messageId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.deleteMessageForMe(messageId)
            cachedMessageDao?.markDeleted(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessagesForMe(messageIds: List<String>): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.deleteMessagesForMe(messageIds)
            cachedMessageDao?.markDeleted(messageIds)
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
            provider = provider ?: StorageProvider.SUPABASE,
            config = config ?: StorageConfig(),
            bucketName = "chat_attachments",
            onProgress = { floatProgress -> onProgress?.invoke((floatProgress * 100).toInt()) }
        ).onFailure { Logger.e("Error uploading media", throwable = it) }
    }

    override suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            dataSource.broadcastTypingStatus(chatId, isTyping)
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error broadcasting typing status", throwable = e)
            Result.failure(e)
        }
    }

    override fun subscribeToMessages(chatId: String): Flow<Message> =
        dataSource.subscribeToMessages(chatId).map { dto ->
            val userId = getCurrentUserId() ?: ""
            val domainMessage = if (userId.isNotBlank()) encryptionHelper.run { dto.decryptIfNecessary(userId).toDomain() } else dto.toDomain()

            externalScope.launch {
                cachedMessageDao?.upsert(domainMessage)
                conversationMutex.withLock {
                    cachedConversationDao?.getAll()?.find { it.chatId == domainMessage.chatId }?.let { existing ->
                        cachedConversationDao?.upsertAll(listOf(existing.copy(lastMessage = domainMessage.content, lastMessageTime = domainMessage.createdAt)))
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
                cachedMessageDao?.upsert(domainMessage)
                val existing = cachedConversationDao?.getAll()?.find { it.chatId == domainMessage.chatId }
                val chatInfo = if (existing == null) dataSource.getChatInfo(domainMessage.chatId) else null

                conversationMutex.withLock {
                    val currentExisting = cachedConversationDao?.getAll()?.find { it.chatId == domainMessage.chatId }
                    val updated = if (currentExisting != null) {
                        currentExisting.copy(
                            lastMessage = domainMessage.content,
                            lastMessageTime = domainMessage.createdAt,
                            unreadCount = if (domainMessage.senderId == userId) currentExisting.unreadCount else currentExisting.unreadCount + 1
                        )
                    } else {
                        val isGroup = chatInfo?.isGroup ?: false
                        Conversation(
                            chatId = domainMessage.chatId,
                            participantId = if (isGroup) domainMessage.chatId else domainMessage.senderId,
                            participantName = if (isGroup) chatInfo?.name ?: "Group Chat" else "Unknown User",
                            participantAvatar = if (isGroup) chatInfo?.avatarUrl else null,
                            lastMessage = domainMessage.content,
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
        dataSource.subscribeToReadReceipts(chatId).map { 
            val userId = getCurrentUserId() ?: ""
            if (userId.isNotBlank()) encryptionHelper.run { it.decryptIfNecessary(userId).toDomain() } else it.toDomain()
        }

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
                val summary = messageReactions.groupBy { it.reactionEmoji }
                    .mapKeys { entry -> ReactionType.entries.find { it.emoji == entry.key } ?: ReactionType.LIKE }
                    .mapValues { it.value.size }
                val userReaction = messageReactions.find { it.userId == currentUserId }?.reactionEmoji?.let { emoji ->
                    ReactionType.entries.find { it.emoji == emoji }
                }
                message.copy(reactions = summary, userReaction = userReaction)
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
}
