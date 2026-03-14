package com.synapse.social.studioasinc.shared.domain.model.mesh

import kotlinx.serialization.Serializable

@Serializable
data class MeshPeer(
    val id: String,
    val name: String,
    val lastSeen: Long,
    val isNearby: Boolean = true
)
