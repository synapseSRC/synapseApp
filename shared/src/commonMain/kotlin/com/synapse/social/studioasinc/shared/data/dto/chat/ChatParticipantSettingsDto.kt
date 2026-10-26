package com.synapse.social.studioasinc.shared.data.dto.chat

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatParticipantSettingsDto(
    val id: String? = null,
    @SerialName("chat_id") val chatId: String,
    @SerialName("user_id") val userId: String,
    @EncodeDefault @SerialName("is_pinned") val isPinned: Boolean = false,
    @EncodeDefault @SerialName("is_muted") val isMuted: Boolean = false,
    @EncodeDefault @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("sync_version") val syncVersion: Int = 1,
    @SerialName("updated_at") val updatedAt: String? = null
)
