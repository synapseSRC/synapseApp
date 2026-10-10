package com.synapse.social.studioasinc.feature.profile.personalinfo

import com.synapse.social.studioasinc.data.model.RelationshipStatus
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.domain.model.Gender
import com.synapse.social.studioasinc.domain.model.UserProfile
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalInformationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository = mock(EditProfileRepositoryImpl::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `formatLocaleDate correctly formats yyyy-MM-dd date string`() {
        val formatted = PersonalInformationViewModel.formatLocaleDate("2000-05-15")
        assertTrue(formatted.isNotBlank())
        assertFalse(formatted == "2000-05-15")
    }

    @Test
    fun `formatLocaleDate handles empty or blank string gracefully`() {
        val formatted = PersonalInformationViewModel.formatLocaleDate("")
        assertEquals("", formatted)
    }

    @Test
    fun `PronounsChanged event updates pronouns state and sets hasChanges to true`() = runTest {
        `when`(mockRepository.getCurrentUserId()).thenReturn(null)
        val viewModel = PersonalInformationViewModel(mockRepository)

        viewModel.onEvent(PersonalInformationEvent.PronounsChanged("they/them"))

        val state = viewModel.uiState.value
        assertEquals("they/them", state.pronouns)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `GenderSelected event updates gender state and sets hasChanges to true`() = runTest {
        `when`(mockRepository.getCurrentUserId()).thenReturn(null)
        val viewModel = PersonalInformationViewModel(mockRepository)

        viewModel.onEvent(PersonalInformationEvent.GenderSelected(Gender.Female))

        val state = viewModel.uiState.value
        assertEquals(Gender.Female, state.gender)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `RelationshipStatusSelected event updates relationship status state and sets hasChanges to true`() = runTest {
        `when`(mockRepository.getCurrentUserId()).thenReturn(null)
        val viewModel = PersonalInformationViewModel(mockRepository)

        viewModel.onEvent(PersonalInformationEvent.RelationshipStatusSelected(RelationshipStatus.SINGLE))

        val state = viewModel.uiState.value
        assertEquals(RelationshipStatus.SINGLE, state.relationshipStatus)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `BirthdayChanged event updates birthday state and sets hasChanges to true`() = runTest {
        `when`(mockRepository.getCurrentUserId()).thenReturn(null)
        val viewModel = PersonalInformationViewModel(mockRepository)

        viewModel.onEvent(PersonalInformationEvent.BirthdayChanged("1995-12-25"))

        val state = viewModel.uiState.value
        assertEquals("1995-12-25", state.birthday)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `loadProfile loads user profile data correctly`() = runTest {
        val userProfile = UserProfile(
            uid = "user-123",
            username = "testuser",
            pronouns = "she/her",
            birthday = "1998-03-20",
            gender = Gender.Female,
            relationshipStatus = "In a relationship"
        )
        `when`(mockRepository.getCurrentUserId()).thenReturn("user-123")
        `when`(mockRepository.getUserProfile("user-123")).thenReturn(flowOf(Result.success(userProfile)))

        val viewModel = PersonalInformationViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("she/her", state.pronouns)
        assertEquals("1998-03-20", state.birthday)
        assertEquals(Gender.Female, state.gender)
        assertEquals(RelationshipStatus.IN_A_RELATIONSHIP, state.relationshipStatus)
        assertFalse(state.hasChanges)
    }
}
