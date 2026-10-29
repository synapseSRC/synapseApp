package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageMetadataContainer
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * UseCase responsible for sending messages with End-to-End Encryption (E2EE).
 *
 * It orchestrates the encryption process using [SignalProtocolManager], ensuring that
 * messages are separately encrypted for each participant in a conversation before
 * being persisted and transmitted via the [ChatRepository].
 */
class SendMessageUseCase(
    private val repository: ChatRepository,
    private val signalProtocolManager: SignalProtocolManager? = null
) {
    /** Ensures a voice asset can be delivered through Signal E2EE before it is uploaded. */
    suspend fun ensureE2EEReadyForChat(chatId: String): Result<Unit> {
        val manager = signalProtocolManager
            ?: return Result.failure(IllegalStateException("End-to-end encryption is unavailable."))

        return try {
            if (!manager.hasIdentity()) {
                throw IllegalStateException("End-to-end encryption is not initialized on this device.")
            }
            val currentUserId = repository.getCurrentUserId()
                ?: throw IllegalStateException("Not logged in")
            val groupMembers = repository.getParticipantIds(chatId).getOrElse { error ->
                throw IllegalStateException("Failed to fetch chat participants for encryption", error)
            }
            val otherParticipants = groupMembers.filter { it != currentUserId }
            val recipients = if (otherParticipants.isEmpty() && groupMembers.isNotEmpty()) {
                groupMembers
            } else {
                otherParticipants
            }
            if (recipients.isEmpty()) {
                throw IllegalStateException("This chat has no recipients available for encryption.")
            }
            for (recipientId in recipients) {
                repository.ensureSession(recipientId)
                if (!manager.hasSession(recipientId)) {
                    throw IllegalStateException("Could not establish an encrypted session with a chat participant.")
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Unable to prepare end-to-end encryption: " + e.message, e))
        }
    }

    /**
     * Executes the message sending flow.
     *
     * @param chatId The unique identifier of the conversation.
     * @param content The plaintext content of the message.
     * @param mediaUrl Optional URL to associated media (e.g., image, video).
     * @param messageType The category of the message (defaults to "text").
     * @param expiresAt Optional timestamp for disappearing messages.
     * @param replyToId Optional ID of the message being replied to.
     * @param metadataContainer Optional structured metadata for rich message types.
     * @return A [Result] containing the sent [Message] on success, or an exception on failure.
     */
    suspend operator fun invoke(
        chatId: String,
        content: String,
        mediaUrl: String? = null,
        messageType: String = "text",
        expiresAt: String? = null,
        replyToId: String? = null,
        attachments: List<com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment>? = null,
        metadataContainer: MessageMetadataContainer? = null,
        requireE2EE: Boolean = false
    ): Result<Message> {
        val currentUserId = repository.getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        val serializedMetadata = metadataContainer?.let {
            try {
                Json.encodeToString(MessageMetadataContainer.serializer(), it)
            } catch (e: Exception) {
                null
            }
        }

        if (signalProtocolManager == null) {
            if (requireE2EE) {
                return Result.failure(IllegalStateException("End-to-end encryption is required; voice message was not sent."))
            }
            Napier.w("E2EE_ENCRYPT: SignalProtocolManager is null — sending plain text message", tag = "E2EE")
            return repository.sendMessage(
                chatId = chatId,
                content = content,
                mediaUrl = mediaUrl,
                messageType = messageType,
                expiresAt = expiresAt,
                replyToId = replyToId,
                senderPlaintext = content,
                attachments = attachments,
                metadataContainer = metadataContainer
            )
        }

        return try {
            val groupMembers = repository.getParticipantIds(chatId).getOrElse {
                return Result.failure(Exception("Failed to fetch participants for encryption"))
            }

            // Determine recipients: exclude self unless it's a "saved messages" style self-chat
            var otherParticipants = groupMembers.filter { it != currentUserId }
            if (otherParticipants.isEmpty() && groupMembers.isNotEmpty()) {
                otherParticipants = groupMembers // chatting with self
            }
            if (otherParticipants.isEmpty()) {
                return Result.failure(Exception("Chat $chatId has no other participants to encrypt for"))
            }

            val jsonPayload = kotlinx.serialization.json.buildJsonObject {
                put("content", content)
                if (mediaUrl != null) put("mediaUrl", mediaUrl)
                if (serializedMetadata != null) put("metadata", serializedMetadata)
                if (attachments != null) {
                    val attachmentsJson = kotlinx.serialization.json.Json.encodeToJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(com.synapse.social.studioasinc.shared.data.dto.chat.MessageAttachmentDto.serializer()),
                        attachments.map { with(com.synapse.social.studioasinc.shared.data.mapper.ChatMapper) { it.toDto() } }
                    )
                    put("attachments", attachmentsJson)
                }
            }.toString()
            val contentBytes = jsonPayload.encodeToByteArray()

            val payloadMap: Map<String, JsonElement> = coroutineScope {
                otherParticipants.map { userId ->
                    async {
                        try {
                            Napier.d("E2EE_ENCRYPT: Establishing session with $userId", tag = "E2EE")
                            repository.ensureSession(userId)
                            val encrypted = signalProtocolManager.encryptMessage(userId, contentBytes)
                            userId to Json.encodeToJsonElement(EncryptedMessage.serializer(), encrypted)
                        } catch (e: Exception) {
                            Napier.w("E2EE_ENCRYPT: Failed to encrypt for $userId: ${e.message}", tag = "E2EE")
                            null
                        }
                    }
                }.awaitAll().filterNotNull().toMap()
            }

            if (payloadMap.size < otherParticipants.size) {
                return Result.failure(
                    Exception("Encryption failed for one or more recipients. Message not sent.")
                )
            }

            // Multi-device support: also encrypt for the sender so their other logged-in
            // devices can decrypt this message. Without this, Device B (same account as
            // the sender) has no entry in the payload map and won't decrypt this message.
            val finalPayloadMap: Map<String, JsonElement> = if (currentUserId !in payloadMap.keys) {
                try {
                    Napier.d("E2EE_ENCRYPT: Encrypting for sender ($currentUserId) for multi-device support", tag = "E2EE")
                    repository.ensureSession(currentUserId)
                    val encryptedForSelf = signalProtocolManager.encryptMessage(currentUserId, contentBytes)
                    payloadMap + (currentUserId to Json.encodeToJsonElement(EncryptedMessage.serializer(), encryptedForSelf))
                } catch (selfErr: Exception) {
                    Napier.w("E2EE_ENCRYPT: Failed to encrypt for self — other devices won't decrypt: ${selfErr.message}", tag = "E2EE")
                    payloadMap
                }
            } else {
                payloadMap
            }

            val encryptedPayload = JsonObject(finalPayloadMap).toString()
            Napier.d("E2EE_ENCRYPT: Message encrypted for ${finalPayloadMap.size} participants (incl. sender for multi-device)", tag = "E2EE")

            val sendResult = repository.sendMessage(
                chatId = chatId,
                content = encryptedPayload,
                mediaUrl = null,
                messageType = messageType,
                expiresAt = expiresAt,
                replyToId = replyToId,
                senderPlaintext = jsonPayload,
                attachments = attachments,
                metadataContainer = metadataContainer
            )
            if (requireE2EE) sendResult.map { it.copy(mediaUrl = mediaUrl) } else sendResult
        } catch (e: Exception) {
            Napier.e("E2EE_ENCRYPT: Failed: ${e.message}", tag = "E2EE", throwable = e)
            Result.failure(Exception("Encryption Error: ${e.message ?: "Unknown error"}"))
        }
    }
}
