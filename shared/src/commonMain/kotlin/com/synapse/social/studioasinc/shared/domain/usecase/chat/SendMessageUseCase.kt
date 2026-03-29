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

        var isEncrypted = false
        var encryptedContentStr: String? = null
        var finalContent = content
        var failureReason: String? = null

        if (currentUserId == null) {
            return Result.failure(Exception("Not logged in"))
        }

        if (signalProtocolManager == null) {
            failureReason = "E2EE not initialized - SignalProtocolManager is null"
            Napier.e("E2EE_SEND: $failureReason. Check DI configuration.", tag = "E2EE")
        } else {
            Napier.d("E2EE_SEND: SignalProtocolManager is available, attempting encryption", tag = "E2EE")
            try {
                val groupMembersResult = repository.getParticipantIds(chatId)
                if (groupMembersResult.isSuccess) {
                    val groupMembers = groupMembersResult.getOrThrow()
                    val otherParticipants = groupMembers.filter { it != currentUserId }
                    if (otherParticipants.isEmpty() && groupMembers.isNotEmpty()) {
                        // Chatting with self — still encrypt for self
                    }
                    // Include sender so their copy is also Signal-encrypted (not plaintext)
                    val allParticipants = (otherParticipants + currentUserId!!).distinct()
                    Napier.d("E2EE_ENCRYPT: All participants (incl. sender): $allParticipants", tag = "E2EE")

                    if (allParticipants.isNotEmpty()) {
                        val payloadMap = mutableMapOf<String, JsonElement>()
                        val jsonPayload = kotlinx.serialization.json.buildJsonObject {
                            put("content", content)
                            if (mediaUrl != null) {
                                put("mediaUrl", mediaUrl)
                            }
                        }.toString()
                        val contentBytes = jsonPayload.encodeToByteArray()
                        val failures = mutableListOf<String>()

                        for (userId in allParticipants) {
                            try {
                                Napier.d("E2EE_ENCRYPT: Establishing/Verifying session with $userId", tag = "E2EE")
                                repository.ensureSession(userId)

                                Napier.d("E2EE_ENCRYPT: Calling SignalProtocolManager.encryptMessage for $userId", tag = "E2EE")
                                val encryptedForReceiver = signalProtocolManager.encryptMessage(userId, contentBytes)
                                payloadMap[userId] = Json.encodeToJsonElement(EncryptedMessage.serializer(), encryptedForReceiver)
                            } catch (e: Throwable) {
                                val reason = when {
                                    e.message?.contains("hasn't enabled E2EE") == true -> "User $userId hasn't enabled E2EE"
                                    else -> "Encryption failed for $userId: ${e.message}"
                                }
                                failures.add(reason)
                                Napier.e("E2EE_ENCRYPT: $reason", tag = "E2EE", throwable = e)
                            }
                        }

                        if (failures.isEmpty() && payloadMap.isNotEmpty()) {
                            val payloads = JsonObject(payloadMap)

                            isEncrypted = true
                            encryptedContentStr = payloads.toString()
                            finalContent = "Message is encrypted"
                            Napier.d("E2EE_ENCRYPT: Message fully prepared for secure transit", tag = "E2EE")
                        } else {
                            failureReason = failures.joinToString(separator = ", ")
                            if (failureReason.isEmpty()) {
                                failureReason = "Failed to encrypt for any participant"
                            }
                            Napier.w("E2EE_ENCRYPT: Encryption failed. Reasons: $failureReason. Falling back to unencrypted.", tag = "E2EE")
                            // DO NOT RETURN FAILURE. Fallback to plaintext.
                            isEncrypted = false
                            encryptedContentStr = null
                            finalContent = content
                        }
                    } else {
                        failureReason = "Chat $chatId has no other participants to encrypt for"
                        Napier.w("E2EE_ENCRYPT: $failureReason", tag = "E2EE")
                        // If it's a 1-to-1 chat and we can't find the other person, it's a real error in E2EE context
                        return Result.failure(Exception(failureReason))
                    }
                } else {
                    failureReason = "Failed to fetch group members for encryption"
                    Napier.e("E2EE_ENCRYPT: $failureReason", tag = "E2EE")
                    return Result.failure(Exception(failureReason))
                }
            } catch (e: Exception) {
                failureReason = e.message ?: "Encryption failed during participant lookup"
                Napier.e("E2EE_ENCRYPT: $failureReason", tag = "E2EE", throwable = e)
                return Result.failure(Exception("Encryption Error: $failureReason"))
            }
        }

        return repository.sendMessage(
            chatId = chatId,
            content = finalContent,
            mediaUrl = if (isEncrypted) null else mediaUrl,
            messageType = messageType,
            isEncrypted = isEncrypted,
            encryptedContent = encryptedContentStr,
            expiresAt = expiresAt,
            replyToId = replyToId
        )
    }
}
