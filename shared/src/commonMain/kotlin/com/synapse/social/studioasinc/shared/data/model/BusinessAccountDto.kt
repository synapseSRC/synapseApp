package com.synapse.social.studioasinc.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BusinessAccountDto(
    val user_id: String,
    val account_type: String,
    val monetization_enabled: Boolean,
    val verification_status: String
)
