package com.synapse.social.studioasinc.shared.domain.model

sealed class AuthError : Exception() {
    data class NetworkError(
        override val message: String = "Unable to reach the authentication server. Please check your connection and try again.",
        override val cause: Throwable? = null
    ) : AuthError()

    data class DnsResolutionError(
        override val message: String = "Unable to reach the authentication server. Please check your connection and try again.",
        override val cause: Throwable? = null
    ) : AuthError()

    data class ConnectionError(
        override val message: String = "Unable to reach the authentication server. Please check your connection and try again.",
        override val cause: Throwable? = null
    ) : AuthError()

    data class Timeout(
        override val message: String = "The authentication server took too long to respond. Please try again.",
        override val cause: Throwable? = null
    ) : AuthError()

    data class ServerError(
        override val message: String = "The authentication service is temporarily unavailable. Please try again later.",
        override val cause: Throwable? = null
    ) : AuthError()

    data class InvalidCredentials(
        override val message: String = "Invalid email or password.",
        override val cause: Throwable? = null
    ) : AuthError()

    data class EmailNotVerified(
        override val message: String = "Please verify your email address before signing in.",
        override val cause: Throwable? = null
    ) : AuthError()

    data class ConfigurationError(
        override val message: String = "Authentication service is unavailable due to a configuration problem.",
        override val cause: Throwable? = null
    ) : AuthError()

    data class UserCollision(
        override val message: String = "This email is already registered",
        override val cause: Throwable? = null
    ) : AuthError()

    data class WeakPassword(
        override val message: String = "Password must be at least 8 characters",
        override val cause: Throwable? = null
    ) : AuthError()

    data class ValidationFailed(
        override val message: String,
        override val cause: Throwable? = null
    ) : AuthError()

    data class Unknown(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AuthError()
}
