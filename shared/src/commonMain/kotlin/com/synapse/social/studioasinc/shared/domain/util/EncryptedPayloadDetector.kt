package com.synapse.social.studioasinc.shared.domain.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object EncryptedPayloadDetector {
    private val recipientIdPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val encryptedBodyPattern = Regex("^[A-Za-z0-9+/_-]+={0,2}$")
    private val legacyPlaceholders = setOf(
        "Message is encrypted",
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
            val envelope = value as? JsonObject ?: return false
            if (!isSingleCipherEnvelope(envelope)) return false
        }
        return true
    }
}
