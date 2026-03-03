package com.synapse.social.studioasinc.core.util

import kotlinx.serialization.json.*

val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
}

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Map<*, *> -> this.toJsonObject()
    is List<*> -> this.toJsonArray()
    is Boolean -> JsonPrimitive(this)
    is Number -> {
        if (this is Int || this is Long || this is Short || this is Byte) {
            JsonPrimitive(this)
        } else {
            val d = this.toDouble()
            if (!d.isFinite()) {
                JsonNull
            } else if (d % 1.0 == 0.0) {
                JsonPrimitive(d.toLong())
            } else {
                JsonPrimitive(this)
            }
        }
    }
    is String -> JsonPrimitive(this)
    is Enum<*> -> JsonPrimitive(this.name)
    else -> JsonPrimitive(this.toString())
}

fun Map<*, *>.toJsonObject(): JsonObject = buildJsonObject {
    forEach { (key, value) ->
        put(key.toString(), value.toJsonElement())
    }
}

fun List<*>.toJsonArray(): JsonArray = buildJsonArray {
    forEach { add(it.toJsonElement()) }
}
