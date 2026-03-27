package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignInUseCaseTest {

    @Test
    fun `invoke with valid credentials returns success`() = runTest {
        val fakeRepository = FakeAuthRepository().apply {
            shouldSucceed = true
            expectedUserId = "user_123"
        }
        val useCase = SignInUseCase(fakeRepository)

        val result = useCase("test@example.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals("user_123", result.getOrNull())
        assertEquals("test@example.com", fakeRepository.lastSignInEmail)
        assertEquals("password123", fakeRepository.lastSignInPassword)
    }

    @Test
    fun `invoke with invalid credentials returns failure`() = runTest {
        val fakeRepository = FakeAuthRepository().apply {
            shouldSucceed = false
            errorToThrow = Exception("Invalid credentials")
        }
        val useCase = SignInUseCase(fakeRepository)

        val result = useCase("test@example.com", "wrong_password")

        assertTrue(result.isFailure)
        assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
        assertEquals("test@example.com", fakeRepository.lastSignInEmail)
        assertEquals("wrong_password", fakeRepository.lastSignInPassword)
    }

    // A fake implementation of AuthRepository to stub out responses for testing
    class FakeAuthRepository : AuthRepository {
        var shouldSucceed: Boolean = true
        var expectedUserId: String = "default_user_id"
        var errorToThrow: Throwable = Exception("Unknown error")

        var lastSignInEmail: String? = null
        var lastSignInPassword: String? = null

        override val sessionStatus: Flow<AuthSessionStatus> = MutableStateFlow(AuthSessionStatus.NOT_AUTHENTICATED)

        override suspend fun signIn(email: String, password: String): Result<String> {
            lastSignInEmail = email
            lastSignInPassword = password
            return if (shouldSucceed) {
                Result.success(expectedUserId)
            } else {
                Result.failure(errorToThrow)
            }
        }

        override suspend fun signUp(email: String, password: String): Result<String> = Result.success("")
        override suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> = Result.success("")
        override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> = Result.success(Unit)
        override suspend fun signOut(): Result<Unit> = Result.success(Unit)
        override fun getCurrentUserId(): String? = null
        override fun getCurrentUserEmail(): String? = null
        override fun isEmailVerified(): Boolean = false
        override suspend fun refreshSession(): Result<Unit> = Result.success(Unit)
        override fun restoreSession(): Boolean = false
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun resendVerificationEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun updatePassword(password: String): Result<Unit> = Result.success(Unit)
        override suspend fun updatePhoneNumber(phone: String): Result<Unit> = Result.success(Unit)
        override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> = Result.success("")
        override suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit> = Result.success(Unit)
        override suspend fun signInWithOAuth(provider: SocialProvider, redirectUrl: String): Result<Unit> = Result.success(Unit)
        override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = Result.success("")
        override suspend fun linkIdentity(provider: SocialProvider): Result<Unit> = Result.success(Unit)
        override suspend fun unlinkIdentity(identityId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getLinkedIdentities(): Result<List<String>> = Result.success(emptyList())
        override suspend fun updateEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    }
}
