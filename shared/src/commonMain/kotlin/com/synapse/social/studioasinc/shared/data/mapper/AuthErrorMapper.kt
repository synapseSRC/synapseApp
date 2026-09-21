package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.core.network.ConfigurationException
import com.synapse.social.studioasinc.shared.domain.model.AuthError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException

object AuthErrorMapper {

    fun mapException(exception: Throwable): AuthError {
        val rootCause = unwrapRootCause(exception)
        val allMessages = collectMessages(exception)
        val rootClassName = rootCause::class.simpleName ?: ""
        val excClassName = exception::class.simpleName ?: ""

        // 1. Configuration Failure
        if (exception is ConfigurationException || rootCause is ConfigurationException ||
            allMessages.contains("not configured") ||
            allMessages.contains("configuration problem") ||
            allMessages.contains("placeholder.supabase.co") ||
            allMessages.contains("your-project.supabase.co") ||
            allMessages.contains("supabase credentials not configured")
        ) {
            return AuthError.ConfigurationError(
                message = "Authentication service is unavailable due to a configuration problem.",
                cause = rootCause
            )
        }

        // 2. HTTP 5xx / Server Errors
        if (allMessages.contains("500") || allMessages.contains("502") || allMessages.contains("503") || allMessages.contains("504") ||
            allMessages.contains("server error") || allMessages.contains("internal server error") ||
            allMessages.contains("service unavailable") || allMessages.contains("bad gateway") || allMessages.contains("gateway timeout")
        ) {
            return AuthError.ServerError(
                message = "The authentication service is temporarily unavailable. Please try again later.",
                cause = rootCause
            )
        }

        // 3. Email Not Verified
        if (allMessages.contains("email not confirmed") || allMessages.contains("email_not_confirmed") || allMessages.contains("email not verified")) {
            return AuthError.EmailNotVerified(
                message = "Please verify your email address before signing in.",
                cause = rootCause
            )
        }

        // 4. User Collision / Account Already Exists
        if (allMessages.contains("user already registered") || allMessages.contains("email already exists") ||
            (allMessages.contains("duplicate") && allMessages.contains("email")) || allMessages.contains("user_email_key") ||
            allMessages.contains("already in use")
        ) {
            return AuthError.UserCollision(
                message = "This email is already registered",
                cause = rootCause
            )
        }

        // 5. Invalid Credentials / Wrong Password
        if (allMessages.contains("invalid login credentials") || allMessages.contains("invalid email or password") ||
            allMessages.contains("invalid_credentials") || allMessages.contains("invalid grant") || allMessages.contains("user not found") ||
            allMessages.contains("invalid request")
        ) {
            return AuthError.InvalidCredentials(
                message = "Invalid email or password.",
                cause = rootCause
            )
        }

        // 6. Weak Password
        if (allMessages.contains("password") && (
                allMessages.contains("weak") ||
                allMessages.contains("too short") ||
                allMessages.contains("at least") ||
                allMessages.contains("minimum")
            )
        ) {
            return AuthError.WeakPassword(
                message = "Password must be at least 8 characters",
                cause = rootCause
            )
        }

        // 7. Validation Failed
        if (allMessages.contains("invalid email") || allMessages.contains("email format")) {
            return AuthError.ValidationFailed(
                message = "Please enter a valid email address",
                cause = rootCause
            )
        }

        // 8. Supabase RestException specifics
        val restException = findRestException(exception)
        if (restException != null) {
            val restMsg = (restException.message ?: restException.toString()).lowercase()
            if (restMsg.contains("invalid")) {
                return AuthError.InvalidCredentials(
                    message = "Invalid email or password.",
                    cause = restException
                )
            }
            return AuthError.Unknown(
                message = "An unexpected error occurred. Please try again later.",
                cause = restException
            )
        }

        // 9. DNS / Host Resolution Failure
        if (rootClassName == "UnknownHostException" || rootClassName == "UnresolvedAddressException" ||
            excClassName == "UnknownHostException" || excClassName == "UnresolvedAddressException" ||
            allMessages.contains("unknownhostexception") ||
            allMessages.contains("unresolvedaddressexception") ||
            allMessages.contains("unable to resolve host") ||
            allMessages.contains("no address associated with hostname") ||
            allMessages.contains("name or service not known")
        ) {
            return AuthError.DnsResolutionError(
                message = "Unable to reach the authentication server. Please check your connection and try again.",
                cause = rootCause
            )
        }

        // 10. Timeout
        if (rootClassName == "SocketTimeoutException" || rootClassName == "ConnectTimeoutException" ||
            rootClassName == "HttpRequestTimeoutException" || rootClassName == "TimeoutCancellationException" ||
            excClassName == "SocketTimeoutException" || excClassName == "ConnectTimeoutException" ||
            excClassName == "HttpRequestTimeoutException" || excClassName == "TimeoutCancellationException" ||
            allMessages.contains("sockettimeoutexception") ||
            allMessages.contains("connecttimeoutexception") ||
            allMessages.contains("httprequesttimeoutexception") ||
            allMessages.contains("timeoutcancellationexception") ||
            allMessages.contains("timed out")
        ) {
            return AuthError.Timeout(
                message = "The authentication server took too long to respond. Please try again.",
                cause = rootCause
            )
        }

        // 11. Connection Failure
        if (rootClassName == "ConnectException" || rootClassName == "SocketException" ||
            rootClassName == "SSLHandshakeException" || rootClassName == "SSLException" ||
            excClassName == "ConnectException" || excClassName == "SocketException" ||
            excClassName == "SSLHandshakeException" || excClassName == "SSLException" ||
            allMessages.contains("failed to connect") ||
            allMessages.contains("connection refused") ||
            allMessages.contains("connection reset") ||
            allMessages.contains("connection closed") ||
            allMessages.contains("cleartext communication not permitted") ||
            allMessages.contains("socket closed") ||
            allMessages.contains("broken pipe")
        ) {
            return AuthError.ConnectionError(
                message = "Unable to reach the authentication server. Please check your connection and try again.",
                cause = rootCause
            )
        }

        // Fallback Unknown Error
        return AuthError.Unknown(
            message = "An unexpected error occurred. Please try again later.",
            cause = rootCause
        )
    }

    private fun unwrapRootCause(throwable: Throwable): Throwable {
        var cause: Throwable = throwable
        val visited = mutableSetOf<Int>()
        while (cause.cause != null && cause.cause != cause && visited.add(System.identityHashCode(cause))) {
            cause = cause.cause!!
        }
        return cause
    }

    private fun findRestException(throwable: Throwable): RestException? {
        var curr: Throwable? = throwable
        val visited = mutableSetOf<Int>()
        while (curr != null && visited.add(System.identityHashCode(curr))) {
            if (curr is RestException) return curr
            curr = curr.cause
        }
        return null
    }

    private fun collectMessages(throwable: Throwable): String {
        val sb = StringBuilder()
        var curr: Throwable? = throwable
        val visited = mutableSetOf<Int>()
        while (curr != null && visited.add(System.identityHashCode(curr))) {
            curr.message?.let { sb.append(it).append(" ") }
            curr = curr.cause
        }
        return sb.toString().lowercase()
    }
}
