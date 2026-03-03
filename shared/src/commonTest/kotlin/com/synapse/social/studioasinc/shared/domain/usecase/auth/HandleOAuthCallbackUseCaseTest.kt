package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.OAuthDeepLink
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandleOAuthCallbackUseCaseTest {

    private val mockRepository = object : AuthRepository {
        var handleOAuthCallbackCalled = false
        var handleOAuthCallbackCode: String? = null
        var handleOAuthCallbackAccessToken: String? = null
        var handleOAuthCallbackRefreshToken: String? = null
        var handleOAuthCallbackResult: Result<Unit> = Result.success(Unit)

        var ensureProfileExistsCalled = false
        var ensureProfileExistsUserId: String? = null
        var ensureProfileExistsEmail: String? = null

        var testUserId: String? = "test-user-id"
        var testUserEmail: String? = "test@example.com"

        override val sessionStatus: Flow<AuthSessionStatus> = flowOf()
        override suspend fun signUp(email: String, password: String): Result<String> = Result.success("")
        override suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> = Result.success("")
        override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> {
            ensureProfileExistsCalled = true
            ensureProfileExistsUserId = userId
            ensureProfileExistsEmail = email
            return Result.success(Unit)
        }
        override suspend fun signIn(email: String, password: String): Result<String> = Result.success("")
        override suspend fun signOut(): Result<Unit> = Result.success(Unit)
        override fun getCurrentUserId(): String? = testUserId
        override fun getCurrentUserEmail(): String? = testUserEmail
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
        override suspend fun linkIdentity(provider: SocialProvider): Result<Unit> = Result.success(Unit)
        override suspend fun unlinkIdentity(identityId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getLinkedIdentities(): Result<List<String>> = Result.success(emptyList())
        override suspend fun updateEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    }

    private val useCase = HandleOAuthCallbackUseCase(mockRepository)

    @Test
    fun testErrorInDeepLink() = runTest {
        val errorDescription = "Access denied"
        val deepLink = OAuthDeepLink(
            error = "access_denied",
            errorDescription = errorDescription
        )

        val result = useCase(deepLink)

        assertTrue(result.isFailure)
        assertEquals("OAuth error: $errorDescription", result.exceptionOrNull()?.message)
        assertTrue(!mockRepository.handleOAuthCallbackCalled)
    }

    @Test
    fun testErrorInDeepLinkFallback() = runTest {
        val error = "access_denied"
        val deepLink = OAuthDeepLink(
            error = error,
            errorDescription = null
        )

        val result = useCase(deepLink)

        assertTrue(result.isFailure)
        assertEquals("OAuth error: $error", result.exceptionOrNull()?.message)
        assertTrue(!mockRepository.handleOAuthCallbackCalled)
    }

    @Test
    fun testSuccess() = runTest {
        val deepLink = OAuthDeepLink(
            code = "test-code",
            accessToken = "test-access-token",
            refreshToken = "test-refresh-token"
        )
        mockRepository.handleOAuthCallbackResult = Result.success(Unit)
        mockRepository.testUserId = "test-user-id"
        mockRepository.testUserEmail = "test@example.com"

        val result = useCase(deepLink)

        assertTrue(result.isSuccess)
        assertTrue(mockRepository.handleOAuthCallbackCalled)
        assertEquals("test-code", mockRepository.handleOAuthCallbackCode)
        assertEquals("test-access-token", mockRepository.handleOAuthCallbackAccessToken)
        assertEquals("test-refresh-token", mockRepository.handleOAuthCallbackRefreshToken)

        assertTrue(mockRepository.ensureProfileExistsCalled)
        assertEquals("test-user-id", mockRepository.ensureProfileExistsUserId)
        assertEquals("test@example.com", mockRepository.ensureProfileExistsEmail)
    }

    @Test
    fun testSuccessMissingEmailOrUserId() = runTest {
        val deepLink = OAuthDeepLink(
            code = "test-code",
            accessToken = "test-access-token",
            refreshToken = "test-refresh-token"
        )
        mockRepository.handleOAuthCallbackResult = Result.success(Unit)
        mockRepository.testUserId = null
        mockRepository.testUserEmail = null
        mockRepository.ensureProfileExistsCalled = false

        val result = useCase(deepLink)

        assertTrue(result.isSuccess)
        assertTrue(mockRepository.handleOAuthCallbackCalled)
        assertTrue(!mockRepository.ensureProfileExistsCalled)
    }

    @Test
    fun testRepositoryFailure() = runTest {
        val deepLink = OAuthDeepLink(
            code = "test-code",
            accessToken = "test-access-token",
            refreshToken = "test-refresh-token"
        )
        val expectedException = Exception("Repository error")
        mockRepository.handleOAuthCallbackResult = Result.failure(expectedException)
        mockRepository.ensureProfileExistsCalled = false

        val result = useCase(deepLink)

        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
        assertTrue(mockRepository.handleOAuthCallbackCalled)
        assertTrue(!mockRepository.ensureProfileExistsCalled)
    }
}
