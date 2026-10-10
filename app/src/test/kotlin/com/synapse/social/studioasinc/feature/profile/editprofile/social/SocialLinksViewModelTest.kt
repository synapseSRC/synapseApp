package com.synapse.social.studioasinc.feature.profile.editprofile.social

import android.app.Application
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.presentation.editprofile.social.SocialLinksEvent
import com.synapse.social.studioasinc.presentation.editprofile.social.SocialLinksNavigation
import com.synapse.social.studioasinc.presentation.editprofile.social.SocialLinksViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class SocialLinksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var repository: EditProfileRepositoryImpl

    @Captor
    private lateinit var updateMapCaptor: ArgumentCaptor<Map<String, Any?>>

    private lateinit var viewModel: SocialLinksViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProfile loads existing social links into UI state`() = runTest {
        val dummyProfile = UserProfile(
            uid = "user123",
            username = "johndoe",
            githubProfile = "https://github.com/johndoe",
            discordTag = "johndoe#1234",
            personalWebsite = "https://johndoe.com",
            publicEmail = "john@example.com"
        )

        `when`(repository.getCurrentUserId()).thenReturn("user123")
        `when`(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(dummyProfile)))

        viewModel = SocialLinksViewModel(application, repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("https://github.com/johndoe", state.githubProfile)
        assertEquals("johndoe#1234", state.discordTag)
        assertEquals("https://johndoe.com", state.personalWebsite)
        assertEquals("john@example.com", state.publicEmail)
        assertFalse(state.hasChanges)
        assertNull(state.error)
    }

    @Test
    fun `changing field updates UI state and marks hasChanges`() = runTest {
        val dummyProfile = UserProfile(
            uid = "user123",
            username = "johndoe"
        )
        `when`(repository.getCurrentUserId()).thenReturn("user123")
        `when`(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(dummyProfile)))

        viewModel = SocialLinksViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onEvent(SocialLinksEvent.GithubProfileChanged("octocat"))

        val state = viewModel.uiState.value
        assertEquals("octocat", state.githubProfile)
        assertTrue(state.hasChanges)
        assertNull(state.githubError)
    }

    @Test
    fun `validating email format produces error on invalid format`() = runTest {
        val dummyProfile = UserProfile(uid = "user123", username = "johndoe")
        `when`(repository.getCurrentUserId()).thenReturn("user123")
        `when`(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(dummyProfile)))

        viewModel = SocialLinksViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onEvent(SocialLinksEvent.PublicEmailChanged("invalid-email"))

        val state = viewModel.uiState.value
        assertEquals("Invalid email address format", state.publicEmailError)
    }

    @Test
    fun `saveSocialLinks normalizes GitHub and website URLs and persists through repository`() = runTest {
        val dummyProfile = UserProfile(uid = "user123", username = "johndoe")
        `when`(repository.getCurrentUserId()).thenReturn("user123")
        `when`(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(dummyProfile)))
        `when`(repository.updateProfile(org.mockito.kotlin.eq("user123"), org.mockito.kotlin.any())).thenReturn(Result.success(Unit))

        viewModel = SocialLinksViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onEvent(SocialLinksEvent.GithubProfileChanged("octocat"))
        viewModel.onEvent(SocialLinksEvent.PersonalWebsiteChanged("example.com"))
        viewModel.onEvent(SocialLinksEvent.DiscordTagChanged("user#0001"))
        viewModel.onEvent(SocialLinksEvent.PublicEmailChanged("public@example.com"))

        val navEvents = mutableListOf<SocialLinksNavigation>()
        val job = launch { viewModel.navigationEvents.collect { navEvents.add(it) } }

        viewModel.onEvent(SocialLinksEvent.SaveClicked)
        advanceUntilIdle()

        verify(repository).updateProfile(org.mockito.kotlin.eq("user123"), updateMapCaptor.capture())
        val updateMap = updateMapCaptor.value

        assertEquals("https://github.com/octocat", updateMap["github_profile"])
        assertEquals("https://example.com", updateMap["personal_website"])
        assertEquals("user#0001", updateMap["discord_tag"])
        assertEquals("public@example.com", updateMap["public_email"])

        assertFalse(viewModel.uiState.value.hasChanges)
        assertEquals(1, navEvents.size)
        assertEquals(SocialLinksNavigation.NavigateBack, navEvents.first())

        job.cancel()
    }

    @Test
    fun `clearing a field persists null to repository`() = runTest {
        val dummyProfile = UserProfile(
            uid = "user123",
            username = "johndoe",
            githubProfile = "https://github.com/johndoe",
            discordTag = "tag#123"
        )
        `when`(repository.getCurrentUserId()).thenReturn("user123")
        `when`(repository.getUserProfile("user123")).thenReturn(flowOf(Result.success(dummyProfile)))
        `when`(repository.updateProfile(org.mockito.kotlin.eq("user123"), org.mockito.kotlin.any())).thenReturn(Result.success(Unit))

        viewModel = SocialLinksViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onEvent(SocialLinksEvent.GithubProfileChanged(""))
        viewModel.onEvent(SocialLinksEvent.DiscordTagChanged(""))

        viewModel.onEvent(SocialLinksEvent.SaveClicked)
        advanceUntilIdle()

        verify(repository).updateProfile(org.mockito.kotlin.eq("user123"), updateMapCaptor.capture())
        val updateMap = updateMapCaptor.value

        assertNull(updateMap["github_profile"])
        assertNull(updateMap["discord_tag"])
    }
}
