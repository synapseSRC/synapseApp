package com.synapse.social.studioasinc.shared.domain.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class Passkey(
    val id: String,
    val deviceName: String,
    val dateAdded: Long,
    val lastUsed: Long? = null
)
