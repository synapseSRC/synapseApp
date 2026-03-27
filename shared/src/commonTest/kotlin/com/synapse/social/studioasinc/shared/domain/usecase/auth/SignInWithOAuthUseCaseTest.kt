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

class FakeAuthRepository : AuthRepository {
    override val sessionStatus: Flow<AuthSessionStatus> = MutableStateFlow(AuthSessionStatus.NOT_AUTHENTICATED)

    var signInWithOAuthCalledCount = 0
    var lastProvider: SocialProvider? = null
    var lastRedirectUrl: String? = null
    var signInWithOAuthResult: Result<Unit> = Result.success(Unit)

    override suspend fun signUp(email: String, password: String): Result<String> = Result.failure(Exception("Not implemented"))
    override suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> = Result.failure(Exception("Not implemented"))
    override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> = Result.failure(Exception("Not implemented"))
    override suspend fun signIn(email: String, password: String): Result<String> = Result.failure(Exception("Not implemented"))
    override suspend fun signOut(): Result<Unit> = Result.failure(Exception("Not implemented"))
    override fun getCurrentUserId(): String? = null
    override fun getCurrentUserEmail(): String? = null
    override fun isEmailVerified(): Boolean = false
    override suspend fun refreshSession(): Result<Unit> = Result.failure(Exception("Not implemented"))
    override fun restoreSession(): Boolean = false
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.failure(Exception("Not implemented"))
    override suspend fun resendVerificationEmail(email: String): Result<Unit> = Result.failure(Exception("Not implemented"))
    override suspend fun updatePassword(password: String): Result<Unit> = Result.failure(Exception("Not implemented"))
    override suspend fun updatePhoneNumber(phone: String): Result<Unit> = Result.failure(Exception("Not implemented"))
    override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> = Result.failure(Exception("Not implemented"))
    override suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit> = Result.failure(Exception("Not implemented"))

    override suspend fun signInWithOAuth(provider: SocialProvider, redirectUrl: String): Result<Unit> {
        signInWithOAuthCalledCount++
        lastProvider = provider
        lastRedirectUrl = redirectUrl
        return signInWithOAuthResult
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = Result.failure(Exception("Not implemented"))
    override suspend fun linkIdentity(provider: SocialProvider): Result<Unit> = Result.failure(Exception("Not implemented"))
    override suspend fun unlinkIdentity(identityId: String): Result<Unit> = Result.failure(Exception("Not implemented"))
    override suspend fun getLinkedIdentities(): Result<List<String>> = Result.failure(Exception("Not implemented"))
    override suspend fun updateEmail(email: String): Result<Unit> = Result.failure(Exception("Not implemented"))
    override suspend fun deleteAccount(): Result<Unit> = Result.failure(Exception("Not implemented"))
}

class SignInWithOAuthUseCaseTest {

    private val fakeRepository = FakeAuthRepository()
    private val useCase = SignInWithOAuthUseCase(fakeRepository)

    @Test
    fun `invoke calls repository with correct parameters and returns success`() = runTest {
        // Arrange
        val provider = SocialProvider.GOOGLE
        val redirectUrl = "app://callback"
        fakeRepository.signInWithOAuthResult = Result.success(Unit)

        // Act
        val result = useCase(provider, redirectUrl)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(1, fakeRepository.signInWithOAuthCalledCount)
        assertEquals(provider, fakeRepository.lastProvider)
        assertEquals(redirectUrl, fakeRepository.lastRedirectUrl)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        // Arrange
        val provider = SocialProvider.GITHUB
        val redirectUrl = "app://callback"
        val exception = Exception("OAuth failed")
        fakeRepository.signInWithOAuthResult = Result.failure(exception)

        // Act
        val result = useCase(provider, redirectUrl)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        assertEquals(1, fakeRepository.signInWithOAuthCalledCount)
        assertEquals(provider, fakeRepository.lastProvider)
        assertEquals(redirectUrl, fakeRepository.lastRedirectUrl)
    }

    @Test
    fun `invoke validates redirect URL is not empty`() = runTest {
        // Arrange
        val provider = SocialProvider.GOOGLE
        val redirectUrl = ""

        // Act
        val result = useCase(provider, redirectUrl)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Redirect URL cannot be empty", result.exceptionOrNull()?.message)
        assertEquals(0, fakeRepository.signInWithOAuthCalledCount)
    }
}
