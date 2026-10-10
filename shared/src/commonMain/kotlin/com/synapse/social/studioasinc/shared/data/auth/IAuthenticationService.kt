package com.synapse.social.studioasinc.shared.data.auth



interface IAuthenticationService {


    suspend fun signUp(email: String, password: String): Result<String>
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signOut(): Result<Unit>


    suspend fun createUserProfile(userId: String, email: String, username: String): Result<Unit>
    suspend fun ensureProfileExists(userId: String, email: String): Result<Unit>


    suspend fun getCurrentUserId(): String?
    suspend fun getCurrentUserEmail(): String?
    suspend fun isEmailVerified(): Boolean
    suspend fun restoreSession(): Boolean
    suspend fun refreshSession(): Result<Unit>


    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun resendVerificationEmail(email: String): Result<Unit>
    suspend fun updatePassword(password: String): Result<Unit>
    suspend fun updateEmail(email: String): Result<Unit>


    suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String>
    suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit>


    suspend fun storeSessionTokens(accessToken: String, refreshToken: String, userId: String, userEmail: String, expiresIn: Int)
    suspend fun clearStoredTokens()
}
