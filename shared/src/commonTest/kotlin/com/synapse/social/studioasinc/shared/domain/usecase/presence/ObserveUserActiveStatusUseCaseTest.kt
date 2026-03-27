package com.synapse.social.studioasinc.shared.domain.usecase.presence

import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ObserveUserActiveStatusUseCaseTest {
    
    @Test
    fun `invoke returns flow from repository`() = runTest {
        // Given
        val userId = "test-user-id"
        val expectedStatus = true
        val repository = FakePresenceRepository(expectedStatus)
        val useCase = ObserveUserActiveStatusUseCase(repository)
        
        // When
        val result = useCase(userId).first()
        
        // Then
        assertTrue(result == expectedStatus)
    }
}

private class FakePresenceRepository(
    private val status: Boolean
) : PresenceRepository {
    override suspend fun updatePresence(isOnline: Boolean, currentChatId: String?): Result<Unit> = Result.success(Unit)
    override suspend fun startPresenceTracking() {}
    override suspend fun stopPresenceTracking() {}
    override fun observeUserPresence(userId: String) = flowOf(status)
    override suspend fun isUserInChat(userId: String, chatId: String): Boolean = false
}
