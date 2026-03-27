package com.synapse.social.studioasinc.shared.domain.usecase.user

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FakeUserRepository : UserRepository {
    var isUpdateSuccessful = true
    var lastUid: String? = null
    var lastUpdates: Map<String, Any?>? = null
    var shouldFail = false

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> = Result.success(true)
    override suspend fun getUserProfile(uid: String): Result<User?> = Result.success(null)
    override suspend fun searchUsers(query: String): Result<List<User>> = Result.success(emptyList())
    override suspend fun getCurrentUserAvatar(): Result<String?> = Result.success(null)

    override suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Result<Boolean> {
        lastUid = uid
        lastUpdates = updates
        return if (shouldFail) {
            Result.failure(Exception("Test exception"))
        } else {
            Result.success(isUpdateSuccessful)
        }
    }
}

class UpdateProfileUseCaseTest {

    @Test
    fun `invoke should pass correct parameters to repository`() = runTest {
        val fakeRepository = FakeUserRepository()
        val useCase = UpdateProfileUseCase(fakeRepository)
        val testUid = "user_123"
        val testUpdates = mapOf(
            "display_name" to "New Name",
            "bio" to "New Bio"
        )

        val result = useCase(testUid, testUpdates)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        assertEquals(testUid, fakeRepository.lastUid)
        assertEquals(testUpdates, fakeRepository.lastUpdates)
    }

    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        val fakeRepository = FakeUserRepository().apply {
            shouldFail = true
        }
        val useCase = UpdateProfileUseCase(fakeRepository)
        val testUid = "user_123"
        val testUpdates = mapOf("display_name" to "New Name")

        val result = useCase(testUid, testUpdates)

        assertTrue(result.isFailure)
        assertEquals("Test exception", result.exceptionOrNull()?.message)
        assertEquals(testUid, fakeRepository.lastUid)
        assertEquals(testUpdates, fakeRepository.lastUpdates)
    }

    @Test
    fun `invoke should return success with false when update is not successful`() = runTest {
        val fakeRepository = FakeUserRepository().apply {
            isUpdateSuccessful = false
        }
        val useCase = UpdateProfileUseCase(fakeRepository)
        val testUid = "user_123"
        val testUpdates = mapOf("display_name" to "New Name")

        val result = useCase(testUid, testUpdates)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
        assertEquals(testUid, fakeRepository.lastUid)
        assertEquals(testUpdates, fakeRepository.lastUpdates)
    }
}
