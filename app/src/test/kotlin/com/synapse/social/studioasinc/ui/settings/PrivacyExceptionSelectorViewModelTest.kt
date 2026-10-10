package com.synapse.social.studioasinc.ui.settings

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyExceptionSelectorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePrivacySettingsRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeFollowRepository: FakeFollowRepository
    private lateinit var viewModel: PrivacyExceptionSelectorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePrivacySettingsRepository()
        fakeAuthRepository = FakeAuthRepository("user_1")
        fakeFollowRepository = FakeFollowRepository(
            followers = listOf(
                User(uid = "user_2", displayName = "Alice"),
                User(uid = "user_3", displayName = "Bob")
            ),
            following = listOf(
                User(uid = "user_3", displayName = "Bob"),
                User(uid = "user_4", displayName = "Charlie")
            )
        )

        val getFollowersUseCase = GetFollowersUseCase(fakeFollowRepository)
        val getFollowingUseCase = GetFollowingUseCase(fakeFollowRepository)

        viewModel = PrivacyExceptionSelectorViewModel(
            getFollowersUseCase = getFollowersUseCase,
            getFollowingUseCase = getFollowingUseCase,
            authRepository = fakeAuthRepository,
            settingsRepository = fakeRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialize_loadsCombinedContactsWithoutDuplicates() = runTest {
        viewModel.initialize("last_seen")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.contacts.size)
        assertEquals(listOf("user_2", "user_3", "user_4"), state.contacts.map { it.uid })
    }

    @Test
    fun onSearchQueryChanged_filtersContacts() = runTest {
        viewModel.initialize("last_seen")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Alice")

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredContacts.size)
        assertEquals("user_2", state.filteredContacts.first().uid)
    }

    @Test
    fun toggleUserSelection_addsAndRemovesUserId() = runTest {
        viewModel.initialize("last_seen")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleUserSelection("user_2")
        assertTrue(viewModel.uiState.value.selectedUserIds.contains("user_2"))

        viewModel.toggleUserSelection("user_2")
        assertTrue(!viewModel.uiState.value.selectedUserIds.contains("user_2"))
    }

    @Test
    fun saveSelection_persistsExcludedUserIds() = runTest {
        viewModel.initialize("last_seen")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleUserSelection("user_2")
        var saved = false
        viewModel.saveSelection { saved = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved)
        assertEquals(setOf("user_2"), fakeRepository.lastSeenExcludedUserIdsValue)
    }
}

class FakeAuthRepository(private val uid: String) : AuthRepository {
    override fun getCurrentUserId(): String? = uid
    override fun getCurrentUserEmail(): String? = "test@example.com"
    override val sessionStatus: kotlinx.coroutines.flow.Flow<com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus> = flowOf(com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus.AUTHENTICATED)
    override suspend fun signUp(email: String, password: String): Result<String> = Result.success(uid)
    override suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> = Result.success(uid)
    override suspend fun ensureProfileExists(userId: String, email: String, username: String?): Result<Unit> = Result.success(Unit)
    override suspend fun signIn(email: String, password: String): Result<String> = Result.success(uid)
    override suspend fun signOut(): Result<Unit> = Result.success(Unit)
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun refreshSession(): Result<Unit> = Result.success(Unit)
    override fun restoreSession(): Boolean = true
    override fun isEmailVerified(): Boolean = true
    override suspend fun resendVerificationEmail(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun updatePassword(password: String): Result<Unit> = Result.success(Unit)
    override suspend fun updatePhoneNumber(phone: String): Result<Unit> = Result.success(Unit)
    override suspend fun getOAuthUrl(provider: String, redirectUrl: String): Result<String> = Result.success("")
    override suspend fun handleOAuthCallback(code: String?, accessToken: String?, refreshToken: String?): Result<Unit> = Result.success(Unit)
    override suspend fun signInWithOAuth(provider: com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider, redirectUrl: String): Result<Unit> = Result.success(Unit)
    override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = Result.success(uid)
    override suspend fun linkIdentity(provider: com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider): Result<Unit> = Result.success(Unit)
    override suspend fun unlinkIdentity(identityId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getLinkedIdentities(): Result<List<String>> = Result.success(emptyList())
    override suspend fun updateEmail(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
}

class FakeFollowRepository(
    private val followers: List<User>,
    private val following: List<User>
) : com.synapse.social.studioasinc.shared.domain.repository.FollowRepository {
    override suspend fun getFollowers(userId: String): Result<List<User>> = Result.success(followers)
    override suspend fun getFollowing(userId: String): Result<List<User>> = Result.success(following)
}
