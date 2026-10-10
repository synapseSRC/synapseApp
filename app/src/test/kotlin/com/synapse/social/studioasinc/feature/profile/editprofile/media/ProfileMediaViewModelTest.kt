package com.synapse.social.studioasinc.feature.profile.editprofile.media

import android.app.Application
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileMediaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val application = mock(Application::class.java)
    private val repository = mock(EditProfileRepositoryImpl::class.java)
    private val settingsRepository = mock(SettingsRepository::class.java)
    private val mediaCompressor = mock(MediaCompressor::class.java)

    private val mockUserId = "test-user-123"
    private val mockProfile = UserProfile(
        uid = mockUserId,
        username = "johndoe",
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
    fun `loadProfileMedia loads avatar and cover urls successfully`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))

        val viewModel = ProfileMediaViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("https://example.com/avatar.jpg", state.avatarUrl)
        assertEquals("https://example.com/cover.jpg", state.coverUrl)
    }

    @Test
    fun `RequestRemoveAvatar opens remove avatar dialog`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))

        val viewModel = ProfileMediaViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ProfileMediaEvent.RequestRemoveAvatar)

        assertTrue(viewModel.uiState.value.showRemoveAvatarDialog)
    }

    @Test
    fun `ConfirmRemoveAvatar removes avatar via repository and updates UI state`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))
        `when`(repository.updateProfile(mockUserId, mapOf("avatar" to null))).thenReturn(Result.success(Unit))

        val viewModel = ProfileMediaViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ProfileMediaEvent.ConfirmRemoveAvatar)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showRemoveAvatarDialog)
        assertNull(state.avatarUrl)
        verify(repository).updateProfile(mockUserId, mapOf("avatar" to null))
    }

    @Test
    fun `ConfirmRemoveCover removes cover via repository and updates UI state`() = runTest {
        `when`(repository.getCurrentUserId()).thenReturn(mockUserId)
        `when`(repository.getUserProfile(mockUserId)).thenReturn(flowOf(Result.success(mockProfile)))
        `when`(repository.updateProfile(mockUserId, mapOf("profile_cover_image" to null))).thenReturn(Result.success(Unit))

        val viewModel = ProfileMediaViewModel(application, repository, settingsRepository, mediaCompressor)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ProfileMediaEvent.ConfirmRemoveCover)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showRemoveCoverDialog)
        assertNull(state.coverUrl)
        verify(repository).updateProfile(mockUserId, mapOf("profile_cover_image" to null))
    }
}
