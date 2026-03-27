package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.domain.model.AuthError

object AuthErrorMapper {
    
    fun mapException(exception: Throwable): AuthError {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            // User already exists / duplicate email
            message.contains("user already registered") ||
            message.contains("email already exists") ||
            message.contains("duplicate") && message.contains("email") ||
            message.contains("user_email_key") ||
            message.contains("already in use") -> {
                AuthError.UserCollision("This email is already registered")
            }
            
            // Weak password
            message.contains("password") && (
                message.contains("weak") ||
                message.contains("too short") ||
                message.contains("at least") ||
                message.contains("minimum")
            ) -> {
                AuthError.WeakPassword("Password must be at least 8 characters")
            }
            
            // Invalid credentials
            message.contains("invalid login credentials") ||
            message.contains("invalid email or password") ||
            message.contains("email not confirmed") -> {
                AuthError.InvalidCredentials("Invalid email or password")
            }
            
            // Network errors
            message.contains("network") ||
            message.contains("connection") ||
            message.contains("timeout") ||
            message.contains("unable to resolve host") ||
            message.contains("failed to connect") -> {
                AuthError.NetworkError("Network connection failed. Please check your internet")
            }
            
            // Validation errors
            message.contains("invalid email") ||
            message.contains("email format") -> {
                AuthError.ValidationFailed("Please enter a valid email address")
            }
            
            // Database/table errors
            message.contains("relation") && message.contains("does not exist") -> {
                AuthError.Unknown("Database configuration error. Please contact support")
            }
            
            // Default unknown error
            else -> {
                AuthError.Unknown(exception.message ?: "An unexpected error occurred")
            }
        }
    }
}
