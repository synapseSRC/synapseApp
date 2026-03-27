package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Helper to extract the human-readable content from the decrypted JSON payload.
 * The encrypted payload format is: {"content":"actual message", "mediaUrl":"..."}
 * Returns a Pair of (content, mediaUrl?).
 */
private fun extractFromJsonPayload(decryptedString: String): Pair<String, String?> {
    return try {
        val jsonPayload = Json.parseToJsonElement(decryptedString).jsonObject
        val content = jsonPayload["content"]?.jsonPrimitive?.content ?: decryptedString
        val mediaUrl = jsonPayload["mediaUrl"]?.jsonPrimitive?.content
        Pair(content, mediaUrl)
    } catch (_: Exception) {
        // Not a JSON payload — treat the whole string as content
        Pair(decryptedString, null)
    }
}

internal suspend fun decryptMessageIfNecessary(
    message: Message,
    currentUserId: String,
    signalProtocolManager: SignalProtocolManager?
): Message {
    if (!message.isEncrypted || message.encryptedContent.isNullOrBlank()) return message

    // Check if the message is already decrypted (content is not a placeholder)
    val placeholders = setOf(
        "Message is encrypted", 
        "🔒 Encrypted message", 
        "🔒 You sent an encrypted message",
        "🔒 You sent an encrypted message (Copy)"
    )
    if (message.content !in placeholders && message.content.isNotBlank()) {
        // The message has already been decrypted by another layer (e.g. Repository)
        return message
    }

    if (signalProtocolManager == null) {
        Napier.e("E2EE_DECRYPT: SignalProtocolManager is null, cannot decrypt", tag = "E2EE")
        return message
    }

    try {
        val jsonElement = Json.parseToJsonElement(message.encryptedContent) as JsonObject
        val senderId = message.senderId
        val messageId = message.id

        if (senderId == currentUserId) {
            // SENDER COPY — stored as a plaintext JsonPrimitive (not Signal-encrypted)
            // in the encrypted blob to avoid Double Ratchet desync.
            val myPayloadElement = jsonElement[currentUserId]
            if (myPayloadElement != null) {
                try {
                    val plainText = myPayloadElement.jsonPrimitive.content
                    val (content, mediaUrl) = extractFromJsonPayload(plainText)
                    Napier.d("E2EE_DECRYPT: Retrieved sender's plaintext copy for $messageId", tag = "E2EE")
                    return message.copy(
                        content = content,
                        mediaUrl = mediaUrl ?: message.mediaUrl
                    )
                } catch (_: Exception) {
                    Napier.w("E2EE_DECRYPT: Sender copy exists but not a primitive for $messageId", tag = "E2EE")
                }
            }
            // Fallback: no plaintext copy found (old message format)
            Napier.d("E2EE_DECRYPT: No sender plaintext copy for $messageId, using cached/local content", tag = "E2EE")
            return if (message.content == "Message is encrypted" || message.content == "🔒 Encrypted message") {
                message.copy(content = "🔒 You sent an encrypted message")
            } else {
                message
            }
        } else {
            // RECIPIENT COPY — decrypt using Signal Protocol
            val myPayloadElement = jsonElement[currentUserId]
            if (myPayloadElement != null) {
                try {
                    val myPayload = Json.decodeFromJsonElement(EncryptedMessage.serializer(), myPayloadElement)
                    val decryptedBytes = signalProtocolManager.decryptMessage(senderId, myPayload)
                    val decryptedString = decryptedBytes.decodeToString()
                    Napier.d("E2EE_DECRYPT: Successfully decrypted message $messageId", tag = "E2EE")

                    // Extract the actual content from the JSON payload
                    val (content, mediaUrl) = extractFromJsonPayload(decryptedString)
                    return message.copy(
                        content = content,
                        mediaUrl = mediaUrl ?: message.mediaUrl
                    )
                } catch (e: Exception) {
                    Napier.e("E2EE_DECRYPT: Decryption failed for message $messageId. Content might stay encrypted.", tag = "E2EE", throwable = e)
                    // We DO NOT delete the session here anymore. Automatic session deletion is too destructive
                    // and causes "un-encrypted" messages if triggered by double-decryption or temporary desync.
                }
            } else {
                Napier.w("E2EE_DECRYPT: No payload found for current user in message $messageId. Available keys: ${jsonElement.keys}", tag = "E2EE")
            }
        }
    } catch (e: Exception) {
        Napier.e("E2EE_DECRYPT: Critical failure parsing payload for message ${message.id}: ${e.message}", tag = "E2EE", throwable = e)
    }

    return message
}
