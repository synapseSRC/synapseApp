package com.synapse.social.studioasinc.shared.domain.model

sealed class AuthError : Exception() {
    data class NetworkError(override val message: String = "Network error") : AuthError()
    data class InvalidCredentials(override val message: String = "Invalid credentials") : AuthError()
    data class UserCollision(override val message: String = "User already exists") : AuthError()
    data class WeakPassword(override val message: String = "Password is too weak") : AuthError()
    data class ValidationFailed(override val message: String) : AuthError()
    data class Unknown(override val message: String = "Unknown error") : AuthError()
}
