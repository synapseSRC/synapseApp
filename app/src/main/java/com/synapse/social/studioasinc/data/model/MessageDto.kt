package com.synapse.social.studioasinc.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    @SerialName("id") val id: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("content") val content: String,
    @SerialName("type") val type: Int,
    @SerialName("registration_id") val registrationId: Int,
    @SerialName("device_id") val deviceId: Int,
    @SerialName("created_at") val createdAt: String? = null
)
