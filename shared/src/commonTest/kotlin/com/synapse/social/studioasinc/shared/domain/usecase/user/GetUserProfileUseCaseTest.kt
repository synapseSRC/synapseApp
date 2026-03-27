package com.synapse.social.studioasinc.shared.domain.usecase.user

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetUserProfileUseCaseTest {
    @Test
    fun `invoke returns user profile on success`() = runTest {
        val expectedUser = User(uid = "123", username = "testuser")
        val fakeRepository = object : UserRepository {
            override suspend fun isUsernameAvailable(username: String): Result<Boolean> = Result.success(true)
            override suspend fun getUserProfile(uid: String): Result<User?> = Result.success(expectedUser)
            override suspend fun searchUsers(query: String): Result<List<User>> = Result.success(emptyList())
            override suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Result<Boolean> = Result.success(true)
            override suspend fun getCurrentUserAvatar(): Result<String?> = Result.success(null)
        }

        val useCase = GetUserProfileUseCase(fakeRepository)
        val result = useCase.invoke("123")

        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
    }
}
