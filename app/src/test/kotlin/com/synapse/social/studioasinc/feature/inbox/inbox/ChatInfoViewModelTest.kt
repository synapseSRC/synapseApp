package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.ChatLockManager
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.IsUserBlockedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.UnblockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteConversationUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetSharedContentSummaryUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SetDisappearingModeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SharedContentSummary
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatInfoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getUserProfileUseCase = mock(GetUserProfileUseCase::class.java)
    private val observeUserPresenceUseCase = mock(ObserveUserPresenceUseCase::class.java)
    private val getDisappearingModeUseCase = mock(GetDisappearingModeUseCase::class.java)
    private val setDisappearingModeUseCase = mock(SetDisappearingModeUseCase::class.java)
    private val blockUserUseCase = mock(BlockUserUseCase::class.java)
    private val unblockUserUseCase = mock(UnblockUserUseCase::class.java)
    private val isUserBlockedUseCase = mock(IsUserBlockedUseCase::class.java)
    private val deleteConversationUseCase = mock(DeleteConversationUseCase::class.java)
    private val getSharedContentSummaryUseCase = mock(GetSharedContentSummaryUseCase::class.java)
    private val chatLockManager = mock(ChatLockManager::class.java)

    private lateinit var viewModel: ChatInfoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChatInfoViewModel(
            getUserProfileUseCase = getUserProfileUseCase,
            observeUserPresenceUseCase = observeUserPresenceUseCase,
            getDisappearingModeUseCase = getDisappearingModeUseCase,
            setDisappearingModeUseCase = setDisappearingModeUseCase,
            blockUserUseCase = blockUserUseCase,
            unblockUserUseCase = unblockUserUseCase,
            isUserBlockedUseCase = isUserBlockedUseCase,
            deleteConversationUseCase = deleteConversationUseCase,
            getSharedContentSummaryUseCase = getSharedContentSummaryUseCase,
            chatLockManager = chatLockManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadChatInfo populates profile, presence, settings, and shared content summary`() = runTest {
        val userId = "user_1"
        val chatId = "chat_1"
        val user = User(uid = userId, username = "alice", displayName = "Alice")
        val summary = SharedContentSummary(mediaCount = 5, linkCount = 2, fileCount = 1, isSample = false)

        `when`(getUserProfileUseCase.invoke(userId)).thenReturn(Result.success(user))
        `when`(isUserBlockedUseCase.invoke(userId)).thenReturn(Result.success(false))
        `when`(observeUserPresenceUseCase.invoke(userId)).thenReturn(flowOf(true))
        `when`(chatLockManager.isChatLocked(chatId)).thenReturn(true)
        `when`(getDisappearingModeUseCase.invoke(chatId)).thenReturn(Result.success(DisappearingMode.TWENTY_FOUR_HOURS))
        `when`(getSharedContentSummaryUseCase.invoke(chatId)).thenReturn(Result.success(summary))

        viewModel.loadChatInfo(chatId, userId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Alice", state.userProfile?.displayName)
        assertTrue(state.isOnline)
        assertFalse(state.isBlocked)
        assertTrue(state.isLocked)
        assertEquals(DisappearingMode.TWENTY_FOUR_HOURS, state.disappearingMode)
        assertEquals(5, state.mediaCount)
        assertEquals(2, state.linkCount)
        assertEquals(1, state.fileCount)
        assertNull(state.errorMessageRes)
    }

    @Test
    fun `loadChatInfo sets error message when profile load fails`() = runTest {
        val userId = "user_err"
        val chatId = "chat_1"

        `when`(getUserProfileUseCase.invoke(userId)).thenReturn(Result.failure(RuntimeException("Error")))
        `when`(isUserBlockedUseCase.invoke(userId)).thenReturn(Result.success(false))
        `when`(observeUserPresenceUseCase.invoke(userId)).thenReturn(flowOf(false))
        `when`(chatLockManager.isChatLocked(chatId)).thenReturn(false)
        `when`(getDisappearingModeUseCase.invoke(chatId)).thenReturn(Result.success(DisappearingMode.OFF))
        `when`(getSharedContentSummaryUseCase.invoke(chatId)).thenReturn(Result.success(SharedContentSummary(0, 0, 0)))

        viewModel.loadChatInfo(chatId, userId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.chat_info_error_load_profile, state.errorMessageRes)
    }

    @Test
    fun `setDisappearingMode updates mode on success`() = runTest {
        val chatId = "chat_1"
        val newMode = DisappearingMode.SEVEN_DAYS
        `when`(setDisappearingModeUseCase.invoke(chatId, newMode)).thenReturn(Result.success(Unit))

        viewModel.setDisappearingMode(chatId, newMode)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DisappearingMode.SEVEN_DAYS, state.disappearingMode)
        assertEquals(R.string.toast_disappearing_mode_updated, state.userMessageRes)
    }

    @Test
    fun `setDisappearingMode sets error on failure`() = runTest {
        val chatId = "chat_1"
        val newMode = DisappearingMode.SEVEN_DAYS
        `when`(setDisappearingModeUseCase.invoke(chatId, newMode)).thenReturn(Result.failure(RuntimeException("Failed")))

        viewModel.setDisappearingMode(chatId, newMode)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.chat_info_error_update_disappearing_mode, state.errorMessageRes)
    }

    @Test
    fun `toggleChatLock toggles lock state`() = runTest {
        val chatId = "chat_1"
        `when`(chatLockManager.isChatLocked(chatId)).thenReturn(false)

        viewModel.toggleChatLock(chatId)
        verify(chatLockManager).lockChat(chatId)
        assertTrue(viewModel.uiState.value.isLocked)

        viewModel.toggleChatLock(chatId)
        verify(chatLockManager).unlockChat(chatId)
        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun `toggleMuteNotifications toggles muted state`() = runTest {
        assertFalse(viewModel.uiState.value.isMuted)

        viewModel.toggleMuteNotifications()
        assertTrue(viewModel.uiState.value.isMuted)
        assertEquals(R.string.muted, viewModel.uiState.value.userMessageRes)

        viewModel.toggleMuteNotifications()
        assertFalse(viewModel.uiState.value.isMuted)
        assertEquals(R.string.unmuted, viewModel.uiState.value.userMessageRes)
    }

    @Test
    fun `toggleBlockUser blocks user when currently unblocked`() = runTest {
        val userId = "user_1"
        `when`(blockUserUseCase.invoke(userId)).thenReturn(Result.success(Unit))

        viewModel.toggleBlockUser(userId)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isBlocked)
        assertEquals(R.string.chat_info_msg_blocked, viewModel.uiState.value.userMessageRes)
    }

    @Test
    fun `toggleBlockUser sets error on block failure`() = runTest {
        val userId = "user_1"
        `when`(blockUserUseCase.invoke(userId)).thenReturn(Result.failure(RuntimeException("Failed")))

        viewModel.toggleBlockUser(userId)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBlocked)
        assertEquals(R.string.chat_info_error_block, viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `clearChat deletes conversation and invokes onCleared`() = runTest {
        val chatId = "chat_1"
        var cleared = false
        `when`(deleteConversationUseCase.invoke(chatId)).thenReturn(Result.success(Unit))

        viewModel.clearChat(chatId) {
            cleared = true
        }
        advanceUntilIdle()

        assertTrue(cleared)
        assertEquals(R.string.chat_info_msg_cleared, viewModel.uiState.value.userMessageRes)
    }

    @Test
    fun `clearChat sets error on clear failure`() = runTest {
        val chatId = "chat_1"
        var cleared = false
        `when`(deleteConversationUseCase.invoke(chatId)).thenReturn(Result.failure(RuntimeException("Failed")))

        viewModel.clearChat(chatId) {
            cleared = true
        }
        advanceUntilIdle()

        assertFalse(cleared)
        assertEquals(R.string.chat_info_error_clear_chat, viewModel.uiState.value.errorMessageRes)
    }
}
