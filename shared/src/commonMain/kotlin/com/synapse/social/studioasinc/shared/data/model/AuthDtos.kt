package com.synapse.social.studioasinc.shared.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserSettingsInsert(
    val user_id: String
)

@Serializable
data class UserPresenceInsert(
    val user_id: String
)

@Serializable
// SECURITY: Sensitive fields (account_premium, verify, banned) are intentionally omitted.
data class UserProfileInsert(
    @SerialName("uid") val uid: String,
    val username: String
)
