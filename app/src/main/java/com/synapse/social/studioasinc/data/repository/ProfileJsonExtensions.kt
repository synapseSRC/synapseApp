package com.synapse.social.studioasinc.data.repository

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal fun JsonObject.getString(key: String, default: String = ""): String =
    this[key]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull ?: default

internal fun JsonObject.getNullableString(key: String): String? {
    val element = this[key]
    return when {
        element == null || element is JsonNull -> null
        element is JsonPrimitive -> element.contentOrNull
        element is JsonArray -> null // Handle arrays gracefully
        else -> null
    }
}

internal fun JsonObject.getBoolean(key: String, default: Boolean = false): Boolean =
    this[key]?.let { if (it is JsonPrimitive) it else null }?.booleanOrNull ?: default

internal fun JsonObject.getInt(key: String, default: Int = 0): Int =
    this[key]?.let { if (it is JsonPrimitive) it else null }?.intOrNull ?: default

internal fun JsonObject.getLong(key: String, default: Long = 0L): Long =
    this[key]?.let { if (it is JsonPrimitive) it else null }?.longOrNull ?: default

internal fun parseDateToLong(dateStr: String?): Long {
    if (dateStr.isNullOrBlank()) return 0L
    return try {
        java.time.Instant.parse(dateStr).toEpochMilli()
    } catch (e: Exception) {
        0L
    }
}
