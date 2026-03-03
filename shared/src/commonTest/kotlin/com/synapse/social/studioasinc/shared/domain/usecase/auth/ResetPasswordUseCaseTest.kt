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

class ResetPasswordUseCaseTest {

    @Test
    fun `invoke should return success when repository returns success`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = ResetPasswordUseCase(repository)
        repository.updatePasswordResult = Result.success(Unit)

        val result = useCase("new_password")

        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        assertEquals("new_password", repository.lastUpdatedPassword)
    }

    @Test
    fun `invoke should return failure when repository returns failure`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = ResetPasswordUseCase(repository)
        val exception = Exception("Reset password failed")
        repository.updatePasswordResult = Result.failure(exception)

        val result = useCase("new_password")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        assertEquals("new_password", repository.lastUpdatedPassword)
    }

    private class FakeAuthRepository : AuthRepository {
        var updatePasswordResult: Result<Unit> = Result.success(Unit)
        var lastUpdatedPassword: String? = null

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
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun resendVerificationEmail(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun updatePassword(password: String): Result<Unit> {
            lastUpdatedPassword = password
            return updatePasswordResult
        }

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
}
