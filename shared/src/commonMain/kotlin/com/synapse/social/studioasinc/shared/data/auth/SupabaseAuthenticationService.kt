package com.synapse.social.studioasinc.shared.data.auth

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.local.SecureStorage
import com.synapse.social.studioasinc.shared.data.model.UserProfileInsert
import com.synapse.social.studioasinc.shared.data.model.UserSettingsInsert
import com.synapse.social.studioasinc.shared.data.model.UserPresenceInsert
import io.github.aakira.napier.Napier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.time.ExperimentalTime
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments

class SupabaseAuthenticationService(
    private val client: SupabaseClientLib = SupabaseClient.client,
    private val secureStorage: SecureStorage? = null
) : IAuthenticationService {

    private val KEY_ACCESS_TOKEN = "auth_access_token"
    private val KEY_REFRESH_TOKEN = "auth_refresh_token"
    private val KEY_USER_ID = "auth_user_id"
    private val KEY_USER_EMAIL = "auth_user_email"
    private val KEY_EXPIRES_IN = "auth_expires_in"

    private val allowedRedirectUrls = setOf(
        "synapse://login",
        "https://synapseofficial.vercel.app",
        "https://www.synapse.com/auth"
    )

    override suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val session = client.auth.currentSessionOrNull()
                if (session != null) {
                    storeSessionTokens(
                        accessToken = session.accessToken,
                        refreshToken = session.refreshToken,
                        userId = session.user?.id ?: "",
                        userEmail = session.user?.email ?: "",
                        expiresIn = session.expiresIn.toInt()
                    )
                }

                val userId = client.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(IllegalStateException("User ID not found after signup"))

                Napier.d("User signed up successfully: $userId")
                Result.success(userId)
            }
        } catch (e: Exception) {
            Napier.e("Sign up failed", e)
            Result.failure(e)
        }
    }

    override suspend fun createUserProfile(userId: String, email: String, username: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                val profileInsert = UserProfileInsert(
                    uid = userId,
                    username = username
                )
                client.from("users").insert(profileInsert)

                val settingsInsert = UserSettingsInsert(user_id = userId)
                client.from("user_settings").insert(settingsInsert)

                val presenceInsert = UserPresenceInsert(user_id = userId)
                client.from("user_presence").insert(presenceInsert)

                Napier.d("User profile created successfully: $userId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Create user profile failed", e)
            Result.failure(e)
        }
    }

    override suspend fun ensureProfileExists(userId: String, email: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                val count = client.from("users").select(columns = Columns.list("id")) {
                    count(Count.EXACT)
                    filter {
                        eq("id", userId)
                    }
                }.countOrNull()

                if (count == null || count == 0L) {
                    createUserProfile(userId, email, email.substringBefore("@"))
                } else {
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Napier.e("Ensure profile exists failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val session = client.auth.currentSessionOrNull()
                if (session != null) {
                    storeSessionTokens(
                        accessToken = session.accessToken,
                        refreshToken = session.refreshToken,
                        userId = session.user?.id ?: "",
                        userEmail = session.user?.email ?: "",
                        expiresIn = session.expiresIn.toInt()
                    )
                }

                val userId = client.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.failure(IllegalStateException("User ID not found after sign in"))

                Napier.d("User signed in successfully: $userId")
                Result.success(userId)
            }
        } catch (e: Exception) {
            Napier.e("Sign in failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.signOut()
                clearStoredTokens()
                Napier.d("User signed out successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Sign out failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return try {
            client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Napier.e("Failed to get current user ID", e)
            null
        }
    }

    override suspend fun getCurrentUserEmail(): String? {
        return try {
            client.auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            Napier.e("Failed to get current user email", e)
            null
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun isEmailVerified(): Boolean {
        return try {
            val user = client.auth.currentUserOrNull()
            user?.identities?.any { it.provider == "email" && it.identityData["email_verified"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull == "true" } == true
        } catch (e: Exception) {
            Napier.e("Failed to check email verification", e)
            false
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun refreshSession(): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.refreshCurrentSession()

                val session = client.auth.currentSessionOrNull()
                if (session != null) {
                    storeSessionTokens(
                        accessToken = session.accessToken,
                        refreshToken = session.refreshToken,
                        userId = session.user?.id ?: "",
                        userEmail = session.user?.email ?: "",
                        expiresIn = session.expiresIn.toInt()
                    )
                }

                Napier.d("Session refreshed successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Session refresh failed", e)
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun restoreSession(): Boolean {
        return try {
            withContext(Dispatchers.Default) {
                var session = client.auth.currentSessionOrNull()

                if (session == null && secureStorage != null) {
                    val accessToken = secureStorage.getString(KEY_ACCESS_TOKEN)
                    val refreshToken = secureStorage.getString(KEY_REFRESH_TOKEN)

                    if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
                        try {
                            client.auth.importAuthToken(accessToken, refreshToken)
                            session = client.auth.currentSessionOrNull()
                            Napier.d("Session restored from storage")
                        } catch (e: Exception) {
                            Napier.e("Failed to restore session from storage", e)
                        }
                    }
                }

                session != null
            }
        } catch (e: Exception) {
            Napier.e("Session restore failed", e)
            false
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.resetPasswordForEmail(email)
                Napier.d("Password reset email sent successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Password reset email failed", e)
            Result.failure(e)
        }
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.resendEmail(OtpType.Email.SIGNUP, email)
                Napier.d("Verification email resent successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Resend verification email failed", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePassword(password: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.updateUser {
                    this.password = password
                }
                Napier.d("Password updated successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Password update failed", e)
            Result.failure(e)
        }
    }

    override suspend fun updateEmail(email: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.updateUser {
                    this.email = email
                }
                Napier.d("Email updated successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Email update failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> {
        return try {
            if (!isValidRedirectUrl(redirectUrl)) {
                return Result.failure(IllegalArgumentException("Invalid redirect URL: $redirectUrl"))
            }
            val oauthUrl = URLBuilder(client.supabaseUrl).apply {
                appendPathSegments("auth", "v1", "authorize")
                parameters.append("provider", provider)
                parameters.append("redirect_to", redirectUrl)
            }.buildString()
            Result.success(oauthUrl)
        } catch (e: Exception) {
            Napier.e("Failed to generate OAuth URL", e)
            Result.failure(e)
        }
    }

    private fun isValidRedirectUrl(url: String): Boolean {
        return allowedRedirectUrls.any { allowed ->
            url == allowed || url.startsWith("$allowed/") || url.startsWith("$allowed?")
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                when {
                    code != null -> {
                        client.auth.exchangeCodeForSession(code)
                    }
                    accessToken != null && refreshToken != null -> {
                        client.auth.importAuthToken(accessToken, refreshToken)
                    }
                    else -> {
                        throw Exception("No valid OAuth parameters provided")
                    }
                }

                val session = client.auth.currentSessionOrNull()
                if (session != null) {
                    storeSessionTokens(
                        accessToken = session.accessToken,
                        refreshToken = session.refreshToken,
                        userId = session.user?.id ?: "",
                        userEmail = session.user?.email ?: "",
                        expiresIn = session.expiresIn.toInt()
                    )
                }

                Napier.d("OAuth callback handled successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("OAuth callback handling failed", e)
            Result.failure(e)
        }
    }

    override suspend fun storeSessionTokens(accessToken: String, refreshToken: String, userId: String, userEmail: String, expiresIn: Int) {
        secureStorage?.let { storage ->
            storage.save(KEY_ACCESS_TOKEN, accessToken)
            storage.save(KEY_REFRESH_TOKEN, refreshToken)
            storage.save(KEY_USER_ID, userId)
            storage.save(KEY_USER_EMAIL, userEmail)
            storage.save(KEY_EXPIRES_IN, expiresIn.toString())
            Napier.d("Session tokens stored for user: $userId")
        } ?: run {
            Napier.w("SecureStorage not available. Session tokens not persisted.")
        }
    }

    override suspend fun clearStoredTokens() {
        secureStorage?.let { storage ->
            storage.clear(KEY_ACCESS_TOKEN)
            storage.clear(KEY_REFRESH_TOKEN)
            storage.clear(KEY_USER_ID)
            storage.clear(KEY_USER_EMAIL)
            storage.clear(KEY_EXPIRES_IN)
            Napier.d("Stored tokens cleared")
        } ?: run {
            Napier.w("SecureStorage not available. Cannot clear stored tokens.")
        }
    }
}
