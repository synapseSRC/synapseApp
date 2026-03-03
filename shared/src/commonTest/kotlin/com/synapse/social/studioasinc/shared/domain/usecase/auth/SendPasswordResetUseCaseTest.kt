package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendPasswordResetUseCaseTest {

    private class MockAuthRepository(
        private val sendPasswordResetResult: Result<Unit>
    ) : AuthRepository {
        override val sessionStatus: Flow<AuthSessionStatus> = emptyFlow()

        override suspend fun signUp(email: String, password: String): Result<String> = Result.success("")
        override suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> = Result.success("")
        override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> = Result.success(Unit)
        override suspend fun signIn(email: String, password: String): Result<String> = Result.success("")
        override suspend fun signOut(): Result<Unit> = Result.success(Unit)
        override fun getCurrentUserId(): String? = null
        override fun getCurrentUserEmail(): String? = null
        override fun isEmailVerified(): Boolean = false
        override suspend fun refreshSession(): Result<Unit> = Result.success(Unit)
        override fun restoreSession(): Boolean = false

        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
            return sendPasswordResetResult
        }

        override suspend fun resendVerificationEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun updatePassword(password: String): Result<Unit> = Result.success(Unit)
        override suspend fun updatePhoneNumber(phone: String): Result<Unit> = Result.success(Unit)
        override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> = Result.success("")
        override suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit> = Result.success(Unit)
        override suspend fun signInWithOAuth(provider: SocialProvider, redirectUrl: String): Result<Unit> = Result.success(Unit)
        override suspend fun linkIdentity(provider: SocialProvider): Result<Unit> = Result.success(Unit)
        override suspend fun unlinkIdentity(identityId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getLinkedIdentities(): Result<List<String>> = Result.success(emptyList())
        override suspend fun updateEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `invoke should return success when repository call succeeds`() = runTest {
        // Arrange
        val repository = MockAuthRepository(Result.success(Unit))
        val useCase = SendPasswordResetUseCase(repository)
        val email = "test@example.com"

        // Act
        val result = useCase(email)

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke should return failure when repository call fails`() = runTest {
        // Arrange
        val exception = Exception("Failed to send reset email")
        val repository = MockAuthRepository(Result.failure(exception))
        val useCase = SendPasswordResetUseCase(repository)
        val email = "test@example.com"

        // Act
        val result = useCase(email)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
