package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResendVerificationEmailUseCaseTest {

    private class MockAuthRepository : AuthRepository {
        var resendVerificationEmailCalledWith: String? = null
        var resendVerificationEmailResult: Result<Unit> = Result.success(Unit)

        override val sessionStatus: Flow<AuthSessionStatus> get() = throw NotImplementedError()
        override suspend fun signUp(email: String, password: String): Result<String> = throw NotImplementedError()
        override suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> = throw NotImplementedError()
        override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> = throw NotImplementedError()
        override suspend fun signIn(email: String, password: String): Result<String> = throw NotImplementedError()
        override suspend fun signOut(): Result<Unit> = throw NotImplementedError()
        override fun getCurrentUserId(): String? = throw NotImplementedError()
        override fun getCurrentUserEmail(): String? = throw NotImplementedError()
        override fun isEmailVerified(): Boolean = throw NotImplementedError()
        override suspend fun refreshSession(): Result<Unit> = throw NotImplementedError()
        override fun restoreSession(): Boolean = throw NotImplementedError()
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = throw NotImplementedError()
        override suspend fun resendVerificationEmail(email: String): Result<Unit> {
            resendVerificationEmailCalledWith = email
            return resendVerificationEmailResult
        }
        override suspend fun updatePassword(password: String): Result<Unit> = throw NotImplementedError()
        override suspend fun updatePhoneNumber(phone: String): Result<Unit> = throw NotImplementedError()
        override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> = throw NotImplementedError()
        override suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit> = throw NotImplementedError()
        override suspend fun signInWithOAuth(provider: SocialProvider, redirectUrl: String): Result<Unit> = throw NotImplementedError()
        override suspend fun linkIdentity(provider: SocialProvider): Result<Unit> = throw NotImplementedError()
        override suspend fun unlinkIdentity(identityId: String): Result<Unit> = throw NotImplementedError()
        override suspend fun getLinkedIdentities(): Result<List<String>> = throw NotImplementedError()
        override suspend fun updateEmail(email: String): Result<Unit> = throw NotImplementedError()
        override suspend fun deleteAccount(): Result<Unit> = throw NotImplementedError()
    }

    @Test
    fun testInvoke_success() = runTest {
        val mockRepo = MockAuthRepository()
        val useCase = ResendVerificationEmailUseCase(mockRepo)
        val email = "test@example.com"

        val result = useCase(email)

        assertTrue(result.isSuccess)
        assertEquals(email, mockRepo.resendVerificationEmailCalledWith)
    }

    @Test
    fun testInvoke_failure() = runTest {
        val mockRepo = MockAuthRepository()
        val exception = RuntimeException("Network error")
        mockRepo.resendVerificationEmailResult = Result.failure(exception)

        val useCase = ResendVerificationEmailUseCase(mockRepo)
        val email = "test@example.com"

        val result = useCase(email)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        assertEquals(email, mockRepo.resendVerificationEmailCalledWith)
    }
}
