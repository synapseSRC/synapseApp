package com.synapse.social.studioasinc.ui.home

import com.synapse.social.studioasinc.shared.domain.usecase.user.GetCurrentUserAvatarUseCase
import com.synapse.social.studioasinc.ui.settings.FakeSettingsRepository
import com.synapse.social.studioasinc.ui.settings.NavigationPlacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSettingsRepository
    private lateinit var getCurrentUserAvatarUseCase: GetCurrentUserAvatarUseCase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSettingsRepository()
        getCurrentUserAvatarUseCase = mock()
        viewModel = HomeViewModel(getCurrentUserAvatarUseCase, fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun navigationPlacement_exposesDefaultTopPlacement() = runTest {
        assertEquals(NavigationPlacement.TOP, viewModel.navigationPlacement.value)
    }

    @Test
    fun navigationPlacement_reactsToRepositoryChanges() = runTest {
        backgroundScope.launch { viewModel.navigationPlacement.collect {} }
        fakeRepository.setNavigationPlacement(NavigationPlacement.BOTTOM)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(NavigationPlacement.BOTTOM, viewModel.navigationPlacement.value)
    }
}
