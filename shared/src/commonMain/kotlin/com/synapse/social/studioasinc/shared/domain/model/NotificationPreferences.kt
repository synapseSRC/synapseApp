package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.json.JsonObject

data class NotificationPreferences(
    val userId: String,
    val enabled: Boolean,
    val settings: JsonObject,
    val quietHours: JsonObject,
    val doNotDisturb: Boolean,
    val dndUntil: String?,
    val updatedAt: String?
)
