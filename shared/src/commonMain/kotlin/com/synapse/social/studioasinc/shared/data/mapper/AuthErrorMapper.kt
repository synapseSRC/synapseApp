package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.domain.model.AuthError
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.UnknownRestException

object AuthErrorMapper {

    private const val GENERIC_UNKNOWN_ERROR = "unknown error"
    private const val FALLBACK_AUTH_ERROR = "An unexpected error occurred"
    private const val FALLBACK_SIGN_UP_SERVER_ERROR = "Could not create your account right now. Please try again in a moment"
    
    fun mapException(exception: Throwable): AuthError {
        val originalMessage = exception.message

        val messageToCheck = if (exception is RestException) {
            // Supabase API errors often contain details in description or error fields instead of just the generic message
            listOfNotNull(
                exception.description,
                exception.error,
                exception.message,
                exception.cause?.message
            ).joinToString(" ").lowercase()
        } else {
            collectCauseMessages(exception).lowercase()
        }
        
        return when {
            // User already exists / duplicate email
            messageToCheck.contains("user already registered") ||
            messageToCheck.contains("email already exists") ||
            messageToCheck.contains("duplicate") && messageToCheck.contains("email") ||
            messageToCheck.contains("user_email_key") ||
            messageToCheck.contains("already in use") -> {
                AuthError.UserCollision("This email is already registered")
            }

            // Username already exists / unique constraint violation
            messageToCheck.contains("duplicate") && messageToCheck.contains("username") ||
            messageToCheck.contains("users_username_key") -> {
                AuthError.UserCollision("This username is already taken")
            }
            
            // Weak password
            messageToCheck.contains("password") && (
                messageToCheck.contains("weak") ||
                messageToCheck.contains("too short") ||
                messageToCheck.contains("at least") ||
                messageToCheck.contains("minimum")
            ) -> {
                AuthError.WeakPassword("Password must be at least 8 characters")
            }
            
            // Invalid credentials
            messageToCheck.contains("invalid login credentials") ||
            messageToCheck.contains("invalid email or password") ||
            messageToCheck.contains("email not confirmed") -> {
                AuthError.InvalidCredentials("Invalid email or password")
            }
            
            // Network errors
            messageToCheck.contains("network") ||
            messageToCheck.contains("connection") ||
            messageToCheck.contains("timeout") ||
            messageToCheck.contains("unable to resolve host") ||
            messageToCheck.contains("failed to connect") ||
            exception is HttpRequestException -> {
                AuthError.NetworkError("Network connection failed. Please check your internet")
            }
            
            // Validation errors
            messageToCheck.contains("invalid email") ||
            messageToCheck.contains("email format") -> {
                AuthError.ValidationFailed("Please enter a valid email address")
            }

            // Sign-up disabled / backend blocked registration
            messageToCheck.contains("signup") && (
                messageToCheck.contains("disabled") ||
                messageToCheck.contains("not allowed") ||
                messageToCheck.contains("forbidden")
            ) -> {
                AuthError.Unknown("New account registration is currently unavailable. Please try again later")
            }

            // Supabase internal failure during signup
            messageToCheck.contains("database error saving new user") ||
            messageToCheck.contains("unexpected_failure") ||
            messageToCheck.contains("500") -> {
                AuthError.Unknown(FALLBACK_SIGN_UP_SERVER_ERROR)
            }
            
            // Database/table errors
            messageToCheck.contains("relation") && messageToCheck.contains("does not exist") -> {
                AuthError.Unknown("Database configuration error. Please contact support")
            }
            
            // Default unknown error
            else -> {
                if (exception is RestException) {
                    val fallbackMsg = listOfNotNull(
                        exception.description,
                        exception.error,
                        originalMessage,
                        exception.cause?.message
                    ).firstOrNull { !it.isNullOrBlank() && !isGenericUnknownError(it) }

                    AuthError.Unknown(fallbackMsg ?: FALLBACK_SIGN_UP_SERVER_ERROR)
                } else if (exception is UnknownRestException) {
                    AuthError.Unknown(FALLBACK_SIGN_UP_SERVER_ERROR)
                } else {
                    val fallbackMessage = listOfNotNull(
                        originalMessage,
                        exception.cause?.message
                    ).firstOrNull { !it.isNullOrBlank() && !isGenericUnknownError(it) }

                    AuthError.Unknown(fallbackMessage ?: FALLBACK_AUTH_ERROR)
                }
            }
        }
    }

    private fun collectCauseMessages(exception: Throwable): String {
        val messages = mutableListOf<String>()
        var current: Throwable? = exception
        while (current != null) {
            current.message?.takeIf { it.isNotBlank() }?.let(messages::add)
            current = current.cause
        }
        return messages.joinToString(" ")
    }

    private fun isGenericUnknownError(message: String): Boolean {
        return message.trim().equals(GENERIC_UNKNOWN_ERROR, ignoreCase = true)
    }
}
