package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DraftType {
    POST,
    STORY,
    REPLY
}

@Serializable
data class Draft(
    val id: String,
    val type: DraftType,
    val content: String,
    val updatedAt: Long
)
