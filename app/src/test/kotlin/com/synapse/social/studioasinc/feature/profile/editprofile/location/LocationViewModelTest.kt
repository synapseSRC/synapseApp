package com.synapse.social.studioasinc.feature.profile.editprofile.location

import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: EditProfileRepositoryImpl = mock(EditProfileRepositoryImpl::class.java)
    private lateinit var viewModel: LocationViewModel

    private val testUser = UserProfile(
        uid = "user123",
        username = "johndoe",
        region = "Canada",
        currentCity = "Toronto",
        hometown = "Montreal"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProfile success updates state with profile location details`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))

        viewModel = LocationViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Canada", state.selectedRegion)
        assertEquals("Toronto", state.currentCity)
        assertEquals("Montreal", state.hometown)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `editing fields sets hasChanges to true`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))

        viewModel = LocationViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(LocationEvent.CurrentCityChanged("Vancouver"))

        val state = viewModel.uiState.value
        assertEquals("Vancouver", state.currentCity)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `saveProfile success updates initial values and emits NavigateBack`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))
        whenever(repository.updateProfile(any(), any())).thenReturn(Result.success(Unit))

        viewModel = LocationViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(LocationEvent.CurrentCityChanged("Vancouver"))
        viewModel.onEvent(LocationEvent.SaveClicked)
        advanceUntilIdle()

        verify(repository).updateProfile(
            eq("user123"),
            argThat {
                get("current_city") == "Vancouver" &&
                get("region") == "Canada" &&
                get("hometown") == "Montreal"
            }
        )

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("Vancouver", state.currentCity)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `saveProfile failure displays error and allows dismissal`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))
        whenever(repository.updateProfile(any(), any())).thenReturn(Result.failure(Exception("Save failed")))

        viewModel = LocationViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(LocationEvent.CurrentCityChanged("Ottawa"))
        viewModel.onEvent(LocationEvent.SaveClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("Save failed", state.error)

        viewModel.onEvent(LocationEvent.DismissError)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `SelectRegionClicked emits NavigateToRegionSelection event`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))

        viewModel = LocationViewModel(repository)
        advanceUntilIdle()

        val navEvents = mutableListOf<LocationNavigation>()
        val job = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigationEvents.collect { navEvents.add(it) }
        }

        viewModel.onEvent(LocationEvent.SelectRegionClicked)
        advanceUntilIdle()

        assertEquals(1, navEvents.size)
        assertEquals("Canada", (navEvents.first() as LocationNavigation.NavigateToRegionSelection).currentRegion)

        job.cancel()
    }

    @Test
    fun `BackClicked emits NavigateBack event`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))

        viewModel = LocationViewModel(repository)
        advanceUntilIdle()

        val navEvents = mutableListOf<LocationNavigation>()
        val job = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigationEvents.collect { navEvents.add(it) }
        }

        viewModel.onEvent(LocationEvent.BackClicked)
        advanceUntilIdle()

        assertEquals(1, navEvents.size)
        assertTrue(navEvents.first() is LocationNavigation.NavigateBack)

        job.cancel()
    }
}
