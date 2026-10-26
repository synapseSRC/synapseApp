package com.synapse.social.studioasinc.shared.domain.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object EncryptedPayloadDetector {
    /** Canonical legacy placeholder also used as the provider-neutral preview for undecryptable content. */
    const val PLACEHOLDER = "Message is encrypted"

    private val recipientIdPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val encryptedBodyPattern = Regex("^[A-Za-z0-9+/_-]+={0,2}$")
    private val legacyPlaceholders = setOf(
        PLACEHOLDER,
        "🔒 Encrypted message",
        "🔒 You sent an encrypted message",
        "🔒 You sent an encrypted message (Copy)"
    )

    private fun isSingleCipherEnvelope(obj: JsonObject): Boolean {
        val type = try {
            obj["type"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: obj["type"]?.toString()?.trim('"')?.toIntOrNull()
        } catch (_: Exception) { null } ?: return false
        if (type <= 0) return false

        val body = try {
            obj["body"]?.jsonPrimitive?.content
                ?: obj["body"]?.toString()?.trim('"')
        } catch (_: Exception) { null } ?: return false
        if (body.isEmpty() || !encryptedBodyPattern.matches(body)) return false

        val regId = try {
            obj["registrationId"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: obj["registrationId"]?.toString()?.trim('"')?.toIntOrNull()
        } catch (_: Exception) { null } ?: return false
        return regId > 0
    }

    private fun JsonElement.asCipherEnvelopeOrNull(): JsonObject? =
        (this as? JsonObject)?.takeIf(::isSingleCipherEnvelope)

    /**
     * The sender's own entry in a multi-recipient envelope is a plaintext JSON payload
     * (`{"content":...}`) so the sending device and its other devices can display it
     * without consuming a Signal ratchet key. It is part of the envelope, not user text.
     */
    private fun JsonElement.isSenderPlaintextCopy(): Boolean {
        if (this !is JsonPrimitive || !isString) return false
        val parsed = try { Json.parseToJsonElement(content) as? JsonObject } catch (_: Exception) { null }
            ?: return false
        return parsed.containsKey("content")
    }

    fun isPlaceholder(content: String?): Boolean {
        if (content == null) return false
        return content.trim() in legacyPlaceholders
    }

    fun isEncryptedPayload(content: String?): Boolean {
        if (content.isNullOrBlank()) return false
        val trimmed = content.trim()
        if (trimmed in legacyPlaceholders) return true
        val root = try { Json.parseToJsonElement(trimmed) as? JsonObject } catch (_: Exception) { null }
            ?: return false
        if (root.isEmpty()) return false

        if (isSingleCipherEnvelope(root)) return true

        for ((key, value) in root) {
            if (!recipientIdPattern.matches(key)) return false
            if (value.asCipherEnvelopeOrNull() == null && !value.isSenderPlaintextCopy()) return false
        }
        // Every value is part of an envelope (cipher envelope or sender plaintext copy), so the
        // map itself is machine data even when this device only received the sender copy.
        return true
    }

    /** True when [content] must never be rendered as ordinary message text. */
    fun isUndecryptable(content: String?, failureReason: String? = null): Boolean =
        failureReason != null || isEncryptedPayload(content)
}
