package com.synapse.social.studioasinc.shared.data.crypto.models

import kotlinx.serialization.Serializable

@Serializable
data class SignalIdentityKeys(
    val registrationId: Int,
    val identityKey: String,
    val signedPreKeyId: Int,
    val signedPreKey: String,
    val signedPreKeySignature: String
)

@Serializable
data class SignalOneTimePreKey(
    val keyId: Int,
    val publicKey: String
)

@Serializable
data class PreKeyBundle(
    val registrationId: Int,
    val deviceId: Int,
    val preKeyId: Int?,
    val preKeyPublic: String?,
    val signedPreKeyId: Int,
    val signedPreKeyPublic: String,
    val signedPreKeySignature: String,
    val identityKey: String
)

@Serializable
data class EncryptedMessage(
    val type: Int,
    val body: String,
    val registrationId: Int
)
