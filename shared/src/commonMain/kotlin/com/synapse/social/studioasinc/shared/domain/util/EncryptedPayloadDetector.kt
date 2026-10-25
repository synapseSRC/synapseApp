package com.synapse.social.studioasinc.shared.domain.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object EncryptedPayloadDetector {
    private val recipientIdPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val encryptedBodyPattern = Regex("^[A-Za-z0-9+/_-]+={0,2}$")
    private val legacyPlaceholders = setOf("Message is encrypted")

    fun isEncryptedPayload(content: String): Boolean {
        val trimmed = content.trim()
        if (trimmed in legacyPlaceholders) return true
        val root = try { Json.parseToJsonElement(trimmed) as? JsonObject } catch (_: Exception) { null } ?: return false
        if (root.isEmpty()) return false
        for ((key, value) in root) {
            if (!recipientIdPattern.matches(key)) return false
            val envelope = value as? JsonObject ?: return false
            if (envelope["type"]?.toString()?.trim('"')?.toIntOrNull() != 2) return false
            val body = envelope["body"]?.toString()?.trim('"') ?: return false
            if (!encryptedBodyPattern.matches(body)) return false
            val regId = envelope["registrationId"]?.toString()?.trim('"')?.toIntOrNull() ?: return false
            if (regId <= 0) return false
        }
        return true
    }
}
