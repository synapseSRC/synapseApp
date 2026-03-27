package com.synapse.social.studioasinc.ui.settings

import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import com.synapse.social.studioasinc.shared.domain.usecase.settings.ObserveChatSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SyncChatSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChatSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var observeChatSettingsUseCase: ObserveChatSettingsUseCase
    private lateinit var syncChatSettingsUseCase: SyncChatSettingsUseCase
    private lateinit var viewModel: ChatSettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        observeChatSettingsUseCase = mock()
        syncChatSettingsUseCase = mock()

        // Mock state flows for observer
        whenever(observeChatSettingsUseCase.chatFontScale).thenReturn(MutableStateFlow(1.0f))
        whenever(observeChatSettingsUseCase.chatMessageCornerRadius).thenReturn(MutableStateFlow(16))
        whenever(observeChatSettingsUseCase.chatThemePreset).thenReturn(MutableStateFlow(ChatThemePreset.DEFAULT))
        whenever(observeChatSettingsUseCase.chatWallpaperType).thenReturn(MutableStateFlow(WallpaperType.DEFAULT))
        whenever(observeChatSettingsUseCase.chatWallpaperValue).thenReturn(MutableStateFlow(null))
        whenever(observeChatSettingsUseCase.chatWallpaperBlur).thenReturn(MutableStateFlow(0f))
        whenever(observeChatSettingsUseCase.chatListLayout).thenReturn(MutableStateFlow(ChatListLayout.DOUBLE_LINE))
        whenever(observeChatSettingsUseCase.chatSwipeGesture).thenReturn(MutableStateFlow(ChatSwipeGesture.ARCHIVE))
        whenever(observeChatSettingsUseCase.themeMode).thenReturn(MutableStateFlow(ThemeMode.SYSTEM))

        viewModel = ChatSettingsViewModel(observeChatSettingsUseCase, syncChatSettingsUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateChatFontScale calls sync use case`() = runTest {
        viewModel.updateChatFontScale(1.2f)
        testScheduler.advanceUntilIdle()
        verify(syncChatSettingsUseCase).updateFontScale(1.2f)
    }

    @Test
    fun `updateChatMessageCornerRadius calls sync use case`() = runTest {
        viewModel.updateChatMessageCornerRadius(8)
        testScheduler.advanceUntilIdle()
        verify(syncChatSettingsUseCase).updateMessageCornerRadius(8)
    }

    @Test
    fun `updateChatThemePreset calls sync use case`() = runTest {
        viewModel.updateChatThemePreset(ChatThemePreset.OCEAN)
        testScheduler.advanceUntilIdle()
        verify(syncChatSettingsUseCase).updateThemePreset(ChatThemePreset.OCEAN)
    }

    @Test
    fun `updateChatWallpaperType calls sync use case`() = runTest {
        viewModel.updateChatWallpaperType(WallpaperType.SOLID_COLOR)
        testScheduler.advanceUntilIdle()
        verify(syncChatSettingsUseCase).updateWallpaperType(WallpaperType.SOLID_COLOR)
    }

    @Test
    fun `updateChatListLayout calls sync use case`() = runTest {
        viewModel.updateChatListLayout(ChatListLayout.SINGLE_LINE)
        testScheduler.advanceUntilIdle()
        verify(syncChatSettingsUseCase).updateListLayout(ChatListLayout.SINGLE_LINE)
    }

    @Test
    fun `updateChatSwipeGesture calls sync use case`() = runTest {
        viewModel.updateChatSwipeGesture(ChatSwipeGesture.DELETE)
        testScheduler.advanceUntilIdle()
        verify(syncChatSettingsUseCase).updateSwipeGesture(ChatSwipeGesture.DELETE)
    }
}
