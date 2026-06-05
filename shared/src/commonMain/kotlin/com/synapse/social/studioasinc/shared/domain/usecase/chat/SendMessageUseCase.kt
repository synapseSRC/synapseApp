package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import io.github.aakira.napier.Napier
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
    /**
     * Executes the message sending flow.
     *
     * @param chatId The unique identifier of the conversation.
     * @param content The plaintext content of the message.
     * @param mediaUrl Optional URL to associated media (e.g., image, video).
     * @param messageType The category of the message (defaults to "text").
     * @param expiresAt Optional timestamp for disappearing messages.
     * @param replyToId Optional ID of the message being replied to.
     * @return A [Result] containing the sent [Message] on success, or an exception on failure.
     */
    suspend operator fun invoke(
        chatId: String,
        content: String,
        mediaUrl: String? = null,
        messageType: String = "text",
        expiresAt: String? = null,
        replyToId: String? = null
    ): Result<Message> {
        val currentUserId = repository.getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        // If SignalProtocolManager is unavailable, send plaintext directly.
        if (signalProtocolManager == null) {
            Napier.w("E2EE_ENCRYPT: SignalProtocolManager unavailable, sending plaintext", tag = "E2EE")
            return repository.sendMessage(
                chatId = chatId,
                content = content,
                mediaUrl = mediaUrl,
                messageType = messageType,
                expiresAt = expiresAt,
                replyToId = replyToId
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
            }.toString()
            val contentBytes = jsonPayload.encodeToByteArray()

            // Encrypt for each recipient. If any recipient hasn't set up E2EE keys,
            // fall back to sending plaintext so the message is never silently dropped.
            val payloadMap = mutableMapOf<String, JsonElement>()
            for (userId in otherParticipants) {
                try {
                    Napier.d("E2EE_ENCRYPT: Establishing session with $userId", tag = "E2EE")
                    repository.ensureSession(userId)
                    val encrypted = signalProtocolManager.encryptMessage(userId, contentBytes)
                    payloadMap[userId] = Json.encodeToJsonElement(EncryptedMessage.serializer(), encrypted)
                } catch (e: Exception) {
                    // Recipient has no E2EE keys — fall back to plaintext for the whole message.
                    Napier.w("E2EE_ENCRYPT: Recipient $userId has no E2EE keys (${e.message}), falling back to plaintext", tag = "E2EE")
                    return repository.sendMessage(
                        chatId = chatId,
                        content = content,
                        mediaUrl = mediaUrl,
                        messageType = messageType,
                        expiresAt = expiresAt,
                        replyToId = replyToId
                    )
                }
            }

            val encryptedPayload = JsonObject(payloadMap).toString()
            Napier.d("E2EE_ENCRYPT: Message encrypted for ${payloadMap.size} recipients", tag = "E2EE")

            repository.sendMessage(
                chatId = chatId,
                content = encryptedPayload,
                mediaUrl = null, // mediaUrl is embedded in the encrypted payload
                messageType = messageType,
                expiresAt = expiresAt,
                replyToId = replyToId,
                senderPlaintext = jsonPayload
            )
        } catch (e: Exception) {
            Napier.e("E2EE_ENCRYPT: Failed: ${e.message}", tag = "E2EE", throwable = e)
            Result.failure(Exception("Encryption Error: ${e.message ?: "Unknown error"}"))
        }
    }
}
