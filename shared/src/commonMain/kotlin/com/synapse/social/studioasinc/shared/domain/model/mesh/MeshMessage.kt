package com.synapse.social.studioasinc.shared.domain.model.mesh

import kotlinx.serialization.Serializable

@Serializable
data class MeshMessage(
    val id: String,
    val senderId: String,
    val recipientId: String?, // null for broadcast
    val chatId: String?,
    val content: String,
    val timestamp: Long,
    val type: String = "text", // "text", "post", "presence"
    val isSynced: Boolean = false
)
