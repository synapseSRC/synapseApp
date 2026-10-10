package com.synapse.social.studioasinc.feature.profile.editprofile.appearance

import android.app.Application
import android.net.Uri
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileAppearanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val application = mock(Application::class.java)
    private val repository = mock(EditProfileRepositoryImpl::class.java)
    private val settingsRepository = mock(SettingsRepository::class.java)
    private val mediaCompressor = mock(MediaCompressor::class.java)

    private val mockUserId = "test-user-456"
    private val mockProfile = UserProfile(
        uid = mockUserId,
        username = "janedoe",
        displayName = "Jane Doe",
        bio = "Hello world!",
        avatar = "https://example.com/avatar.jpg",
        profileCoverImage = "https://example.com/cover.jpg"
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
    fun `loadProfileAppearance loads profile cover and avatar successfully`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))

        val viewModel = ProfileAppearanceViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("https://example.com/avatar.jpg", state.avatarUrl)
        assertEquals("https://example.com/cover.jpg", state.coverUrl)
        assertEquals("Jane Doe", state.profile?.displayName)
    }

    @Test
    fun `CoverCropped updates pendingCoverUri and hasPendingChanges`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))

        val viewModel = ProfileAppearanceViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        val mockUri = mock(Uri::class.java)
        viewModel.onEvent(ProfileAppearanceEvent.CoverCropped(mockUri))

        val state = viewModel.uiState.value
        assertEquals(mockUri, state.pendingCoverUri)
        assertTrue(state.hasPendingChanges)
    }

    @Test
    fun `ResetChanges clears draft changes`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))

        val viewModel = ProfileAppearanceViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        val mockUri = mock(Uri::class.java)
        viewModel.onEvent(ProfileAppearanceEvent.CoverCropped(mockUri))
        assertTrue(viewModel.uiState.value.hasPendingChanges)

        viewModel.onEvent(ProfileAppearanceEvent.ResetChanges)
        val state = viewModel.uiState.value
        assertNull(state.pendingCoverUri)
        assertFalse(state.hasPendingChanges)
    }

    @Test
    fun `RequestRemoveCover opens remove cover dialog`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))

        val viewModel = ProfileAppearanceViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ProfileAppearanceEvent.RequestRemoveCover)

        assertTrue(viewModel.uiState.value.showRemoveCoverDialog)
    }

    @Test
    fun `ConfirmRemoveCover removes cover via repository and updates UI state`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))
        `when`(repository.updateProfile(mockUserId, mapOf("profile_cover_image" to null))).thenReturn(Result.success(Unit))

        val viewModel = ProfileAppearanceViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ProfileAppearanceEvent.ConfirmRemoveCover)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showRemoveCoverDialog)
        assertNull(state.coverUrl)
        verify(repository).updateProfile(mockUserId, mapOf("profile_cover_image" to null))
    }

    @Test
    fun `BackClicked with pending changes triggers showUnsavedChangesDialog`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))

        val viewModel = ProfileAppearanceViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        val mockUri = mock(Uri::class.java)
        viewModel.onEvent(ProfileAppearanceEvent.CoverCropped(mockUri))

        viewModel.onEvent(ProfileAppearanceEvent.BackClicked)

        assertTrue(viewModel.uiState.value.showUnsavedChangesDialog)
    }

    @Test
    fun `ConfirmDiscardUnsaved discards pending changes and emits NavigateBack`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))

        val viewModel = ProfileAppearanceViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        val mockUri = mock(Uri::class.java)
        viewModel.onEvent(ProfileAppearanceEvent.CoverCropped(mockUri))

        viewModel.onEvent(ProfileAppearanceEvent.ConfirmDiscardUnsaved)

        val state = viewModel.uiState.value
        assertFalse(state.showUnsavedChangesDialog)
        assertNull(state.pendingCoverUri)
        assertFalse(state.hasPendingChanges)

        val navEvent = viewModel.navigationEvents.first()
        assertEquals(ProfileAppearanceNavigation.NavigateBack, navEvent)
    }
}
