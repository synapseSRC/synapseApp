package com.synapse.social.studioasinc.data.remote.services

data class User(
    val id: String,
    val email: String,
    val emailConfirmed: Boolean = false,
    val createdAt: String? = null
)



data class AuthResult(
    val user: User?,
    val needsEmailVerification: Boolean = false,
    val message: String? = null
)



enum class AuthError {
    EMAIL_NOT_VERIFIED,
    INVALID_CREDENTIALS,
    NETWORK_ERROR,
    SUPABASE_NOT_CONFIGURED,
    UNKNOWN_ERROR
}
