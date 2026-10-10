package com.synapse.social.studioasinc.feature.profile.editprofile.workeducation

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
class WorkEducationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: EditProfileRepositoryImpl = mock(EditProfileRepositoryImpl::class.java)
    private lateinit var viewModel: WorkEducationViewModel

    private val testUser = UserProfile(
        uid = "user123",
        username = "johndoe",
        occupation = "Software Engineer",
        workplace = "Tech Corp",
        education = listOf("Stanford University", "MIT")
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
    fun `loadProfile loads user data and populates UI state`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))

        viewModel = WorkEducationViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Software Engineer", state.occupation)
        assertEquals("Tech Corp", state.workplace)
        assertEquals("Stanford University, MIT", state.education)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `OccupationChanged updates occupation and sets hasChanges to true`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))

        viewModel = WorkEducationViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(WorkEducationEvent.OccupationChanged("Staff Architect"))

        val state = viewModel.uiState.value
        assertEquals("Staff Architect", state.occupation)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `SaveClicked invokes repository updateProfile with sanitized data`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))
        whenever(repository.updateProfile(any(), any())).thenReturn(Result.success(Unit))

        viewModel = WorkEducationViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(WorkEducationEvent.OccupationChanged("Lead Engineer"))
        viewModel.onEvent(WorkEducationEvent.WorkplaceChanged("Acme Inc"))
        viewModel.onEvent(WorkEducationEvent.EducationChanged("Harvard, MIT"))

        viewModel.onEvent(WorkEducationEvent.SaveClicked)
        advanceUntilIdle()

        verify(repository).updateProfile(
            eq("user123"),
            argThat {
                get("occupation") == "Lead Engineer" &&
                        get("workplace") == "Acme Inc" &&
                        get("education") == listOf("Harvard", "MIT")
            }
        )

        val state = viewModel.uiState.value
        assertFalse(state.hasChanges)
        assertFalse(state.isSaving)
    }

    @Test
    fun `clearing fields updates repository with null or empty list`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))
        whenever(repository.updateProfile(any(), any())).thenReturn(Result.success(Unit))

        viewModel = WorkEducationViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(WorkEducationEvent.OccupationChanged(""))
        viewModel.onEvent(WorkEducationEvent.WorkplaceChanged("   "))
        viewModel.onEvent(WorkEducationEvent.EducationChanged(""))

        viewModel.onEvent(WorkEducationEvent.SaveClicked)
        advanceUntilIdle()

        verify(repository).updateProfile(
            eq("user123"),
            argThat {
                get("occupation") == null &&
                        get("workplace") == null &&
                        get("education") == emptyList<String>()
            }
        )
    }

    @Test
    fun `SaveClicked handles error failure gracefully`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))
        whenever(repository.updateProfile(any(), any())).thenReturn(Result.failure(Exception("Network error")))

        viewModel = WorkEducationViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(WorkEducationEvent.OccupationChanged("New Role"))
        viewModel.onEvent(WorkEducationEvent.SaveClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("Network error", state.error)

        viewModel.onEvent(WorkEducationEvent.DismissError)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `BackClicked emits NavigateBack event`() = runTest {
        whenever(repository.getCurrentUserId()).thenReturn("user123")
        whenever(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(testUser)))

        viewModel = WorkEducationViewModel(repository)
        advanceUntilIdle()

        val navEvents = mutableListOf<WorkEducationNavigation>()
        val job = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigationEvents.collect { navEvents.add(it) }
        }

        viewModel.onEvent(WorkEducationEvent.BackClicked)
        advanceUntilIdle()

        assertEquals(1, navEvents.size)
        assertTrue(navEvents.first() is WorkEducationNavigation.NavigateBack)

        job.cancel()
    }
}