package com.synapse.social.studioasinc.shared.domain.usecase.chat
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

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

class SendMessageUseCase(
    private val repository: ChatRepository,
    private val signalProtocolManager: SignalProtocolManager? = null
) {
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

        if (signalProtocolManager == null) {
            return Result.failure(Exception("E2EE not initialized - SignalProtocolManager is null"))
        }

        return try {
            val groupMembers = repository.getParticipantIds(chatId).getOrElse {
                return Result.failure(Exception("Failed to fetch participants for encryption"))
            }

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

            val payloadMap = coroutineScope {
                otherParticipants.map { userId ->
                    async {
                        Napier.d("E2EE_ENCRYPT: Establishing session with $userId", tag = "E2EE")
                        repository.ensureSession(userId)
                        val encrypted = signalProtocolManager.encryptMessage(userId, contentBytes)
                        userId to Json.encodeToJsonElement(EncryptedMessage.serializer(), encrypted)
                    }
                }.awaitAll().toMap()
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
            Result.failure(Exception("Encryption Error: ${e.message}"))
        }
    }
}
