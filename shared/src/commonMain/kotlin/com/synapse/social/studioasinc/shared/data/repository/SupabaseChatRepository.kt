package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDomain
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
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

class SupabaseChatRepository(
    private val dataSource: SupabaseChatDataSource = SupabaseChatDataSource(),
    private val client: SupabaseClientLib = SupabaseClient.client,
    private val signalProtocolManager: SignalProtocolManager? = null,
    private val mediaUploadRepository: MediaUploadRepository,
    private val presenceRepository: com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository? = null
) : ChatRepository {

    private suspend fun MessageDto.decryptIfNecessary(currentUserId: String): MessageDto {
        if (this.isEncrypted && this.encryptedContent != null && signalProtocolManager != null) {
            try {
                Napier.d("E2EE_DECRYPT: Attempting to decrypt message ${this.id}", tag = "E2EE")
                val jsonElement = Json.parseToJsonElement(this.encryptedContent).jsonObject
                val myPayloadElement = jsonElement[currentUserId]

                if (myPayloadElement != null) {
                    val senderId = this.senderId

                    // If the sender is the current user, the payload is stored as plain text (base64)
                    if (senderId == currentUserId) {
                        // Sender's copy: stored as a JSON string with the plain text
                        val plainText = try {
                            myPayloadElement.jsonPrimitive.content
                        } catch (_: Exception) {
                            null
                        }
                        if (plainText != null) {
                            Napier.d("E2EE_DECRYPT: Retrieved sender's plaintext copy for message ${this.id}", tag = "E2EE")
                            return this.copy(content = plainText)
                        }
                        // Fallback: try decoding as EncryptedMessage (legacy format)
                        Napier.d("E2EE_DECRYPT: Sender copy not plaintext, trying legacy decrypt", tag = "E2EE")
                    }

                    // Recipient's copy: decrypt using Signal Protocol
                    val myPayload = Json.decodeFromJsonElement(EncryptedMessage.serializer(), myPayloadElement)
                    try {
                        val decryptedBytes = signalProtocolManager.decryptMessage(
                            senderId = senderId,
                            message = myPayload
                        )
                        Napier.d("E2EE_DECRYPT: Successfully decrypted message ${this.id}", tag = "E2EE")
                        return this.copy(content = decryptedBytes.decodeToString())
                    } catch (decryptError: Exception) {
                        Napier.e("E2EE_DECRYPT: Decryption failed, attempting session recovery", decryptError, tag = "E2EE")
                        // Try to recover by deleting corrupted session and re-establishing
                        try {
                            signalProtocolManager.deleteSession(senderId)
                            Napier.d("E2EE_DECRYPT: Deleted corrupted session for $senderId", tag = "E2EE")
                            
                            // Re-establish session
                            ensureSession(senderId)
                            
                            // Retry decryption
                            val decryptedBytes = signalProtocolManager.decryptMessage(
                                senderId = senderId,
                                message = myPayload
                            )
                            Napier.d("E2EE_DECRYPT: Successfully decrypted after session recovery", tag = "E2EE")
                            return this.copy(content = decryptedBytes.decodeToString())
                        } catch (recoveryError: Exception) {
                            Napier.e("E2EE_DECRYPT: Session recovery failed: ${recoveryError.message}", recoveryError, tag = "E2EE")
                        }
                    }
                } else {
                    Napier.w("E2EE_DECRYPT: No payload found for current user in message ${this.id}", tag = "E2EE")
                }
            } catch (e: Exception) {
                Napier.e("E2EE_DECRYPT: Decryption failed for message ${this.id}: ${e.message}", e, tag = "E2EE")
            }
        }
        return this
    }

    private suspend fun ensureSession(userId: String) {
        if (signalProtocolManager != null && !signalProtocolManager.hasSession(userId)) {
            Napier.d("E2EE_SESSION: No session exists for user $userId, establishing...", tag = "E2EE")
            val keyDto = dataSource.getUserPublicKey(userId)
            if (keyDto != null) {
                Napier.d("E2EE_SESSION: Found public key for user $userId, processing bundle", tag = "E2EE")
                val bundle = Json.decodeFromString<PreKeyBundle>(keyDto.publicKey)
                signalProtocolManager.processPreKeyBundle(userId, bundle)
                Napier.d("E2EE_SESSION: Session established successfully for user $userId", tag = "E2EE")
            } else {
                Napier.e("E2EE_SESSION: Public key not found for user $userId", tag = "E2EE")
                throw Exception("Public key not found for user $userId")
            }
        } else if (signalProtocolManager != null) {
            Napier.d("E2EE_SESSION: Session already exists for user $userId", tag = "E2EE")
        }
    }

    override suspend fun getConversations(): Result<List<Conversation>> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        val conversationData = dataSource.getConversations()
        
        val conversations = kotlinx.coroutines.coroutineScope {
            conversationData.map { (participation, user, chatInfo) ->
                async {
                    var lastMessageText = "No messages yet"
                    val lastMessage = dataSource.getLastMessage(participation.chatId)
                    
                    if (lastMessage != null) {
                        try {
                            val decryptedDto = lastMessage.decryptIfNecessary(currentUserId)
                            lastMessageText = decryptedDto.content
                        } catch (e: Exception) {
                            lastMessageText = "Encrypted message"
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
        }.sortedByDescending { it.lastMessageTime ?: "" } // Use empty string to sort nulls to bottom
        
        Result.success(conversations)
    } catch (e: Exception) {
        Napier.e("Error getting conversations", e)
        Result.failure(e)
    }

    override suspend fun getMessages(chatId: String, limit: Int, before: String?): Result<List<Message>> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        val messageDtos = dataSource.getMessages(chatId, limit, before)
        
        // Decrypt separately
        val decrypted = messageDtos.map { 
            it.decryptIfNecessary(currentUserId).toDomain() 
        }
        Result.success(decrypted)
    } catch (e: Exception) {
        Napier.e("Error getting messages: ${e.message}", e)
        Result.failure(e)
    }

    override suspend fun sendMessage(
        chatId: String, 
        content: String, 
        mediaUrl: String?, 
        messageType: String,
        expiresAt: String?,
        replyToId: String?
    ): Result<Message> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        
        var isEncrypted = false
        var encryptedContentStr: String? = null
        var finalContent = content
        var failureReason: String? = null
        
        if (signalProtocolManager == null) {
            failureReason = "E2EE not initialized - SignalProtocolManager is null"
            Napier.e("E2EE_SEND: $failureReason. Check DI configuration.", tag = "E2EE")
        } else {
            Napier.d("E2EE_SEND: SignalProtocolManager is available, attempting encryption", tag = "E2EE")
            try {
                val otherUserId = dataSource.getOtherParticipantId(chatId, currentUserId)
                Napier.d("E2EE_ENCRYPT: Other participant ID: $otherUserId", tag = "E2EE")
                if (otherUserId != null) {
                    Napier.d("E2EE_ENCRYPT: Found recipient $otherUserId, checking for public keys", tag = "E2EE")
                    
                    // Check if recipient has keys before attempting session
                    val recipientKeyDto = try {
                        dataSource.getUserPublicKey(otherUserId)
                    } catch (e: Exception) {
                        Napier.e("E2EE_ENCRYPT: Error fetching recipient key for $otherUserId", e, tag = "E2EE")
                        null
                    }

                    if (recipientKeyDto == null) {
                        failureReason = "Recipient ($otherUserId) hasn't enabled E2EE (no public key in DB)"
                        Napier.w("E2EE_ENCRYPT: $failureReason", tag = "E2EE")
                    } else {
                        try {
                            Napier.d("E2EE_ENCRYPT: Establishing/Verifying session with $otherUserId", tag = "E2EE")
                            ensureSession(otherUserId)
                            
                            Napier.d("E2EE_ENCRYPT: Calling SignalProtocolManager.encryptMessage for $otherUserId", tag = "E2EE")
                            val encryptedForReceiver = signalProtocolManager.encryptMessage(otherUserId, content.encodeToByteArray())
                            
                            // Store sender's copy as plain text
                            Napier.d("E2EE_ENCRYPT: Encryption successful, building payload", tag = "E2EE")
                            val recipientJson = Json.encodeToJsonElement(EncryptedMessage.serializer(), encryptedForReceiver)
                            val payloads = JsonObject(mapOf(
                                otherUserId to recipientJson,
                                currentUserId to JsonPrimitive(content)
                            ))
                            
                            isEncrypted = true
                            encryptedContentStr = payloads.toString()
                            finalContent = "Message is encrypted"
                            Napier.d("E2EE_ENCRYPT: Message fully prepared for secure transit", tag = "E2EE")
                        } catch (e: Exception) {
                            failureReason = when {
                                e.message?.contains("Public key not found") == true -> "Recipient hasn't enabled E2EE"
                                else -> e.message ?: "Encryption failed"
                            }
                            Napier.e("E2EE_ENCRYPT: $failureReason for recipient $otherUserId, falling back to unencrypted", e, tag = "E2EE")
                        }
                    }
                } else {
                    failureReason = "Chat $chatId has no other participant to encrypt for"
                    Napier.w("E2EE_ENCRYPT: $failureReason, sending unencrypted", tag = "E2EE")
                }
            } catch (e: Exception) {
                failureReason = when {
                    e.message?.contains("Public key not found") == true -> "Recipient hasn't enabled E2EE"
                    else -> e.message ?: "Encryption failed"
                }
                Napier.e("E2EE_ENCRYPT: $failureReason, sending unencrypted", e, tag = "E2EE")
            }
        }

        val message = dataSource.sendMessage(chatId, finalContent, mediaUrl, messageType, isEncrypted, encryptedContentStr, expiresAt, replyToId)
        
        // Send notification to recipient only if they're not in the chat
        try {
            val recipientId = dataSource.getOtherParticipantId(chatId, currentUserId)
            if (recipientId != null) {
                val isInChat = presenceRepository?.isUserInChat(recipientId, chatId) ?: false
                if (!isInChat) {
                    dataSource.sendMessageNotification(recipientId, currentUserId, if (isEncrypted) "🔒 Encrypted message" else content, chatId)
                }
            }
        } catch (e: Exception) {
            Napier.w("Failed to send notification: ${e.message}", tag = "NOTIFICATION")
        }
        
        val decryptedDto = message.copy(
            content = content,
            encryptionFailureReason = if (!isEncrypted) failureReason else null
        )
        Result.success(decryptedDto.toDomain())
    } catch (e: Exception) {
        Napier.e("Error sending message", e)
        Result.failure(e)
    }

    override suspend fun getOrCreateChat(otherUserId: String): Result<String> = try {
        val chatId = dataSource.getOrCreateChat(otherUserId) ?: throw Exception("Failed to create chat")
        Result.success(chatId)
    } catch (e: Exception) {
        Napier.e("Error creating chat", e)
        Result.failure(e)
    }

    override suspend fun markMessagesAsRead(chatId: String): Result<Unit> = try {
        dataSource.markMessagesAsRead(chatId)
        Result.success(Unit)
    } catch (e: Exception) {
        Napier.e("Error marking messages as read", e)
        Result.failure(e)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = try {
        dataSource.deleteMessage(messageId)
        Result.success(Unit)
    } catch (e: Exception) {
        Napier.e("Error deleting message", e)
        Result.failure(e)
    }

    override suspend fun deleteMessageForMe(messageId: String): Result<Unit> = try {
        dataSource.deleteMessageForMe(messageId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("Not logged in")
        val originalMessage = dataSource.getMessageById(messageId) ?: throw Exception("Message not found")
        
        var isEncrypted = false
        var encryptedContentStr: String? = null
        var finalContent = newContent
        
        if (signalProtocolManager != null && originalMessage.isEncrypted) {
            try {
                val chatId = originalMessage.chatId
                val otherUserId = dataSource.getOtherParticipantId(chatId, currentUserId)
                
                if (otherUserId != null) {
                    ensureSession(otherUserId)
                    val encryptedForReceiver = signalProtocolManager.encryptMessage(otherUserId, newContent.encodeToByteArray())
                    
                    // Sender's copy stored as plain text (no self-session)
                    val recipientJson = Json.encodeToJsonElement(EncryptedMessage.serializer(), encryptedForReceiver)
                    val payloads = JsonObject(mapOf(
                        otherUserId to recipientJson,
                        currentUserId to JsonPrimitive(newContent)
                    ))
                    
                    isEncrypted = true
                    encryptedContentStr = payloads.toString()
                    finalContent = "Message is encrypted"
                }
            } catch (e: Exception) {
                Napier.e("E2EE encryption failed for edit, sending unencrypted", e)
            }
        }

        dataSource.editMessage(messageId, finalContent, isEncrypted, encryptedContentStr)
        Result.success(Unit)
    } catch (e: Exception) {
        Napier.e("Error editing message", e)
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
            Napier.e("Error uploading media", e)
            return Result.failure(e)
        }
        Result.failure(Exception("Unknown error"))
    } catch (e: Exception) {
        Napier.e("Error uploading media", e)
        Result.failure(e)
    }

    override suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> = try {
        dataSource.broadcastTypingStatus(chatId, isTyping)
        Result.success(Unit)
    } catch (e: Exception) {
        Napier.e("Error broadcasting typing status", e)
        Result.failure(e)
    }

    override fun subscribeToMessages(chatId: String): Flow<Message> =
        dataSource.subscribeToMessages(chatId).map { 
            val userId = getCurrentUserId() ?: ""
            it.decryptIfNecessary(userId).toDomain() 
        }

    override fun subscribeToInboxUpdates(chatIds: List<String>): Flow<Message> =
        dataSource.subscribeToInboxUpdates(chatIds).map { 
            val userId = getCurrentUserId() ?: ""
            it.decryptIfNecessary(userId).toDomain() 
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
            it.decryptIfNecessary(userId).toDomain() 
        }

    override suspend fun initializeE2EE(): Result<Unit> {
        return try {
            if (signalProtocolManager == null) {
                val error = "SignalProtocolManager is null, E2EE not available"
                Napier.e("E2EE_INIT: $error", tag = "E2EE")
                return Result.failure(Exception(error))
            }

            val currentUserId = getCurrentUserId()
            if (currentUserId == null) {
                val error = "User not authenticated, cannot initialize E2EE"
                Napier.e("E2EE_INIT: $error", tag = "E2EE")
                return Result.failure(Exception(error))
            }

            Napier.d("E2EE_INIT: Starting E2EE initialization for user $currentUserId", tag = "E2EE")
            
            val hasLocalKeys = signalProtocolManager.hasIdentity()
            val remoteKeyDto = try { dataSource.getUserPublicKey(currentUserId) } catch (_: Exception) { null }
            val hasRemoteKeys = remoteKeyDto != null
            
            Napier.d("E2EE_INIT: hasLocalKeys=$hasLocalKeys, hasRemoteKeys=$hasRemoteKeys", tag = "E2EE")

            when {
                hasLocalKeys && hasRemoteKeys -> {
                    // Both exist — fully initialized
                    val regId = signalProtocolManager.getLocalRegistrationId()
                    Napier.d("E2EE_INIT: E2EE already fully initialized (regId: $regId)", tag = "E2EE")
                    
                    if (signalProtocolManager.checkKeyRotationNeeded()) {
                        Napier.w("E2EE_INIT: Key rotation recommended (keys older than 30 days)", tag = "E2EE")
                    }
                }
                hasLocalKeys && !hasRemoteKeys -> {
                    // Local keys exist but remote missing — re-upload the bundle
                    Napier.w("E2EE_INIT: Local keys exist but remote keys MISSING. Re-uploading...", tag = "E2EE")
                    val bundle = buildBundleFromLocalKeys()
                    val bundleStr = Json.encodeToString(bundle)
                    dataSource.uploadUserPublicKey(bundleStr)
                    Napier.d("E2EE_INIT: Successfully re-uploaded public key bundle", tag = "E2EE")
                }
                else -> {
                    // No local keys (or both missing) — full generation
                    if (!hasLocalKeys && hasRemoteKeys) {
                        Napier.w("E2EE_INIT: Remote keys exist but local keys MISSING. Regenerating everything...", tag = "E2EE")
                    } else {
                        Napier.d("E2EE_INIT: Fresh E2EE setup — no local or remote keys", tag = "E2EE")
                    }
                    
                    val identity = signalProtocolManager.generateIdentityAndKeys()
                    Napier.d("E2EE_INIT: Generated identity with regId: ${identity.registrationId}", tag = "E2EE")
                    
                    val preKeys = signalProtocolManager.generateOneTimePreKeys(startId = 1, count = 100)
                    Napier.d("E2EE_INIT: Generated ${preKeys.size} one-time pre-keys", tag = "E2EE")
                    
                    val bundle = PreKeyBundle(
                        registrationId = identity.registrationId,
                        deviceId = 1,
                        preKeyId = preKeys.firstOrNull()?.keyId,
                        preKeyPublic = preKeys.firstOrNull()?.publicKey,
                        signedPreKeyId = identity.signedPreKeyId,
                        signedPreKeyPublic = identity.signedPreKey,
                        signedPreKeySignature = identity.signedPreKeySignature,
                        identityKey = identity.identityKey
                    )
                    
                    val bundleStr = Json.encodeToString(bundle)
                    Napier.d("E2EE_INIT: Uploading public key bundle to server", tag = "E2EE")
                    dataSource.uploadUserPublicKey(bundleStr)
                    Napier.d("E2EE_INIT: Successfully uploaded public key bundle", tag = "E2EE")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("E2EE_INIT: Initialization failed: ${e.message}", e, tag = "E2EE")
            Result.failure(e)
        }
    }
    
    /**
     * Builds a PreKeyBundle from the existing local keys.
     * Used when local keys exist but remote upload was missed.
     */
    private suspend fun buildBundleFromLocalKeys(): PreKeyBundle {
        val regId = signalProtocolManager!!.getLocalRegistrationId()
        val identityKey = signalProtocolManager.getLocalIdentityKey()
        
        // Generate fresh pre-keys to attach to the bundle
        val preKeys = signalProtocolManager.generateOneTimePreKeys(startId = 1, count = 100)
        
        // We need a signed pre-key — regenerate keys to also get the signed pre-key info
        // Since the identity already exists, generateIdentityAndKeys() will re-create from the same pair
        val identity = signalProtocolManager.generateIdentityAndKeys()
        
        return PreKeyBundle(
            registrationId = regId,
            deviceId = 1,
            preKeyId = preKeys.firstOrNull()?.keyId,
            preKeyPublic = preKeys.firstOrNull()?.publicKey,
            signedPreKeyId = identity.signedPreKeyId,
            signedPreKeyPublic = identity.signedPreKey,
            signedPreKeySignature = identity.signedPreKeySignature,
            identityKey = identityKey
        )
    }

    override fun getCurrentUserId(): String? = dataSource.getCurrentUserId()
    override suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String?): Result<String> = try {
        val chatId = dataSource.createGroupChat(name, participantIds, avatarUrl) ?: throw Exception("Failed to create group chat")
        Result.success(chatId)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error creating group chat", e)
        Result.failure(e)
    }

    override suspend fun getGroupMembers(chatId: String): Result<List<Pair<com.synapse.social.studioasinc.shared.domain.model.User, Boolean>>> = try {
        val members = dataSource.getGroupMembers(chatId)
        Result.success(members)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error getting group members", e)
        Result.failure(e)
    }

    override suspend fun addGroupMembers(chatId: String, userIds: List<String>): Result<Unit> = try {
        dataSource.addGroupMembers(chatId, userIds)
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error adding group member", e)
        Result.failure(e)
    }

    override suspend fun removeGroupMember(chatId: String, userId: String): Result<Unit> = try {
        dataSource.removeGroupMember(chatId, userId)
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error removing group member", e)
        Result.failure(e)
    }

}