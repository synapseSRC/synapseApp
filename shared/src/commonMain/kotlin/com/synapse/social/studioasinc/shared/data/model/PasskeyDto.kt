package com.synapse.social.studioasinc.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PasskeyDto(
    val id: String,
    val user_id: String,
    val credential_id: String,
    val device_name: String,
    val date_added: Long,
    val last_used: Long? = null
)
