package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.model.UserProfileInsert
import com.synapse.social.studioasinc.shared.data.model.UserSettingsInsert
import com.synapse.social.studioasinc.shared.data.model.UserPresenceInsert
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Discord
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.Twitter
import io.github.jan.supabase.auth.providers.Facebook
import io.github.jan.supabase.auth.providers.Spotify
import io.github.jan.supabase.auth.providers.Slack
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import io.github.aakira.napier.Napier
import io.github.jan.supabase.functions.functions
import kotlin.time.ExperimentalTime
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments

class SupabaseAuthRepository(private val client: SupabaseClientLib = SupabaseClient.client) : AuthRepository {
    override val sessionStatus: Flow<AuthSessionStatus> get() = client.auth.sessionStatus.map {
        when (it) {
            is SessionStatus.Authenticated -> AuthSessionStatus.AUTHENTICATED
            is SessionStatus.NotAuthenticated -> AuthSessionStatus.NOT_AUTHENTICATED
            is SessionStatus.Initializing -> AuthSessionStatus.INITIALIZING
            else -> AuthSessionStatus.NOT_AUTHENTICATED // Default/Fallback
        }
    }
    private val TAG = "AuthRepository"

    override suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("User ID not found")
                Napier.d("User signed up: $userId", tag = TAG)
                Result.success(userId)
            }
        } catch (e: Exception) {
            logSafeError("Sign up failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> {
        return try {
            val signUpResult = signUp(email, password)
            if (signUpResult.isSuccess) {
                val userId = signUpResult.getOrThrow()
                ensureProfileExists(userId, email, username).map { userId }
            } else {
                signUpResult
            }
        } catch (e: Exception) {
            logSafeError("Sign up with profile failed", e)
            Result.failure(e)
        }
    }

    override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                Napier.d("Checking if user profile exists for userId: $userId", tag = TAG)
                val count = client.from("users").select(columns = Columns.list("id")) {
                    count(Count.EXACT)
                    filter {
                        eq("id", userId)
                    }
                }.countOrNull()
                Napier.d("User profile count for $userId: $count", tag = TAG)

                if (count == null || count == 0L) {
                    Napier.d("Profile does not exist for $userId, creating new profile...", tag = TAG)
                    val actualUsername = username ?: email.substringBefore("@")

                    // SECURITY: Do not include sensitive fields (account_premium, verify, banned) here. They must be handled server-side.
                    val profileInsert = UserProfileInsert(
                        uid = userId, // Ensure ID is passed if model requires it
                        username = actualUsername
                    )
                    // Note: Check table name. Previous code said "user_profiles", but supabase tables showed "users".
                    // The app/UserRepository used "users". shared used "user_profiles".
                    // I see "users" table in Supabase list_tables output earlier.
                    // I will change it to "users" to match the actual DB.
                    Napier.d("Inserting user profile for $userId into users table...", tag = TAG)
                    client.from("users").insert(profileInsert)
                    Napier.d("Successfully inserted user profile for $userId.", tag = TAG)

                    // Also check if user_settings and user_presence exist in list_tables
                    // Yes: user_settings, user_presence.
                    val settingsInsert = UserSettingsInsert(user_id = userId)
                    Napier.d("Inserting user settings for $userId...", tag = TAG)
                    client.from("user_settings").insert(settingsInsert)
                    Napier.d("Successfully inserted user settings for $userId.", tag = TAG)

                    val presenceInsert = UserPresenceInsert(user_id = userId)
                    Napier.d("Inserting user presence for $userId...", tag = TAG)
                    client.from("user_presence").insert(presenceInsert)
                    Napier.d("Successfully inserted user presence for $userId.", tag = TAG)

                    Napier.d("User profile created: $userId", tag = TAG)
                } else {
                    Napier.d("Profile already exists for $userId.", tag = TAG)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Ensure profile exists failed", e)
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
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("User ID not found")
                Napier.d("User signed in: $userId", tag = TAG)
                Result.success(userId)
            }
        } catch (e: Exception) {
            logSafeError("Sign in failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.signOut()
                Napier.d("User signed out", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Sign out failed", e)
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? {
        return try {
            client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            logSafeError("Failed to get current user ID", e)
            null
        }
    }

    override fun getCurrentUserEmail(): String? {
        return try {
            client.auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            logSafeError("Failed to get current user email", e)
            null
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun isEmailVerified(): Boolean {
        return try {
            val user = client.auth.currentUserOrNull()
            user?.identities?.any { it.provider == "email" && it.identityData["email_verified"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull == "true" } == true
        } catch (e: Exception) {
            logSafeError("Failed to check email verification", e)
            false
        }
    }

    override suspend fun refreshSession(): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.refreshCurrentSession()
                Napier.d("Session refreshed", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Session refresh failed", e)
            Result.failure(e)
        }
    }

    override fun restoreSession(): Boolean {
        return try {
            client.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            logSafeError("Session restore failed", e)
            false
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.resetPasswordForEmail(email)
                Napier.d("Password reset email sent", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Password reset failed", e)
            Result.failure(e)
        }
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.resendEmail(OtpType.Email.SIGNUP, email)
                Napier.d("Verification email resent", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Resend verification email failed", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePassword(password: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.updateUser {
                    this.password = password
                }
                Napier.d("Password updated", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Password update failed", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePhoneNumber(phone: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.updateUser {
                    this.phone = phone
                }
                Napier.d("Phone number updated", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Phone number update failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> {
        return try {
            val oauthUrl = URLBuilder(client.supabaseUrl).apply {
                appendPathSegments("auth", "v1", "authorize")
                parameters.append("provider", provider)
                parameters.append("redirect_to", redirectUrl)
            }.buildString()
            Result.success(oauthUrl)
        } catch (e: Exception) {
            logSafeError("OAuth URL generation failed", e)
            Result.failure(e)
        }
    }

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
                        throw Exception("No valid OAuth parameters")
                    }
                }
                Napier.d("OAuth callback handled", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("OAuth callback failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signInWithOAuth(provider: SocialProvider, redirectUrl: String): Result<Unit> {
        val supabaseProvider: OAuthProvider = mapSocialProviderToOAuthProvider(provider)
        return try {
            withContext(Dispatchers.Default) {
                client.auth.signInWith(supabaseProvider, redirectUrl)
                Napier.d("OAuth sign-in initiated for ${supabaseProvider.name} with redirect URI: $redirectUrl", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("OAuth sign-in failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> {
        return try {
            withContext(Dispatchers.Default) {
                // Use the ID token to sign in with Google
                client.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    this.provider = Google
                }
                
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("User ID not found after Google sign-in")
                val email = client.auth.currentUserOrNull()?.email
                    ?: throw Exception("Email not found after Google sign-in")
                
                ensureProfileExists(userId, email, null)
                
                Napier.d("Google ID token sign-in successful: $userId", tag = TAG)
                Result.success(userId)
            }
        } catch (e: Exception) {
            logSafeError("Google ID token sign-in failed", e)
            Result.failure(e)
        }
    }

    override suspend fun linkIdentity(provider: SocialProvider): Result<Unit> {
        val supabaseProvider: OAuthProvider = mapSocialProviderToOAuthProvider(provider)
        return try {
            withContext(Dispatchers.Default) {
                client.auth.linkIdentity(supabaseProvider)
                Napier.d("Link identity initiated for ${supabaseProvider.name}", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Link identity failed", e)
            Result.failure(e)
        }
    }

    override suspend fun unlinkIdentity(identityId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.unlinkIdentity(identityId)
                Napier.d("Unlinked identity: $identityId", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Unlink identity failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getLinkedIdentities(): Result<List<String>> {
        return try {
            withContext(Dispatchers.Default) {
                val identities = client.from("user_identities")
                    .select()
                    .decodeList<JsonObject>()

                val providers = identities.mapNotNull {
                    it["provider"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                }
                Result.success(providers)
            }
        } catch (e: Exception) {
             logSafeError("Failed to get linked identities", e)
             Result.failure(e)
        }
    }

    override suspend fun updateEmail(email: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.updateUser {
                    this.email = email
                }
                Napier.d("Email updated to $email", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Email update failed", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.functions.invoke("delete-account")
                client.auth.signOut()
                Napier.d("Account deleted", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Delete account failed", e)
            Result.failure(e)
        }
    }

    private fun mapSocialProviderToOAuthProvider(provider: SocialProvider): OAuthProvider {
        return when (provider) {
            SocialProvider.GOOGLE -> Google
            SocialProvider.APPLE -> Apple
            SocialProvider.DISCORD -> Discord
            SocialProvider.GITHUB -> Github
            SocialProvider.TWITTER -> Twitter
            SocialProvider.FACEBOOK -> Facebook
            SocialProvider.SPOTIFY -> Spotify
            SocialProvider.SLACK -> Slack
        }
    }

    // Compatibility alias
    fun getCurrentUserUid(): String? = getCurrentUserId()

    fun getCurrentUserIdentities(): List<io.github.jan.supabase.auth.user.Identity>? {
        return client.auth.currentUserOrNull()?.identities
    }

    private fun logSafeError(message: String, e: Throwable) {
        Napier.e("$message: ${e::class.simpleName} - ${e.message}", tag = TAG)
    }
}
