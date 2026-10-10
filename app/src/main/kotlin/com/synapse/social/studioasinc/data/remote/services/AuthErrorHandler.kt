package com.synapse.social.studioasinc.data.remote.services

import android.util.Log
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.delay

class AuthErrorHandler {
    companion object {
        private const val TAG = "AuthErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L

        fun handleAuthError(error: Throwable): AuthError {
            Log.d(TAG, "Handling auth error: ${error.message}")
            logAuthenticationError(error)

            return when (error) {
                is RestException -> {
                    val msg = error.message ?: ""
                    when {
                        msg.contains("email not confirmed", ignoreCase = true) -> AuthError.EMAIL_NOT_VERIFIED
                        msg.contains("invalid login credentials", ignoreCase = true) -> AuthError.INVALID_CREDENTIALS
                        msg.contains("invalid", ignoreCase = true) -> AuthError.INVALID_CREDENTIALS
                        else -> AuthError.UNKNOWN_ERROR
                    }
                }
                is HttpRequestException -> AuthError.NETWORK_ERROR
                is java.net.UnknownHostException -> AuthError.NETWORK_ERROR
                is java.net.SocketTimeoutException -> AuthError.NETWORK_ERROR
                is java.io.IOException -> AuthError.NETWORK_ERROR
                else -> {
                    val msg = error.message ?: ""
                    when {
                        msg.contains("network", ignoreCase = true) -> AuthError.NETWORK_ERROR
                        msg.contains("connection", ignoreCase = true) -> AuthError.NETWORK_ERROR
                        msg.contains("timeout", ignoreCase = true) -> AuthError.NETWORK_ERROR
                        msg.contains("unreachable", ignoreCase = true) -> AuthError.NETWORK_ERROR
                        else -> AuthError.UNKNOWN_ERROR
                    }
                }
            }
        }

        fun getErrorMessage(error: AuthError): String {
            return when (error) {
                AuthError.EMAIL_NOT_VERIFIED ->
                    "Please verify your email address to continue"
                AuthError.INVALID_CREDENTIALS ->
                    "Invalid email or password"
                AuthError.NETWORK_ERROR ->
                    "Network connection error. Please check your internet connection and try again."
                AuthError.SUPABASE_NOT_CONFIGURED ->
                    "Authentication service not configured"
                AuthError.UNKNOWN_ERROR ->
                    "An unexpected error occurred"
            }
        }



        fun isRecoverableError(error: AuthError): Boolean {
            return when (error) {
                AuthError.NETWORK_ERROR -> true
                AuthError.UNKNOWN_ERROR -> true
                else -> false
            }
        }



        fun getRecoveryAction(error: AuthError): RecoveryAction {
            return when (error) {
                AuthError.EMAIL_NOT_VERIFIED -> RecoveryAction.RESEND_VERIFICATION
                AuthError.INVALID_CREDENTIALS -> RecoveryAction.RETRY_WITH_CORRECT_CREDENTIALS
                AuthError.NETWORK_ERROR -> RecoveryAction.RETRY_WITH_DELAY
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
                    Log.d(TAG, "Executing operation, attempt ${attempt + 1}/$maxAttempts")
                    return operation()
                } catch (e: Exception) {
                    lastException = e
                    val authError = handleAuthError(e)

                    Log.w(TAG, "Operation failed on attempt ${attempt + 1}: ${e.message}")


                    if (!isRecoverableError(authError) || attempt == maxAttempts - 1) {
                        Log.e(TAG, "Non-recoverable error or max attempts reached", e)
                        throw e
                    }


                    Log.d(TAG, "Retrying in ${currentDelay}ms...")
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay *= 2
                }
            }

            throw lastException ?: Exception("Operation failed after $maxAttempts attempts")
        }



        private fun logAuthenticationError(error: Throwable) {
            Log.e(TAG, "Authentication error occurred", error)


            when {
                error.message?.contains("email not confirmed", ignoreCase = true) == true -> {
                    Log.i(TAG, "Email verification required - user needs to check email")
                }
                error.message?.contains("invalid", ignoreCase = true) == true -> {
                    Log.i(TAG, "Invalid credentials provided - user should check email/password")
                }
                error is java.net.UnknownHostException -> {
                    Log.w(TAG, "Network connectivity issue - DNS resolution failed")
                }
                error is java.net.SocketTimeoutException -> {
                    Log.w(TAG, "Network timeout - slow connection or server issues")
                }
                error is java.io.IOException -> {
                    Log.w(TAG, "IO error during authentication - network or server issue")
                }
            }
        }



        fun logVerificationAttempt(email: String, success: Boolean, errorMessage: String? = null) {
            if (success) {
                Log.i(TAG, "Email verification successful for: $email")
            } else {
                Log.w(TAG, "Email verification failed for: $email, error: $errorMessage")
            }
        }



        fun logResendVerificationAttempt(email: String, success: Boolean, errorMessage: String? = null) {
            if (success) {
                Log.i(TAG, "Resend verification email successful for: $email")
            } else {
                Log.w(TAG, "Resend verification email failed for: $email, error: $errorMessage")
            }
        }
    }
}



enum class RecoveryAction {
    RESEND_VERIFICATION,
    RETRY_WITH_CORRECT_CREDENTIALS,
    RETRY_WITH_DELAY,
    CHECK_CONFIGURATION
}
