package com.synapse.social.studioasinc.shared.domain.model

data class OAuthDeepLink(
    val provider: String? = null,
    val code: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val type: String? = null,
    val error: String? = null,
    val errorCode: String? = null,
    val errorDescription: String? = null
)
