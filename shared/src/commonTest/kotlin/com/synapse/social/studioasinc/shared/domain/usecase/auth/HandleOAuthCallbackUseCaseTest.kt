package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.OAuthDeepLink
import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class HandleOAuthCallbackUseCaseTest {

    class FakeAuthRepository : AuthRepository {
        var handleOAuthCallbackCalled = false
        var handleOAuthCallbackCode: String? = null
        var handleOAuthCallbackAccessToken: String? = null
        var handleOAuthCallbackRefreshToken: String? = null
        var handleOAuthCallbackResult: Result<Unit> = Result.success(Unit)

        var ensureProfileExistsCalled = false
        var ensureProfileExistsUserId: String? = null
        var ensureProfileExistsEmail: String? = null
        var ensureProfileExistsResult: Result<Unit> = Result.success(Unit)

        var fakeCurrentUserId: String? = null
        var fakeCurrentUserEmail: String? = null

        override val sessionStatus: Flow<AuthSessionStatus> = emptyFlow()
        override suspend fun signUp(email: String, password: String): Result<String> = Result.success("")
        override suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> = Result.success("")
        override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> {
            ensureProfileExistsCalled = true
            ensureProfileExistsUserId = userId
            ensureProfileExistsEmail = email
            return ensureProfileExistsResult
        }
        override suspend fun signIn(email: String, password: String): Result<String> = Result.success("")
        override suspend fun signOut(): Result<Unit> = Result.success(Unit)
        override fun getCurrentUserId(): String? = fakeCurrentUserId
        override fun getCurrentUserEmail(): String? = fakeCurrentUserEmail
        override fun isEmailVerified(): Boolean = true
        override suspend fun refreshSession(): Result<Unit> = Result.success(Unit)
        override fun restoreSession(): Boolean = true
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun resendVerificationEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun updatePassword(password: String): Result<Unit> = Result.success(Unit)
        override suspend fun updatePhoneNumber(phone: String): Result<Unit> = Result.success(Unit)
        override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> = Result.success("")
        override suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit> {
            handleOAuthCallbackCalled = true
            handleOAuthCallbackCode = code
            handleOAuthCallbackAccessToken = accessToken
            handleOAuthCallbackRefreshToken = refreshToken
            return handleOAuthCallbackResult
        }
        override suspend fun signInWithOAuth(provider: SocialProvider, redirectUrl: String): Result<Unit> = Result.success(Unit)
        override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = Result.success("")
        override suspend fun linkIdentity(provider: SocialProvider): Result<Unit> = Result.success(Unit)
        override suspend fun unlinkIdentity(identityId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getLinkedIdentities(): Result<List<String>> = Result.success(emptyList())
        override suspend fun updateEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `invoke with error deep link should return failure and not call repository`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = HandleOAuthCallbackUseCase(repository)
        val deepLink = OAuthDeepLink(error = "access_denied")

        val result = useCase(deepLink)

        assertTrue(result.isFailure)
        assertEquals("OAuth error: access_denied", result.exceptionOrNull()?.message)
        assertFalse(repository.handleOAuthCallbackCalled)
        assertFalse(repository.ensureProfileExistsCalled)
    }

    @Test
    fun `invoke with error and errorDescription should use description in exception message`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = HandleOAuthCallbackUseCase(repository)
        val deepLink = OAuthDeepLink(
            error = "access_denied",
            errorDescription = "User cancelled the login process"
        )

        val result = useCase(deepLink)

        assertTrue(result.isFailure)
        assertEquals("OAuth error: User cancelled the login process", result.exceptionOrNull()?.message)
        assertFalse(repository.handleOAuthCallbackCalled)
        assertFalse(repository.ensureProfileExistsCalled)
    }

    @Test
    fun `invoke with valid deep link and successful callback should ensure profile exists`() = runTest {
        val repository = FakeAuthRepository()
        repository.fakeCurrentUserId = "test_user_id"
        repository.fakeCurrentUserEmail = "test@example.com"
        val useCase = HandleOAuthCallbackUseCase(repository)
        val deepLink = OAuthDeepLink(
            code = "test_code",
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token"
        )

        val result = useCase(deepLink)

        assertTrue(result.isSuccess)
        assertTrue(repository.handleOAuthCallbackCalled)
        assertEquals("test_code", repository.handleOAuthCallbackCode)
        assertEquals("test_access_token", repository.handleOAuthCallbackAccessToken)
        assertEquals("test_refresh_token", repository.handleOAuthCallbackRefreshToken)

        assertTrue(repository.ensureProfileExistsCalled)
        assertEquals("test_user_id", repository.ensureProfileExistsUserId)
        assertEquals("test@example.com", repository.ensureProfileExistsEmail)
    }

    @Test
    fun `invoke with valid deep link but missing user info should not ensure profile exists`() = runTest {
        val repository = FakeAuthRepository()
        // Missing user ID and Email
        repository.fakeCurrentUserId = null
        repository.fakeCurrentUserEmail = null
        val useCase = HandleOAuthCallbackUseCase(repository)
        val deepLink = OAuthDeepLink(
            code = "test_code"
        )

        val result = useCase(deepLink)

        assertTrue(result.isSuccess)
        assertTrue(repository.handleOAuthCallbackCalled)
        assertEquals("test_code", repository.handleOAuthCallbackCode)
        assertFalse(repository.ensureProfileExistsCalled)
    }

    @Test
    fun `invoke with failed handleOAuthCallback should return failure and not ensure profile exists`() = runTest {
        val repository = FakeAuthRepository()
        val exception = Exception("Network error")
        repository.handleOAuthCallbackResult = Result.failure(exception)
        repository.fakeCurrentUserId = "test_user_id"
        repository.fakeCurrentUserEmail = "test@example.com"
        val useCase = HandleOAuthCallbackUseCase(repository)
        val deepLink = OAuthDeepLink(
            code = "test_code"
        )

        val result = useCase(deepLink)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        assertTrue(repository.handleOAuthCallbackCalled)
        assertFalse(repository.ensureProfileExistsCalled)
    }
}
