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

class SignUpUseCaseTest {

    private class FakeAuthRepository : AuthRepository {
        var signUpWithProfileResult: Result<String> = Result.success("dummy_id")
        var emailPassed: String? = null
        var passwordPassed: String? = null
        var usernamePassed: String? = null

        override val sessionStatus: Flow<AuthSessionStatus> = MutableStateFlow(AuthSessionStatus.NOT_AUTHENTICATED)

        override suspend fun signUp(email: String, password: String): Result<String> = Result.failure(NotImplementedError())

        override suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> {
            emailPassed = email
            passwordPassed = password
            usernamePassed = username
            return signUpWithProfileResult
        }

        override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun signIn(email: String, password: String): Result<String> = Result.failure(NotImplementedError())
        override suspend fun signOut(): Result<Unit> = Result.failure(NotImplementedError())
        override fun getCurrentUserId(): String? = null
        override fun getCurrentUserEmail(): String? = null
        override fun isEmailVerified(): Boolean = false
        override suspend fun refreshSession(): Result<Unit> = Result.failure(NotImplementedError())
        override fun restoreSession(): Boolean = false
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun resendVerificationEmail(email: String): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun updatePassword(password: String): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun updatePhoneNumber(phone: String): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> = Result.failure(NotImplementedError())
        override suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun signInWithOAuth(provider: SocialProvider, redirectUrl: String): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = Result.failure(NotImplementedError())
        override suspend fun linkIdentity(provider: SocialProvider): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun unlinkIdentity(identityId: String): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun getLinkedIdentities(): Result<List<String>> = Result.failure(NotImplementedError())
        override suspend fun updateEmail(email: String): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun deleteAccount(): Result<Unit> = Result.failure(NotImplementedError())
    }

    @Test
    fun `invoke should return success when repository returns success`() = runTest {
        // Arrange
        val fakeRepository = FakeAuthRepository()
        val expectedResult = Result.success("user_id_123")
        fakeRepository.signUpWithProfileResult = expectedResult
        val useCase = SignUpUseCase(fakeRepository)

        val email = "test@example.com"
        val password = "Password123!"
        val username = "testuser"

        // Act
        val result = useCase(email, password, username)

        // Assert
        assertEquals(expectedResult, result)
        assertEquals(email, fakeRepository.emailPassed)
        assertEquals(password, fakeRepository.passwordPassed)
        assertEquals(username, fakeRepository.usernamePassed)
    }

    @Test
    fun `invoke should return failure when repository returns failure`() = runTest {
        // Arrange
        val fakeRepository = FakeAuthRepository()
        val expectedError = Exception("Sign up failed")
        val expectedResult = Result.failure<String>(expectedError)
        fakeRepository.signUpWithProfileResult = expectedResult
        val useCase = SignUpUseCase(fakeRepository)

        val email = "test@example.com"
        val password = "Password123!"
        val username = "testuser"

        // Act
        val result = useCase(email, password, username)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(expectedError, result.exceptionOrNull())
        assertEquals(email, fakeRepository.emailPassed)
        assertEquals(password, fakeRepository.passwordPassed)
        assertEquals(username, fakeRepository.usernamePassed)
    }
}
