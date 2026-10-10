package com.synapse.social.studioasinc.feature.profile.editprofile.interestsskills

import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.domain.usecase.profile.GetInterestsSkillsUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.UpdateInterestsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@OptIn(ExperimentalCoroutinesApi::class)
class InterestsSkillsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mock(EditProfileRepositoryImpl::class.java)
    private lateinit var getInterestsSkillsUseCase: GetInterestsSkillsUseCase
    private lateinit var updateInterestsUseCase: UpdateInterestsUseCase
    private lateinit var viewModel: InterestsSkillsViewModel

    private val mockProfile = UserProfile(
        uid = "user-123",
        username = "testuser",
        interests = listOf("Coding", "Music", "Reading")
    )

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        `when`(repository.getCurrentUserId()).thenReturn("user-123")
        `when`(repository.getUserProfile("user-123")).thenReturn(flowOf(Result.success(mockProfile)))

        getInterestsSkillsUseCase = GetInterestsSkillsUseCase(repository)
        updateInterestsUseCase = UpdateInterestsUseCase(repository)

        viewModel = InterestsSkillsViewModel(getInterestsSkillsUseCase, updateInterestsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProfile loads existing interests into UI state`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(listOf("Coding", "Music", "Reading"), state.interests)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `addInterest adds new interest and normalizes whitespace`() {
        viewModel.onEvent(InterestsSkillsEvent.InterestInputChanged("   Gaming   "))
        viewModel.onEvent(InterestsSkillsEvent.AddInterest)

        val state = viewModel.uiState.value
        assertEquals(listOf("Coding", "Music", "Reading", "Gaming"), state.interests)
        assertEquals("", state.pendingInterestInput)
        assertNull(state.inputError)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `addInterest rejects blank input`() {
        viewModel.onEvent(InterestsSkillsEvent.InterestInputChanged("   "))
        viewModel.onEvent(InterestsSkillsEvent.AddInterest)

        val state = viewModel.uiState.value
        assertEquals(listOf("Coding", "Music", "Reading"), state.interests)
        assertEquals("Interest cannot be empty", state.inputError)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `addInterest rejects duplicate interest case-insensitively`() {
        viewModel.onEvent(InterestsSkillsEvent.InterestInputChanged("coding"))
        viewModel.onEvent(InterestsSkillsEvent.AddInterest)

        val state = viewModel.uiState.value
        assertEquals(listOf("Coding", "Music", "Reading"), state.interests)
        assertEquals("Interest already exists", state.inputError)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `removeInterest removes interest at valid index`() {
        viewModel.onEvent(InterestsSkillsEvent.RemoveInterest(1)) // Remove "Music"

        val state = viewModel.uiState.value
        assertEquals(listOf("Coding", "Reading"), state.interests)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `removeInterest ignores invalid index`() {
        viewModel.onEvent(InterestsSkillsEvent.RemoveInterest(99))

        val state = viewModel.uiState.value
        assertEquals(listOf("Coding", "Music", "Reading"), state.interests)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `moveInterest reorders items correctly`() {
        viewModel.onEvent(InterestsSkillsEvent.MoveInterest(0, 2)) // Move "Coding" to end

        val state = viewModel.uiState.value
        assertEquals(listOf("Music", "Reading", "Coding"), state.interests)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `saveClicked updates profile when changes exist`() = runTest {
        `when`(repository.updateProfile(eq("user-123"), any())).thenReturn(Result.success(Unit))

        viewModel.onEvent(InterestsSkillsEvent.InterestInputChanged("Travel"))
        viewModel.onEvent(InterestsSkillsEvent.AddInterest)
        viewModel.onEvent(InterestsSkillsEvent.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.hasChanges)
        assertEquals("Interests saved successfully", state.successMessage)
    }
}
