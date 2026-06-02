package com.synapse.social.studioasinc.shared.data.repository
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.model.UserProfileInsert
import com.synapse.social.studioasinc.shared.data.model.UserSettingsInsert
import com.synapse.social.studioasinc.shared.data.model.UserPresenceInsert
import com.synapse.social.studioasinc.shared.data.mapper.AuthErrorMapper
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

/**
 * Implementation of [AuthRepository] that utilizes Supabase Auth for session management
 * and Postgrest for user profile synchronization.
 *
 * This repository handles the full authentication lifecycle, including traditional
 * email/password flows, OAuth integrations, and account management.
 *
 * @property client The [SupabaseClientLib] instance used for authentication and database operations.
 */
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

    /**
     * Registers a new user account with the provided email and password.
     *
     * @return A [Result] containing the new user's ID if successful.
     */
    override suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            withContext(AppDispatchers.IO) {
                val user = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                val userId = user?.id ?: client.auth.currentUserOrNull()?.id
                    ?: throw Exception("User ID not found")
                Napier.d("User signed up: $userId", tag = TAG)
                Result.success(userId)
            }
        } catch (e: Exception) {
            logSafeError("Sign up failed", e)
            val authError = AuthErrorMapper.mapException(e)
            Result.failure(authError)
        }
    }

    /**
     * Registers a new user and immediately initializes their profile in the database.
     *
     * This is a convenience method that chains [signUp] and [ensureProfileExists]
     * to ensure the application state is consistent immediately after registration.
     */
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
            val authError = AuthErrorMapper.mapException(e)
            Result.failure(authError)
        }
    }

    /**
     * Verifies if a user profile exists in the 'users' table and creates one if it's missing.
     *
     * This synchronization step is crucial for new users or those who signed up via
     * mechanisms that don't automatically trigger profile creation.
     *
     * @param userId The unique identifier for the user.
     * @param email The user's email address, used to generate a default username if needed.
     * @param username An optional preferred username.
     */
    override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
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
                    Napier.d("Inserting user profile for $userId into users table...", tag = TAG)
                    client.from("users").insert(profileInsert)
                    Napier.d("Successfully inserted user profile for $userId.", tag = TAG)

                    // Note: user_settings and user_presence are automatically created by database trigger
                    // when the user signs up via Supabase Auth (see handle_new_auth_user trigger)
                    // No need to manually insert them here

                    Napier.d("User profile created: $userId", tag = TAG)
                } else {
                    Napier.d("Profile already exists for $userId.", tag = TAG)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Ensure profile exists failed", e)
            val authError = AuthErrorMapper.mapException(e)
            Result.failure(authError)
        }
    }

    /**
     * Authenticates a user using their email and password credentials.
     *
     * @return A [Result] containing the user's ID if authentication is successful.
     */
    override suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            withContext(AppDispatchers.IO) {
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
            val authError = AuthErrorMapper.mapException(e)
            Result.failure(authError)
        }
    }

    /**
     * Terminates the current user session and clears local authentication state.
     */
    override suspend fun signOut(): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
                client.auth.signOut()
                Napier.d("User signed out", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Sign out failed", e)
            Result.failure(e)
        }
    }

    /**
     * Returns the unique identifier of the currently authenticated user, or null if no session exists.
     */
    override fun getCurrentUserId(): String? {
        return try {
            client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            logSafeError("Failed to get current user ID", e)
            null
        }
    }

    /**
     * Returns the email address of the currently authenticated user, or null if no session exists.
     */
    override fun getCurrentUserEmail(): String? {
        return try {
            client.auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            logSafeError("Failed to get current user email", e)
            null
        }
    }

    /**
     * Checks if the user's primary email identity has been verified.
     */
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

    /**
     * Forcefully refreshes the current authentication session token.
     */
    override suspend fun refreshSession(): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
                client.auth.refreshCurrentSession()
                Napier.d("Session refreshed", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Session refresh failed", e)
            Result.failure(e)
        }
    }

    /**
     * Determines if a valid session is currently cached and can be restored.
     */
    override fun restoreSession(): Boolean {
        return try {
            client.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            logSafeError("Session restore failed", e)
            false
        }
    }

    /**
     * Sends a password reset link to the specified email address.
     */
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
                client.auth.resetPasswordForEmail(email)
                Napier.d("Password reset email sent", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Password reset failed", e)
            Result.failure(e)
        }
    }

    /**
     * Resends the verification email for a user who has not yet confirmed their email address.
     */
    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
                client.auth.resendEmail(OtpType.Email.SIGNUP, email)
                Napier.d("Verification email resent", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Resend verification email failed", e)
            Result.failure(e)
        }
    }

    /**
     * Updates the authenticated user's password.
     */
    override suspend fun updatePassword(password: String): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
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

    /**
     * Updates the authenticated user's phone number.
     */
    override suspend fun updatePhoneNumber(phone: String): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
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

    /**
     * Generates an authorization URL for OAuth flows.
     *
     * This method manually constructs the URL using [URLBuilder] to ensure the correct
     * parameters are appended for the specific provider and redirect URI.
     *
     * @param provider The name of the social provider (e.g., "google", "apple").
     * @param redirectUrl The URI to redirect to after authentication.
     */
    override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> {
        return try {
            // Map string provider to SocialProvider enum
            val socialProvider = when (provider.lowercase()) {
                "google" -> SocialProvider.GOOGLE
                "apple" -> SocialProvider.APPLE
                "github" -> SocialProvider.GITHUB
                "discord" -> SocialProvider.DISCORD
                "twitter" -> SocialProvider.TWITTER
                "facebook" -> SocialProvider.FACEBOOK
                "spotify" -> SocialProvider.SPOTIFY
                "slack" -> SocialProvider.SLACK
                else -> throw IllegalArgumentException("Unsupported OAuth provider: $provider")
            }
            
            val oauthProvider = mapSocialProviderToOAuthProvider(socialProvider)
            
            // Use Supabase's built-in OAuth URL generation with proper redirect
            val oauthUrl = URLBuilder(client.supabaseUrl).apply {
                appendPathSegments("auth", "v1", "authorize")
                parameters.append("provider", oauthProvider.name.lowercase())
                parameters.append("redirect_to", redirectUrl)
            }.buildString()
            
            Napier.d("Generated OAuth URL for ${oauthProvider.name}: $oauthUrl", tag = TAG)
            Result.success(oauthUrl)
        } catch (e: Exception) {
            logSafeError("OAuth URL generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Finalizes an OAuth flow by exchanging a code for a session or importing tokens.
     *
     * This supports both standard PKCE flows (using `code`) and manual token management.
     */
    override suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
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

    /**
     * Initiates a native OAuth sign-in flow.
     */
    override suspend fun signInWithOAuth(provider: SocialProvider, redirectUrl: String): Result<Unit> {
        val supabaseProvider: OAuthProvider = mapSocialProviderToOAuthProvider(provider)
        return try {
            withContext(AppDispatchers.IO) {
                client.auth.signInWith(supabaseProvider, redirectUrl)
                Napier.d("OAuth sign-in initiated for ${supabaseProvider.name} with redirect URI: $redirectUrl", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("OAuth sign-in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Authenticates a user specifically using a Google ID token.
     *
     * After a successful sign-in, it verifies that the user's profile exists
     * in the application database.
     */
    override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> {
        return try {
            withContext(AppDispatchers.IO) {
                // Use the ID token to sign in with Google
                client.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    this.provider = Google
                }
                
                // Retrieve the user from the established session
                val user = client.auth.currentUserOrNull()
                val userId = user?.id
                    ?: throw Exception("User ID not found after Google sign-in")
                val email = user?.email
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

    /**
     * Links an additional social identity to the currently authenticated account.
     */
    override suspend fun linkIdentity(provider: SocialProvider): Result<Unit> {
        val supabaseProvider: OAuthProvider = mapSocialProviderToOAuthProvider(provider)
        return try {
            withContext(AppDispatchers.IO) {
                client.auth.linkIdentity(supabaseProvider)
                Napier.d("Link identity initiated for ${supabaseProvider.name}", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Link identity failed", e)
            Result.failure(e)
        }
    }

    /**
     * Removes a social identity link from the user's account.
     */
    override suspend fun unlinkIdentity(identityId: String): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
                client.auth.unlinkIdentity(identityId)
                Napier.d("Unlinked identity: $identityId", tag = TAG)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logSafeError("Unlink identity failed", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves a list of social providers currently linked to the user's account.
     */
    override suspend fun getLinkedIdentities(): Result<List<String>> {
        return try {
            withContext(AppDispatchers.IO) {
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

    /**
     * Updates the primary email address associated with the user's account.
     */
    override suspend fun updateEmail(email: String): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
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

    /**
     * Permanently deletes the user's account and all associated data.
     *
     * This invokes a Supabase Edge Function to handle complex data cleanup
     * across various tables before signing the user out.
     */
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            withContext(AppDispatchers.IO) {
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
