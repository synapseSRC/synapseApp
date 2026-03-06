package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.domain.model.AuthError
import io.github.jan.supabase.exceptions.RestException

object AuthErrorMapper {
    
    fun mapException(exception: Throwable): AuthError {
        val originalMessage = exception.message

        val messageToCheck = if (exception is RestException) {
            // Supabase API errors often contain details in description or error fields instead of just the generic message
            (exception.description ?: exception.error ?: exception.message ?: "").lowercase()
        } else {
            exception.message?.lowercase() ?: ""
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
            messageToCheck.contains("failed to connect") -> {
                AuthError.NetworkError("Network connection failed. Please check your internet")
            }
            
            // Validation errors
            messageToCheck.contains("invalid email") ||
            messageToCheck.contains("email format") -> {
                AuthError.ValidationFailed("Please enter a valid email address")
            }
            
            // Database/table errors
            messageToCheck.contains("relation") && messageToCheck.contains("does not exist") -> {
                AuthError.Unknown("Database configuration error. Please contact support")
            }
            
            // Default unknown error
            else -> {
                if (exception is RestException) {
                    val fallbackMsg = exception.description ?: exception.error ?: originalMessage ?: "An unexpected error occurred"
                    AuthError.Unknown(fallbackMsg)
                } else {
                    AuthError.Unknown(originalMessage ?: "An unexpected error occurred")
                }
            }
        }
    }
}
