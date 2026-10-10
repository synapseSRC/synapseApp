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
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder



class SupabaseAuthenticationService : com.synapse.social.studioasinc.data.remote.services.interfaces.IAuthenticationService {

    private val context: Context
    private val client = SupabaseClient.client
    private var _authConfig: AuthConfig? = null
    private var authConfig: AuthConfig
        get() {
            if (_authConfig == null) {
                _authConfig = AuthConfig.create(this.context)
            }
            return _authConfig!!
        }
        set(value) {
            _authConfig = value
        }



    constructor(context: Context) {
        this.context = context.applicationContext

    }



    constructor() {
        val instance = INSTANCE ?: throw IllegalStateException(
            "SupabaseAuthenticationService not initialized. Use constructor with context or call initialize(context) first."
        )
        this.context = instance.context

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
                if (userId != null) {
                    try {
                        OneSignal.login(userId)
                        debugLog("✅ OneSignal login successful with user ID: $userId")
                        android.util.Log.d("OneSignal", "✅ Logged in with user: $userId")
                        
                        // Sync subscription ID if available
                        val subId = OneSignal.User.pushSubscription.id
                        if (subId != null) {
                            try {
                                client.postgrest["users"].update(
                                    mapOf("one_signal_player_id" to subId)
                                ) {
                                    filter { eq("uid", userId) }
                                }
                                debugLog("✅ Synced OneSignal subscription ID: $subId")
                            } catch (e: Exception) {
                                debugLog("❌ Failed to sync OneSignal subscription ID: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        debugLog("❌ OneSignal login failed: ${e.message}")
                        android.util.Log.e("OneSignal", "❌ Login failed", e)
                    }
                } else {
                    debugLog("❌ Cannot login to OneSignal: userId is null")
                    android.util.Log.e("OneSignal", "❌ Cannot login: userId is null")
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

                    try {
                        OneSignal.login(user.id)
                        debugLog("✅ OneSignal login successful with user ID: ${user.id}")
                        android.util.Log.d("OneSignal", "✅ Logged in with user: ${user.id}")
                        
                        // Sync subscription ID if available
                        val subId = OneSignal.User.pushSubscription.id
                        if (subId != null) {
                            try {
                                client.postgrest["users"].update(
                                    mapOf("one_signal_player_id" to subId)
                                ) {
                                    filter { eq("uid", user.id) }
                                }
                                debugLog("✅ Synced OneSignal subscription ID: $subId")
                            } catch (e: Exception) {
                                debugLog("❌ Failed to sync OneSignal subscription ID: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        debugLog("❌ OneSignal login failed: ${e.message}")
                        android.util.Log.e("OneSignal", "❌ Login failed", e)
                    }

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

