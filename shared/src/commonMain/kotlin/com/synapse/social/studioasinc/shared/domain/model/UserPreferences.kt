package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    @SerialName("user_id") val userId: String,
    @SerialName("security_notifications_enabled") val securityNotificationsEnabled: Boolean
)
