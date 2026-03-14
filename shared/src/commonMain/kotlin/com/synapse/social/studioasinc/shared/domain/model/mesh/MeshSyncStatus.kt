package com.synapse.social.studioasinc.shared.domain.model.mesh

import kotlinx.serialization.Serializable

@Serializable
data class MeshSyncStatus(
    val messageId: String,
    val status: String, // "pending", "synced", "failed"
    val lastAttempt: Long
)
