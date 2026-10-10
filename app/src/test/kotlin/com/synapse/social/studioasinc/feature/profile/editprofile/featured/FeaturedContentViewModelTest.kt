package com.synapse.social.studioasinc.feature.profile.editprofile.featured

import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.data.repository.PostRepositoryImpl
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FeaturedContentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val editProfileRepository: EditProfileRepositoryImpl = mock(EditProfileRepositoryImpl::class.java)
    private val postRepository: PostRepositoryImpl = mock(PostRepositoryImpl::class.java)
    private lateinit var viewModel: FeaturedContentViewModel

    private val testPosts = listOf(
        Post(
            id = "post1",
            authorUid = "user123",
            postText = "First featured post text",
            mediaItems = mutableListOf(MediaItem(id = "m1", url = "https://example.com/image1.jpg", type = MediaType.IMAGE))
        ),
        Post(
            id = "post2",
            authorUid = "user123",
            postText = "Second post text",
            mediaItems = mutableListOf(MediaItem(id = "m2", url = "https://example.com/video1.mp4", type = MediaType.VIDEO))
        ),
        Post(
            id = "post_other_user",
            authorUid = "other_user",
            postText = "Other user post"
        )
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
    fun `loadUserContent loads user posts and filters owned eligible content`() = runTest {
        whenever(editProfileRepository.getCurrentUserId()).thenReturn("user123")
        whenever(postRepository.getUserPosts("user123")).thenReturn(Result.success(testPosts))

        viewModel = FeaturedContentViewModel(editProfileRepository, postRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("user123", state.currentUserId)
        assertEquals(2, state.candidatePosts.size)
        assertEquals("post1", state.candidatePosts[0].id)
        assertEquals("post2", state.candidatePosts[1].id)
        assertEquals(2, state.candidateMedia.size)
        assertNull(state.error)
    }

    @Test
    fun `TogglePostSelected updates selection state and sets hasPendingChanges`() = runTest {
        whenever(editProfileRepository.getCurrentUserId()).thenReturn("user123")
        whenever(postRepository.getUserPosts("user123")).thenReturn(Result.success(testPosts))

        viewModel = FeaturedContentViewModel(editProfileRepository, postRepository)
        advanceUntilIdle()

        viewModel.onEvent(FeaturedContentEvent.TogglePostSelected("post1"))
        var state = viewModel.uiState.value
        assertEquals(listOf("post1"), state.selectedPostIds)
        assertEquals(1, state.selectedPosts.size)
        assertTrue(state.hasPendingChanges)

        viewModel.onEvent(FeaturedContentEvent.TogglePostSelected("post1"))
        state = viewModel.uiState.value
        assertTrue(state.selectedPostIds.isEmpty())
    }

    @Test
    fun `ToggleMediaSelected updates media selection state`() = runTest {
        whenever(editProfileRepository.getCurrentUserId()).thenReturn("user123")
        whenever(postRepository.getUserPosts("user123")).thenReturn(Result.success(testPosts))

        viewModel = FeaturedContentViewModel(editProfileRepository, postRepository)
        advanceUntilIdle()

        val mediaUrl = "https://example.com/image1.jpg"
        viewModel.onEvent(FeaturedContentEvent.ToggleMediaSelected(mediaUrl))
        val state = viewModel.uiState.value
        assertEquals(listOf(mediaUrl), state.selectedMediaUrls)
        assertEquals(1, state.selectedMedia.size)
        assertTrue(state.hasPendingChanges)
    }

    @Test
    fun `MovePostOrderUp and MovePostOrderDown reorder selected posts`() = runTest {
        whenever(editProfileRepository.getCurrentUserId()).thenReturn("user123")
        whenever(postRepository.getUserPosts("user123")).thenReturn(Result.success(testPosts))

        viewModel = FeaturedContentViewModel(editProfileRepository, postRepository)
        advanceUntilIdle()

        viewModel.onEvent(FeaturedContentEvent.TogglePostSelected("post1"))
        viewModel.onEvent(FeaturedContentEvent.TogglePostSelected("post2"))
        assertEquals(listOf("post1", "post2"), viewModel.uiState.value.selectedPostIds)

        viewModel.onEvent(FeaturedContentEvent.MovePostOrderDown("post1"))
        assertEquals(listOf("post2", "post1"), viewModel.uiState.value.selectedPostIds)

        viewModel.onEvent(FeaturedContentEvent.MovePostOrderUp("post1"))
        assertEquals(listOf("post1", "post2"), viewModel.uiState.value.selectedPostIds)
    }

    @Test
    fun `ClearAllSelected clears selected posts and media`() = runTest {
        whenever(editProfileRepository.getCurrentUserId()).thenReturn("user123")
        whenever(postRepository.getUserPosts("user123")).thenReturn(Result.success(testPosts))

        viewModel = FeaturedContentViewModel(editProfileRepository, postRepository)
        advanceUntilIdle()

        viewModel.onEvent(FeaturedContentEvent.TogglePostSelected("post1"))
        viewModel.onEvent(FeaturedContentEvent.ToggleMediaSelected("https://example.com/image1.jpg"))
        assertTrue(viewModel.uiState.value.hasSelectedContent)

        viewModel.onEvent(FeaturedContentEvent.ClearAllSelected)
        assertFalse(viewModel.uiState.value.hasSelectedContent)
    }

    @Test
    fun `DismissCapabilityNotice updates state`() = runTest {
        whenever(editProfileRepository.getCurrentUserId()).thenReturn("user123")
        whenever(postRepository.getUserPosts("user123")).thenReturn(Result.success(testPosts))

        viewModel = FeaturedContentViewModel(editProfileRepository, postRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showBackendCapabilityNotice)
        viewModel.onEvent(FeaturedContentEvent.DismissCapabilityNotice)
        assertFalse(viewModel.uiState.value.showBackendCapabilityNotice)
    }

    @Test
    fun `BackClicked emits NavigateBack event`() = runTest {
        whenever(editProfileRepository.getCurrentUserId()).thenReturn("user123")
        whenever(postRepository.getUserPosts("user123")).thenReturn(Result.success(testPosts))

        viewModel = FeaturedContentViewModel(editProfileRepository, postRepository)
        advanceUntilIdle()

        val navEvents = mutableListOf<FeaturedContentNavigation>()
        val job = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigationEvents.collect { navEvents.add(it) }
        }

        viewModel.onEvent(FeaturedContentEvent.BackClicked)
        advanceUntilIdle()

        assertEquals(1, navEvents.size)
        assertTrue(navEvents.first() is FeaturedContentNavigation.NavigateBack)

        job.cancel()
    }
}
