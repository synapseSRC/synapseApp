package com.synapse.social.studioasinc.data.remote.services

import io.github.aakira.napier.Napier
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.delay

class AuthErrorHandler {
    companion object {
        private const val TAG = "AuthErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L

        fun handleAuthError(error: Throwable): AuthError {
            Napier.d("Handling auth error: ${error.message}", tag = TAG)
            logAuthenticationError(error)

            val rootCause = unwrapRootCause(error)
            val allMessages = collectMessages(error)
            val rootClassName = rootCause::class.java.simpleName
            val excClassName = error::class.java.simpleName

            // 1. Supabase Not Configured
            if (allMessages.contains("not configured") ||
                allMessages.contains("configuration problem") ||
                allMessages.contains("placeholder.supabase.co") ||
                allMessages.contains("your-project.supabase.co") ||
                allMessages.contains("supabase credentials not configured")
            ) {
                return AuthError.SUPABASE_NOT_CONFIGURED
            }

            // 2. RestException
            val restException = findRestException(error)
            if (restException != null) {
                val restMsg = (restException.message ?: restException.toString()).lowercase()

                if (restMsg.contains("500") || restMsg.contains("502") || restMsg.contains("503") || restMsg.contains("504") ||
                    restMsg.contains("server error") || restMsg.contains("internal server error") ||
                    restMsg.contains("service unavailable") || restMsg.contains("bad gateway") || restMsg.contains("gateway timeout")
                ) {
                    return AuthError.SERVER_ERROR
                }

                if (restMsg.contains("email not confirmed") || restMsg.contains("email_not_confirmed") || restMsg.contains("email not verified")) {
                    return AuthError.EMAIL_NOT_VERIFIED
                }

                if (restMsg.contains("invalid login credentials") || restMsg.contains("invalid email or password") ||
                    restMsg.contains("invalid_credentials") || restMsg.contains("invalid grant") || restMsg.contains("user not found") ||
                    restMsg.contains("invalid request") || restMsg.contains("invalid")
                ) {
                    return AuthError.INVALID_CREDENTIALS
                }

                return AuthError.INVALID_CREDENTIALS
            }

            // 3. HTTP 5xx / Server Errors by message
            if (allMessages.contains("500") || allMessages.contains("502") || allMessages.contains("503") || allMessages.contains("504") ||
                allMessages.contains("server error") || allMessages.contains("internal server error") ||
                allMessages.contains("service unavailable") || allMessages.contains("bad gateway") || allMessages.contains("gateway timeout")
            ) {
                return AuthError.SERVER_ERROR
            }

            // 4. Email not verified by message
            if (allMessages.contains("email not confirmed") || allMessages.contains("email_not_confirmed") || allMessages.contains("email not verified")) {
                return AuthError.EMAIL_NOT_VERIFIED
            }

            // 5. Invalid credentials by message
            if (allMessages.contains("invalid login credentials") || allMessages.contains("invalid email or password") ||
                allMessages.contains("invalid_credentials") || allMessages.contains("invalid grant") || allMessages.contains("user not found") ||
                allMessages.contains("invalid request")
            ) {
                return AuthError.INVALID_CREDENTIALS
            }

            // 6. DNS / Host Resolution
            if (rootClassName.contains("UnknownHostException", ignoreCase = true) ||
                rootClassName.contains("UnresolvedAddressException", ignoreCase = true) ||
                excClassName.contains("UnknownHostException", ignoreCase = true) ||
                excClassName.contains("UnresolvedAddressException", ignoreCase = true) ||
                allMessages.contains("unknownhostexception") ||
                allMessages.contains("unresolvedaddressexception") ||
                allMessages.contains("unable to resolve host") ||
                allMessages.contains("no address associated with hostname") ||
                allMessages.contains("name or service not known")
            ) {
                return AuthError.DNS_ERROR
            }

            // 7. Timeout
            if (rootClassName.contains("Timeout", ignoreCase = true) ||
                excClassName.contains("Timeout", ignoreCase = true) ||
                allMessages.contains("sockettimeoutexception") ||
                allMessages.contains("connecttimeoutexception") ||
                allMessages.contains("httprequesttimeoutexception") ||
                allMessages.contains("timeout")
            ) {
                return AuthError.TIMEOUT_ERROR
            }

            // 8. Connection Failure / Network
            if (rootClassName.contains("ConnectException", ignoreCase = true) ||
                rootClassName.contains("SocketException", ignoreCase = true) ||
                rootClassName.contains("SSLException", ignoreCase = true) ||
                rootClassName.contains("IOException", ignoreCase = true) ||
                excClassName.contains("ConnectException", ignoreCase = true) ||
                excClassName.contains("SocketException", ignoreCase = true) ||
                excClassName.contains("SSLException", ignoreCase = true) ||
                excClassName.contains("IOException", ignoreCase = true) ||
                allMessages.contains("failed to connect") ||
                allMessages.contains("connection refused") ||
                allMessages.contains("connection reset") ||
                allMessages.contains("cleartext communication not permitted") ||
                allMessages.contains("network") ||
                allMessages.contains("connection")
            ) {
                return AuthError.NETWORK_ERROR
            }

            return AuthError.UNKNOWN_ERROR
        }

        fun getErrorMessage(error: AuthError): String {
            return when (error) {
                AuthError.EMAIL_NOT_VERIFIED ->
                    "Please verify your email address before signing in."
                AuthError.INVALID_CREDENTIALS ->
                    "Invalid email or password."
                AuthError.NETWORK_ERROR ->
                    "Unable to reach the authentication server. Please check your connection and try again."
                AuthError.DNS_ERROR ->
                    "Unable to reach the authentication server. Please check your connection and try again."
                AuthError.TIMEOUT_ERROR ->
                    "The authentication server took too long to respond. Please try again."
                AuthError.SERVER_ERROR ->
                    "The authentication service is temporarily unavailable. Please try again later."
                AuthError.SUPABASE_NOT_CONFIGURED ->
                    "Authentication service is unavailable due to a configuration problem."
                AuthError.UNKNOWN_ERROR ->
                    "An unexpected error occurred. Please try again later."
            }
        }

        fun isRecoverableError(error: AuthError): Boolean {
            return when (error) {
                AuthError.NETWORK_ERROR,
                AuthError.DNS_ERROR,
                AuthError.TIMEOUT_ERROR,
                AuthError.SERVER_ERROR -> true
                AuthError.INVALID_CREDENTIALS,
                AuthError.EMAIL_NOT_VERIFIED,
                AuthError.SUPABASE_NOT_CONFIGURED,
                AuthError.UNKNOWN_ERROR -> false
            }
        }

        fun getRecoveryAction(error: AuthError): RecoveryAction {
            return when (error) {
                AuthError.EMAIL_NOT_VERIFIED -> RecoveryAction.RESEND_VERIFICATION
                AuthError.INVALID_CREDENTIALS -> RecoveryAction.RETRY_WITH_CORRECT_CREDENTIALS
                AuthError.NETWORK_ERROR,
                AuthError.DNS_ERROR,
                AuthError.TIMEOUT_ERROR,
                AuthError.SERVER_ERROR -> RecoveryAction.RETRY_WITH_DELAY
                AuthError.SUPABASE_NOT_CONFIGURED -> RecoveryAction.CHECK_CONFIGURATION
                AuthError.UNKNOWN_ERROR -> RecoveryAction.RETRY_WITH_DELAY
            }
        }

        suspend fun <T> executeWithRetry(
            maxAttempts: Int = MAX_RETRY_ATTEMPTS,
            initialDelay: Long = RETRY_DELAY_MS,
            operation: suspend () -> T
        ): T {
            var currentDelay = initialDelay
            var lastException: Exception? = null

            repeat(maxAttempts) { attempt ->
                try {
                    Napier.d("Executing operation, attempt ${attempt + 1}/$maxAttempts", tag = TAG)
                    return operation()
                } catch (e: Exception) {
                    lastException = e
                    val authError = handleAuthError(e)

                    Napier.w("Operation failed on attempt ${attempt + 1}: ${e.message}", tag = TAG)

                    if (!isRecoverableError(authError) || attempt == maxAttempts - 1) {
                        Napier.e("Non-recoverable error or max attempts reached", throwable = e, tag = TAG)
                        throw e
                    }

                    Napier.d("Retrying in ${currentDelay}ms...", tag = TAG)
                    delay(currentDelay)
                    currentDelay *= 2
                }
            }

            throw lastException ?: Exception("Operation failed after $maxAttempts attempts")
        }

        private fun logAuthenticationError(error: Throwable) {
            Napier.e("Authentication error occurred", throwable = error, tag = TAG)

            when {
                error.message?.contains("email not confirmed", ignoreCase = true) == true -> {
                    Napier.i("Email verification required - user needs to check email", tag = TAG)
                }
                error.message?.contains("invalid", ignoreCase = true) == true -> {
                    Napier.i("Invalid credentials provided - user should check email/password", tag = TAG)
                }
                error is java.net.UnknownHostException -> {
                    Napier.w("Network connectivity issue - DNS resolution failed", tag = TAG)
                }
                error is java.net.SocketTimeoutException -> {
                    Napier.w("Network timeout - slow connection or server issues", tag = TAG)
                }
                error is java.io.IOException -> {
                    Napier.w("IO error during authentication - network or server issue", tag = TAG)
                }
            }
        }

        fun logVerificationAttempt(email: String, success: Boolean, errorMessage: String? = null) {
            if (success) {
                Napier.i("Email verification successful for: $email", tag = TAG)
            } else {
                Napier.w("Email verification failed for: $email, error: $errorMessage", tag = TAG)
            }
        }

        fun logResendVerificationAttempt(email: String, success: Boolean, errorMessage: String? = null) {
            if (success) {
                Napier.i("Resend verification email successful for: $email", tag = TAG)
            } else {
                Napier.w("Resend verification email failed for: $email, error: $errorMessage", tag = TAG)
            }
        }

        private fun unwrapRootCause(throwable: Throwable): Throwable {
            var cause: Throwable = throwable
            val visited = mutableSetOf<Throwable>()
            while (cause.cause != null && cause.cause != cause && visited.add(cause)) {
                cause = cause.cause!!
            }
            return cause
        }

        private fun findRestException(throwable: Throwable): RestException? {
            var curr: Throwable? = throwable
            val visited = mutableSetOf<Throwable>()
            while (curr != null && visited.add(curr)) {
                if (curr is RestException) return curr
                curr = curr.cause
            }
            return null
        }

        private fun collectMessages(throwable: Throwable): String {
            val sb = StringBuilder()
            var curr: Throwable? = throwable
            val visited = mutableSetOf<Throwable>()
            while (curr != null && visited.add(curr)) {
                curr.message?.let { sb.append(it).append(" ") }
                curr = curr.cause
            }
            return sb.toString().lowercase()
        }
    }
}

enum class RecoveryAction {
    RESEND_VERIFICATION,
    RETRY_WITH_CORRECT_CREDENTIALS,
    RETRY_WITH_DELAY,
    CHECK_CONFIGURATION
}
