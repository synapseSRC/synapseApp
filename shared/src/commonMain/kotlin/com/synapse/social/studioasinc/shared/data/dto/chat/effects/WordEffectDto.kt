package com.synapse.social.studioasinc.shared.data.dto.chat.effects

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors one row of the public.chat_word_effects table. */
@Serializable
data class WordEffectDto(
    val id: String? = null,
    @SerialName("chat_id") val chatId: String,
    val trigger: String,
    val emoji: String,
    val animation: String,
    @EncodeDefault @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
