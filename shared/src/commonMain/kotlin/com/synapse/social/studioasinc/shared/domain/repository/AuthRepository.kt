package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionStatus: Flow<AuthSessionStatus>
    suspend fun signUp(email: String, password: String): Result<String>
    suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String>
    suspend fun ensureProfileExists(userId: String, email: String, username: String? = null): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signOut(): Result<Unit>
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
    fun isEmailVerified(): Boolean
    suspend fun refreshSession(): Result<Unit>
    fun restoreSession(): Boolean
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun resendVerificationEmail(email: String): Result<Unit>
    suspend fun updatePassword(password: String): Result<Unit>
    suspend fun updatePhoneNumber(phone: String): Result<Unit>
    suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String>
    suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit>
    suspend fun signInWithOAuth(provider: SocialProvider, redirectUrl: String): Result<Unit>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<String>
    suspend fun linkIdentity(provider: SocialProvider): Result<Unit>
    suspend fun unlinkIdentity(identityId: String): Result<Unit>
    suspend fun getLinkedIdentities(): Result<List<String>>
    suspend fun updateEmail(email: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
