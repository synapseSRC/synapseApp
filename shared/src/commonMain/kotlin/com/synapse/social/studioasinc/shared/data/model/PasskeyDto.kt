package com.synapse.social.studioasinc.shared.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors a single item from the backend passkey listing exactly.
 * Timestamps are RFC3339 strings here; conversion to Domain units happens in [com.synapse.social.studioasinc.shared.data.mapper.PasskeyMapper].
 */
@Serializable
data class PasskeyDto(
    val id: String,
    val friendly_name: String? = null,
    val created_at: String? = null,
    val last_used_at: String? = null
)
