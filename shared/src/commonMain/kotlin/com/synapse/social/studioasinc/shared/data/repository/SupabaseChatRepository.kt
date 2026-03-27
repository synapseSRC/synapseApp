package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDomain
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import com.synapse.social.studioasinc.shared.util.Logger
import com.synapse.social.studioasinc.shared.util.UUIDUtils
import com.synapse.social.studioasinc.shared.util.TimeProvider
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.data.local.database.CachedConversationDao

class SupabaseChatRepository(
    private val dataSource: SupabaseChatDataSource = SupabaseChatDataSource(),
    private val client: SupabaseClientLib = SupabaseClient.client,
    private val signalProtocolManager: SignalProtocolManager? = null,
    private val mediaUploadRepository: MediaUploadRepository,
    private val presenceRepository: com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository? = null,
    private val offlineActionRepository: OfflineActionRepository? = null,
    private val cachedMessageDao: CachedMessageDao? = null,
    private val cachedConversationDao: CachedConversationDao? = null,
    private val externalScope: CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
) : ChatRepository {

    private val encryptionHelper = ChatEncryptionHelper(signalProtocolManager, dataSource, cachedMessageDao)
    private val groupRepository = ChatGroupRepository(dataSource)

    override suspend fun ensureSession(userId: String) = encryptionHelper.ensureSession(userId)

    private suspend fun fetchConversationsFromNetwork(): List<Conversation> {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        val conversationData = dataSource.getConversations()
        
        return kotlinx.coroutines.coroutineScope {
            conversationData.map { (participation, user, chatInfo) ->
                async {
                    var lastMessageText = "No messages yet"
                    val lastMessage = dataSource.getLastMessage(participation.chatId)
                    
                    if (lastMessage != null) {
                        try {
                            val decryptedDto = with(encryptionHelper) { lastMessage.decryptIfNecessary(currentUserId) }
                            lastMessageText = if ((decryptedDto.isEncrypted == true) && decryptedDto.content == "Message is encrypted") {
                                "🔒 Encrypted message"
                            } else {
                                decryptedDto.content
                            }
                        } catch (e: Exception) {
                            lastMessageText = "🔒 Encrypted message"
                        }
                    }
                    
                    val unreadCount = dataSource.getUnreadCount(participation.chatId, participation.lastReadAt)
                    
                    val isGroup = chatInfo?.isGroup ?: false
                    val groupName = chatInfo?.name
                    val groupAvatar = chatInfo?.avatarUrl

                    Conversation(
                        chatId = participation.chatId,
                        participantId = user?.uid ?: participation.userId,
                        participantName = if (isGroup) groupName ?: "Group Chat" else (user?.displayName ?: user?.username ?: "Unknown"),
                        participantAvatar = if (isGroup) groupAvatar else user?.avatar,
                        lastMessage = lastMessageText,
                        lastMessageTime = lastMessage?.createdAt,
                        unreadCount = unreadCount,
                        isOnline = if (isGroup) false else (user?.status?.name == "ONLINE"),
                        isGroup = isGroup
                    )
                }
            }.map { it.await() }
        }.sortedByDescending { it.lastMessageTime ?: "" }
    }

    override suspend fun getConversations(): Result<List<Conversation>> = try {
        // Cache-first
        val cached = cachedConversationDao?.getAll() ?: emptyList()
        if (cached.isNotEmpty()) {
            externalScope.launch {
                try {
                    val fresh = fetchConversationsFromNetwork()
                    cachedConversationDao?.upsertAll(fresh)
                } catch (e: Exception) {
                    Logger.e("Failed to sync fresh conversations or messages", throwable = e)
                }
            }
            Result.success(cached)
        } else {
            // No cache: fetch, store, return
            val conversations = fetchConversationsFromNetwork()
            cachedConversationDao?.upsertAll(conversations)
            Result.success(conversations)
        }
    } catch (e: Exception) {
        val cached = cachedConversationDao?.getAll() ?: emptyList()
        if (cached.isNotEmpty()) {
            Result.success(cached)
        } else {
            Logger.e("Error getting conversations", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun getMessages(chatId: String, limit: Int, before: String?): Result<List<Message>> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        
        val cached = if (before == null) cachedMessageDao?.getMessages(chatId, limit) ?: emptyList() else emptyList()
        if (before == null && cached.isNotEmpty()) {
            externalScope.launch {
                try {
                    val fresh = dataSource.getMessages(chatId, limit, null)
                    val decrypted = fresh.map { with(encryptionHelper) { it.decryptIfNecessary(currentUserId).toDomain() } }
                    // Re-read the latest cache inside the background task to avoid stale references
                    val latestCached = cachedMessageDao?.getMessages(chatId, limit * 2) ?: emptyList()
                    val placeholders = setOf(
                        "Message is encrypted", 
                        "🔒 Encrypted message", 
                        "🔒 You sent an encrypted message",
                        "🔒 You sent an encrypted message (Copy)"
                    )
                    // Only upsert messages that were successfully decrypted;
                    // preserve existing cache for messages that failed re-decryption
                    val mergedMessages = decrypted.map { freshMsg ->
                        if (freshMsg.isEncrypted && freshMsg.content in placeholders) {
                            // Decryption failed — keep the cached version if we have one with real content
                            latestCached.find { it.id == freshMsg.id && it.content !in placeholders } ?: freshMsg
                        } else {
                            freshMsg
                        }
                    }
                    cachedMessageDao?.upsertAll(mergedMessages)
                    cachedMessageDao?.trimToLimit(chatId, limit)
                } catch (e: Exception) {
                    Logger.e("Failed to sync fresh conversations or messages", throwable = e)
                }
            }
            Result.success(cached)
        } else {
            // No cache or paginating: fetch from network
            val messageDtos = dataSource.getMessages(chatId, limit, before)
            val decrypted = messageDtos.map { with(encryptionHelper) { it.decryptIfNecessary(currentUserId).toDomain() } }

            if (before == null) {
                cachedMessageDao?.upsertAll(decrypted)
                cachedMessageDao?.trimToLimit(chatId, limit)
            }
            Result.success(decrypted)
        }
    } catch (e: Exception) {
        // Network failed - return cache as fallback
        val cached = cachedMessageDao?.getMessages(chatId, limit) ?: emptyList()
        if (cached.isNotEmpty()) {
            Result.success(cached)
        } else {
            Logger.e("Error getting messages: ${e.message}", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(
        chatId: String, 
        content: String,
        mediaUrl: String?, 
        messageType: String,
        isEncrypted: Boolean,
        encryptedContent: String?,
        expiresAt: String?,
        replyToId: String?
    ): Result<Message> = try {
        // We could do optimistic update here by inserting into a local DAO if we had one for messages
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        
        val failureReason: String? = null
        val contentForDataSource = if (isEncrypted) "Message is encrypted" else content

        val message = try {
            dataSource.sendMessage(chatId, contentForDataSource, mediaUrl, messageType, isEncrypted, encryptedContent, expiresAt, replyToId)
        } catch (e: Exception) {
            if (offlineActionRepository != null) {
                Logger.w("Failed to send message via network, queuing for background sync", tag = "CHAT", throwable = e)
                val actionId = UUIDUtils.randomUUID()
                offlineActionRepository.addAction(
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
                            put("isEncrypted", isEncrypted)
                            put("encryptedContent", encryptedContent)
                        }.toString()
                    )
                )
                // Construct a temporary Message object for the UI
                MessageDto(
                    id = actionId,
                    chatId = chatId,
                    senderId = currentUserId,
                    content = content,
                    messageType = messageType,
                    mediaUrl = mediaUrl,
                    createdAt = TimeProvider.nowInstant().toString(),
                    isEncrypted = isEncrypted,
                    encryptedContent = encryptedContent
                )
            } else {
                throw e
            }
        }
        
        // Notifications are handled by server-side database triggers for reliability
        
        val decryptedDto = message.copy(
            content = content,
            encryptionFailureReason = if (!isEncrypted) failureReason else null
        )
        val domainMessage = decryptedDto.toDomain()
        cachedMessageDao?.upsert(domainMessage)
        Result.success(domainMessage)
    } catch (e: Exception) {
        Logger.e("Error sending message", throwable = e)
        Result.failure(e)
    }

    override suspend fun getOrCreateChat(otherUserId: String): Result<String> = try {
        val chatId = dataSource.getOrCreateChat(otherUserId) ?: throw Exception("Failed to create chat")
        Result.success(chatId)
    } catch (e: Exception) {
        Logger.e("Error creating chat", throwable = e)
        Result.failure(e)
    }


    override suspend fun updateConversationArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = try {
        dataSource.updateConversationArchiveStatus(chatId, isArchived)
        Result.success(Unit)
    } catch (e: Exception) {
        Logger.e("Error updating conversation archive status", throwable = e)
        Result.failure(e)
    }

    override suspend fun deleteConversation(chatId: String): Result<Unit> = try {
        dataSource.deleteConversation(chatId)
        // Also remove from local cache
        cachedConversationDao?.deleteByChatId(chatId)
        Result.success(Unit)
    } catch (e: Exception) {
        Logger.e("Error deleting conversation", throwable = e)
        Result.failure(e)
    }
    override suspend fun markMessagesAsRead(chatId: String): Result<Unit> = try {
        dataSource.markMessagesAsRead(chatId)
        Result.success(Unit)
    } catch (e: Exception) {
        Logger.e("Error marking messages as read", throwable = e)
        Result.failure(e)
    }

    override suspend fun markMessagesAsDelivered(chatId: String): Result<Unit> = try {
        dataSource.markMessagesAsDelivered(chatId)
        Result.success(Unit)
    } catch (e: Exception) {
        Logger.e("Error marking messages as delivered", throwable = e)
        Result.failure(e)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = try {
        dataSource.deleteMessage(messageId)
        cachedMessageDao?.markDeleted(messageId)
        Result.success(Unit)
    } catch (e: Exception) {
        Logger.e("Error deleting message", throwable = e)
        Result.failure(e)
    }

    override suspend fun deleteMessages(messageIds: List<String>): Result<Unit> = try {
        dataSource.deleteMessages(messageIds)
        cachedMessageDao?.markDeleted(messageIds)
        Result.success(Unit)
    } catch (e: Exception) {
        Logger.e("Error deleting messages", throwable = e)
        Result.failure(e)
    }

    override suspend fun deleteMessageForMe(messageId: String): Result<Unit> = try {
        dataSource.deleteMessageForMe(messageId)
        cachedMessageDao?.markDeleted(messageId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteMessagesForMe(messageIds: List<String>): Result<Unit> = try {
        dataSource.deleteMessagesForMe(messageIds)
        cachedMessageDao?.markDeleted(messageIds)
        Result.success(Unit)
    } catch (e: Exception) {
        Logger.e("Error deleting messages for me", throwable = e)
        Result.failure(e)
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        val originalMessage = dataSource.getMessageById(messageId) ?: throw Exception("Message not found")
        
        var isEncrypted = false
        var encryptedContent: String? = null
        var finalContent = newContent
        
        if (signalProtocolManager != null && originalMessage.isEncrypted) {
            try {
                val chatId = originalMessage.chatId
                val groupMembers = dataSource.getGroupMembers(chatId)
                val otherParticipants = groupMembers.map { it.first.uid }.filter { it != currentUserId }

                if (otherParticipants.isNotEmpty()) {
                    val payloadMap = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
                    val contentBytes = newContent.encodeToByteArray()

                    for (otherUserId in otherParticipants) {
                        ensureSession(otherUserId)
                        val encryptedForReceiver = signalProtocolManager.encryptMessage(otherUserId, contentBytes)
                        payloadMap[otherUserId] = Json.encodeToJsonElement(EncryptedMessage.serializer(), encryptedForReceiver)
                    }

                    // Store sender's own plaintext copy (NOT Signal-encrypted)
                    val jsonPayload = kotlinx.serialization.json.buildJsonObject {
                        put("content", newContent)
                    }.toString()
                    payloadMap[currentUserId!!] = JsonPrimitive(jsonPayload)

                    val payloads = JsonObject(payloadMap)

                    isEncrypted = true
                    encryptedContent = payloads.toString()
                    finalContent = "Message is encrypted"
                }
            } catch (e: Exception) {
                Logger.e("E2EE encryption failed for edit, sending unencrypted", throwable = e)
            }
        }

        dataSource.editMessage(messageId, finalContent, isEncrypted, encryptedContent)
        cachedMessageDao?.updateContent(messageId, newContent)
        Result.success(Unit)
    } catch (e: Exception) {
        Logger.e("Error editing message", throwable = e)
        Result.failure(e)
    }



    override suspend fun uploadMedia(chatId: String, filePath: String, fileName: String, contentType: String, provider: StorageProvider?, config: StorageConfig?, onProgress: ((Int) -> Unit)?): Result<String> = try {
        // Use the chat attachments bucket. The fileName is assumed to already contain the necessary prefix (e.g., chat_media/chatId/fileName).
        val bucketName = "chat_attachments"
        mediaUploadRepository.upload(
            filePath = filePath,
            provider = provider ?: StorageProvider.SUPABASE,
            config = config ?: StorageConfig(),
            bucketName = bucketName,
            onProgress = { floatProgress -> onProgress?.invoke((floatProgress * 100).toInt()) }
        ).onSuccess { url ->
            return Result.success(url)
        }.onFailure { e ->
            Logger.e("Error uploading media", throwable = e)
            return Result.failure(e)
        }
        Result.failure(Exception("Unknown error"))
    } catch (e: Exception) {
        Logger.e("Error uploading media", throwable = e)
        Result.failure(e)
    }

    override suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> = try {
        dataSource.broadcastTypingStatus(chatId, isTyping)
        Result.success(Unit)
    } catch (e: Exception) {
        Logger.e("Error broadcasting typing status", throwable = e)
        Result.failure(e)
    }

    override fun subscribeToMessages(chatId: String): Flow<Message> =
        dataSource.subscribeToMessages(chatId).map { 
            val userId = getCurrentUserId() ?: ""
            if (userId.isNotBlank()) with(encryptionHelper) { it.decryptIfNecessary(userId).toDomain() } else it.toDomain()
        }

    override fun subscribeToInboxUpdates(chatIds: List<String>): Flow<Message> =
        dataSource.subscribeToInboxUpdates(chatIds).map { 
            val userId = getCurrentUserId() ?: ""
            if (userId.isNotBlank()) with(encryptionHelper) { it.decryptIfNecessary(userId).toDomain() } else it.toDomain()
        }

    override fun subscribeToTypingStatus(chatId: String): Flow<TypingStatus> =
        dataSource.subscribeToTypingStatus(chatId).map { data ->
            TypingStatus(
                userId = data["user_id"] as? String ?: "",
                chatId = chatId,
                isTyping = data["is_typing"] as? Boolean ?: false
            )
        }

    override fun subscribeToReadReceipts(chatId: String): Flow<Message> =
        dataSource.subscribeToReadReceipts(chatId).map { 
            val userId = getCurrentUserId() ?: ""
            if (userId.isNotBlank()) with(encryptionHelper) { it.decryptIfNecessary(userId).toDomain() } else it.toDomain()
        }

    override suspend fun initializeE2EE(): Result<Unit> = encryptionHelper.initializeE2EE(getCurrentUserId())

    override fun getCurrentUserId(): String? = dataSource.getCurrentUserId()

    override suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String?) = groupRepository.createGroupChat(name, participantIds, avatarUrl)
    override suspend fun getParticipantIds(chatId: String) = groupRepository.getParticipantIds(chatId)
    override suspend fun getGroupMembers(chatId: String) = groupRepository.getGroupMembers(chatId)
    override suspend fun addGroupMembers(chatId: String, userIds: List<String>) = groupRepository.addGroupMembers(chatId, userIds)
    override suspend fun removeGroupMember(chatId: String, userId: String) = groupRepository.removeGroupMember(chatId, userId)
    override suspend fun promoteToAdmin(chatId: String, userId: String) = groupRepository.promoteToAdmin(chatId, userId)
    override suspend fun demoteAdmin(chatId: String, userId: String) = groupRepository.demoteAdmin(chatId, userId)
    override suspend fun leaveGroup(chatId: String) = groupRepository.leaveGroup(chatId)
    override suspend fun toggleOnlyAdminsCanMessage(chatId: String, enabled: Boolean) = groupRepository.toggleOnlyAdminsCanMessage(chatId, enabled)
    override suspend fun getChatInfo(chatId: String) = groupRepository.getChatInfo(chatId)
}
