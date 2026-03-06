package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.onesignal.OneSignal
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import android.util.Log
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.IOException



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



class SupabaseAuthenticationService : com.synapse.social.studioasinc.data.remote.services.interfaces.IAuthenticationService {

    private val context: Context
    private val client = SupabaseClient.client
    private var authConfig: AuthConfig



    constructor(context: Context) {
        this.context = context.applicationContext
        this.authConfig = AuthConfig.create(this.context)
    }



    constructor() {
        val instance = INSTANCE ?: throw IllegalStateException(
            "SupabaseAuthenticationService not initialized. Use constructor with context or call initialize(context) first."
        )
        this.context = instance.context
        this.authConfig = instance.authConfig
    }

    companion object {
        private const val TAG = "SupabaseAuth"

        @Volatile
        private var INSTANCE: SupabaseAuthenticationService? = null



        @JvmStatic
        fun getInstance(context: Context): SupabaseAuthenticationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseAuthenticationService(context.applicationContext).also { INSTANCE = it }
            }
        }



        @JvmStatic
        fun create(context: Context): SupabaseAuthenticationService {
            return SupabaseAuthenticationService(context)
        }



        @JvmStatic
        fun createForDevelopment(context: Context): SupabaseAuthenticationService {
            val service = SupabaseAuthenticationService(context)
            service.enableDevelopmentMode()
            return service
        }



        @JvmStatic
        fun createForProduction(context: Context): SupabaseAuthenticationService {
            val service = SupabaseAuthenticationService(context)
            service.disableDevelopmentMode()
            return service
        }



        @JvmStatic
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = SupabaseAuthenticationService(context.applicationContext)
                    }
                }
            }
        }



        @JvmStatic
        fun getInstance(): SupabaseAuthenticationService {
            return INSTANCE ?: throw IllegalStateException(
                "SupabaseAuthenticationService not initialized. Call initialize(context) first or use getInstance(context)."
            )
        }
    }



    fun updateConfig(newConfig: AuthConfig) {
        authConfig = newConfig
        AuthConfig.save(context, newConfig)
        debugLog("Authentication configuration updated: $newConfig")
    }



    fun getConfig(): AuthConfig = authConfig



    fun enableDevelopmentMode() {
        authConfig = AuthConfig.enableDevelopmentMode(context)
        debugLog("Development mode enabled - email verification bypassed")
    }



    fun disableDevelopmentMode() {
        authConfig = AuthConfig.disableDevelopmentMode(context)
        debugLog("Development mode disabled - email verification required")
    }



    private fun debugLog(message: String, throwable: Throwable? = null) {
        if (authConfig.isDebugLoggingEnabled()) {
            if (throwable != null) {
                Log.d(TAG, message, throwable)
            } else {
                Log.d(TAG, message)
            }
        }
    }



    private fun logAuthenticationStep(step: String, email: String? = null, success: Boolean? = null) {
        if (authConfig.isDebugLoggingEnabled()) {
            val emailPart = email?.let { " for email: $it" } ?: ""
            val successPart = success?.let { " - ${if (it) "SUCCESS" else "FAILED"}" } ?: ""
            debugLog("Auth Step: $step$emailPart$successPart")
        }
    }

    private suspend fun clearExistingSession(action: String) {
        try {
            client.auth.signOut()
            debugLog("Cleared existing session before $action")
        } catch (e: Exception) {
            debugLog("No existing session to clear", e)
        }
    }

    private fun createLocalUser(email: String, supabaseUser: io.github.jan.supabase.auth.user.UserInfo?): User {
        return if (supabaseUser != null && supabaseUser.id.isNotEmpty()) {
            User(
                id = supabaseUser.id,
                email = supabaseUser.email ?: email,
                emailConfirmed = supabaseUser.emailConfirmedAt != null,
                createdAt = supabaseUser.createdAt?.toString()
            )
        } else {
            User(
                id = "pending",
                email = email,
                emailConfirmed = false,
                createdAt = System.currentTimeMillis().toString()
            )
        }
    }

    private suspend fun handleAuthFailure(email: String, step: String, e: Exception): Result<AuthResult> {
        logAuthenticationStep("$step failed with exception", email, false)
        debugLog("$step failed", e)

        try {
            client.auth.signOut()
        } catch (signOutError: Exception) {
            debugLog("Failed to clear session after error", signOutError)
        }

        val authError = AuthErrorHandler.handleAuthError(e)
        val errorMessage = AuthErrorHandler.getErrorMessage(authError)
        return Result.failure(Exception(errorMessage))
    }



    override suspend fun signUp(email: String, password: String): Result<AuthResult> {
        return withContext(Dispatchers.IO) {
            try {
                logAuthenticationStep("Starting sign up", email)

                if (!SupabaseClient.isConfigured()) {
                    logAuthenticationStep("Sign up failed - Supabase not configured", email, false)
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }

                clearExistingSession("sign up")

                logAuthenticationStep("Attempting Supabase sign up", email)
                val createdUser = AuthErrorHandler.executeWithRetry(
                    maxAttempts = authConfig.getEffectiveRetryAttempts(),
                    initialDelay = authConfig.getEffectiveRetryDelay()
                ) {
                    client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
                debugLog("Sign up request completed successfully")

                val userId = createdUser?.id ?: client.auth.currentUserOrNull()?.id
                userId?.let {
                    OneSignal.login(it)
                    debugLog("Linked OneSignal with External User ID: $it")
                }

                val supabaseUser = createdUser ?: client.auth.currentUserOrNull()
                val user = createLocalUser(email, supabaseUser)
                debugLog("User created successfully: ${user.id}")

                val needsVerification = if (authConfig.shouldBypassEmailVerification()) {
                    debugLog("Email verification bypassed in development mode")
                    false
                } else {
                    supabaseUser?.emailConfirmedAt == null
                }

                AuthErrorHandler.logVerificationAttempt(email, !needsVerification)
                logAuthenticationStep("Sign up completed successfully", email, true)

                val message = when {
                    needsVerification -> "Please check your email and click the verification link to activate your account."
                    authConfig.shouldBypassEmailVerification() -> "Account created successfully. Email verification bypassed in development mode."
                    else -> "Account created successfully!"
                }

                Result.success(AuthResult(
                    user = user,
                    needsEmailVerification = needsVerification,
                    message = message
                ))
            } catch (e: Exception) {
                handleAuthFailure(email, "Sign up", e)
            }
        }
    }



    override suspend fun signIn(email: String, password: String): Result<AuthResult> {
        return withContext(Dispatchers.IO) {
            try {
                logAuthenticationStep("Starting sign in", email)

                if (!SupabaseClient.isConfigured()) {
                    logAuthenticationStep("Sign in failed - Supabase not configured", email, false)
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }

                clearExistingSession("sign in")

                logAuthenticationStep("Attempting Supabase sign in", email)
                AuthErrorHandler.executeWithRetry(
                    maxAttempts = authConfig.getEffectiveRetryAttempts(),
                    initialDelay = authConfig.getEffectiveRetryDelay()
                ) {
                    client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }

                val supabaseUser = client.auth.currentUserOrNull()
                if (supabaseUser != null && supabaseUser.id.isNotEmpty()) {
                    val user = createLocalUser(email, supabaseUser)
                    debugLog("User authenticated successfully: ${user.id}")

                    OneSignal.login(user.id)
                    debugLog("Linked OneSignal with External User ID: ${user.id}")

                    val emailVerified = supabaseUser.emailConfirmedAt != null || authConfig.shouldBypassEmailVerification()

                    if (!emailVerified && !authConfig.shouldBypassEmailVerification()) {
                        AuthErrorHandler.logVerificationAttempt(email, false, "Email not verified")
                        logAuthenticationStep("Sign in requires email verification", email, false)

                        Result.success(AuthResult(
                            user = user,
                            needsEmailVerification = true,
                            message = "Please verify your email address to continue"
                        ))
                    } else {
                        AuthErrorHandler.logVerificationAttempt(email, true)
                        logAuthenticationStep("Sign in completed successfully", email, true)

                        val message = if (authConfig.shouldBypassEmailVerification() && supabaseUser.emailConfirmedAt == null) {
                            "Signed in successfully. Email verification bypassed in development mode."
                        } else {
                            null
                        }

                        Result.success(AuthResult(
                            user = user,
                            needsEmailVerification = false,
                            message = message
                        ))
                    }
                } else {
                    logAuthenticationStep("Sign in failed - invalid credentials", email, false)
                    Result.failure(Exception("Authentication failed - invalid credentials"))
                }
            } catch (e: Exception) {
                handleAuthFailure(email, "Sign in", e)
            }
        }
    }



    override suspend fun signOut(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {

                OneSignal.logout()
                debugLog("Logged out from OneSignal")

                client.auth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logAuthenticationStep("Starting resend verification email", email)


                if (authConfig.shouldBypassEmailVerification()) {
                    debugLog("Resend verification email bypassed in development mode")
                    logAuthenticationStep("Resend verification email bypassed (dev mode)", email, true)
                    return@withContext Result.success(Unit)
                }


                if (!SupabaseClient.isConfigured()) {
                    logAuthenticationStep("Resend verification failed - Supabase not configured", email, false)
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }


                AuthErrorHandler.executeWithRetry(
                    maxAttempts = authConfig.getEffectiveRetryAttempts(),
                    initialDelay = authConfig.getEffectiveRetryDelay()
                ) {
                    logAuthenticationStep("Calling Supabase resend", email)
                    client.auth.resendEmail(OtpType.Email.SIGNUP, email)
                    Unit
                }


                AuthErrorHandler.logResendVerificationAttempt(email, true)
                logAuthenticationStep("Resend verification email completed", email, true)

                Result.success(Unit)
            } catch (e: Exception) {
                logAuthenticationStep("Resend verification email failed", email, false)
                debugLog("Failed to resend verification email", e)


                AuthErrorHandler.logResendVerificationAttempt(email, false, e.message)

                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }



    override suspend fun checkEmailVerified(email: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logAuthenticationStep("Checking email verification status", email)


                if (authConfig.shouldBypassEmailVerification()) {
                    debugLog("Email verification check bypassed in development mode - returning true")
                    logAuthenticationStep("Email verification check bypassed (dev mode)", email, true)
                    return@withContext Result.success(true)
                }


                if (!SupabaseClient.isConfigured()) {
                    logAuthenticationStep("Email verification check failed - Supabase not configured", email, false)
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }


                val isVerified = AuthErrorHandler.executeWithRetry(
                    maxAttempts = authConfig.getEffectiveRetryAttempts(),
                    initialDelay = authConfig.getEffectiveRetryDelay()
                ) {
                    val user = client.auth.currentUserOrNull()
                    if (user != null && user.email == email) {
                        val verified = user.emailConfirmedAt != null
                        debugLog("Email verification status for $email: $verified")
                        verified
                    } else {
                        debugLog("No current user or email mismatch for verification check")
                        false
                    }
                }


                AuthErrorHandler.logVerificationAttempt(email, isVerified)
                logAuthenticationStep("Email verification check completed", email, isVerified)

                Result.success(isVerified)
            } catch (e: Exception) {
                logAuthenticationStep("Email verification check failed", email, false)
                debugLog("Failed to check email verification status", e)


                AuthErrorHandler.logVerificationAttempt(email, false, e.message)

                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }



    override fun getCurrentUser(): User? {

        if (!SupabaseClient.isConfigured()) {
            return null
        }

        val user = client.auth.currentUserOrNull()
        return if (user != null && user.id.isNotEmpty()) {
            User(
                id = user.id,
                email = user.email ?: "",
                emailConfirmed = user.emailConfirmedAt != null,
                createdAt = user.createdAt?.toString()
            )
        } else {
            null
        }
    }



    override fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }



    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.updateUser {
                    password = newPassword
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    override suspend fun updateEmail(newEmail: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.updateUser {
                    email = newEmail
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}



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
