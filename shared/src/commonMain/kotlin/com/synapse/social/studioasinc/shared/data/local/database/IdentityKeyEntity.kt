package com.synapse.social.studioasinc.shared.data.local.database





data class IdentityKeyEntity(

    val userId: String,
    val publicKey: String,
    val privateKey: String,
    val registrationId: Int,
    val createdAt: Long
)
