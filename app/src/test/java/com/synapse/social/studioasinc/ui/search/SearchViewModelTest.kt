package com.synapse.social.studioasinc.ui.search

import android.content.SharedPreferences
import com.synapse.social.studioasinc.domain.usecase.post.BookmarkPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.PopulatePostPollsUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ReactToPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ReportPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.RevokeVoteUseCase
import com.synapse.social.studioasinc.domain.usecase.post.VotePollUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.FollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.UnfollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.reaction.PopulatePostReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizePostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.TogglePostCommentsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.GetSuggestedAccountsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchHashtagsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchNewsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchPostsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var searchPostsUseCase: SearchPostsUseCase
    @Mock
    private lateinit var searchHashtagsUseCase: SearchHashtagsUseCase
    @Mock
    private lateinit var searchNewsUseCase: SearchNewsUseCase
    @Mock
    private lateinit var getSuggestedAccountsUseCase: GetSuggestedAccountsUseCase
    @Mock
    private lateinit var followUserUseCase: FollowUserUseCase
    @Mock
    private lateinit var unfollowUserUseCase: UnfollowUserUseCase
    @Mock
    private lateinit var blockUserUseCase: BlockUserUseCase
    @Mock
    private lateinit var authRepository: AuthRepository
    @Mock
    private lateinit var settingsDataStore: com.synapse.social.studioasinc.data.local.database.SettingsDataStore
    @Mock
    private lateinit var reactToPostUseCase: ReactToPostUseCase
    @Mock
    private lateinit var bookmarkPostUseCase: BookmarkPostUseCase
    @Mock
    private lateinit var votePollUseCase: VotePollUseCase
    @Mock
    private lateinit var revokeVoteUseCase: RevokeVoteUseCase
    @Mock
    private lateinit var deletePostUseCase: DeletePostUseCase
    @Mock
    private lateinit var togglePostCommentsUseCase: TogglePostCommentsUseCase
    @Mock
    private lateinit var reportPostUseCase: ReportPostUseCase
    @Mock
    private lateinit var populatePostPollsUseCase: PopulatePostPollsUseCase
    @Mock
    private lateinit var populatePostReactionsUseCase: PopulatePostReactionsUseCase
    @Mock
    private lateinit var summarizePostUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizePostUseCase

    private lateinit var viewModel: SearchViewModel
    private val HISTORY_KEY = "search_history"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = SearchViewModel(
            searchPostsUseCase,
            searchHashtagsUseCase,
            searchNewsUseCase,
            getSuggestedAccountsUseCase,
            followUserUseCase,
            unfollowUserUseCase,
            blockUserUseCase,
            authRepository,
            settingsDataStore,
            reactToPostUseCase,
            bookmarkPostUseCase,
            votePollUseCase,
            revokeVoteUseCase,
            deletePostUseCase,
            togglePostCommentsUseCase,
            reportPostUseCase,
            populatePostPollsUseCase,
            populatePostReactionsUseCase,
            summarizePostUseCase
        )
    }

    @Test
    fun `loadHistory when settingsDataStore returns empty list sets empty searchHistory`() = runTest {
        `when`(settingsDataStore.searchHistory).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        createViewModel()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchHistory.isEmpty())
    }

    @Test
    fun `loadHistory when settingsDataStore returns list sets correct searchHistory`() = runTest {
        `when`(settingsDataStore.searchHistory).thenReturn(kotlinx.coroutines.flow.flowOf(listOf("kotlin","android","testing")))

        createViewModel()
        testScheduler.advanceUntilIdle()

        val expected = listOf("kotlin", "android", "testing")
        assertEquals(expected, viewModel.uiState.value.searchHistory)
    }

    @Test
    fun `addToHistory prepends new query to existing history and updates settingsDataStore`() = runTest {
        `when`(settingsDataStore.searchHistory).thenReturn(kotlinx.coroutines.flow.flowOf(listOf("existing1", "existing2")))

        createViewModel()
        testScheduler.advanceUntilIdle()

        `when`(searchPostsUseCase(org.mockito.Mockito.anyString())).thenReturn(Result.success(emptyList()))

        viewModel.onQueryChange("newQuery")
        viewModel.addToHistory("newQuery")
        testScheduler.advanceUntilIdle()

        val expectedHistory = listOf("newQuery", "existing1", "existing2")
        assertEquals(expectedHistory, viewModel.uiState.value.searchHistory)
        org.mockito.kotlin.verify(settingsDataStore).setSearchHistory(expectedHistory)
    }

    @Test
    fun `removeFromHistory removes item and updates settingsDataStore`() = runTest {
        `when`(settingsDataStore.searchHistory).thenReturn(kotlinx.coroutines.flow.flowOf(listOf("Kotlin", "Android")))

        createViewModel()
        testScheduler.advanceUntilIdle()
        viewModel.removeFromHistory("Kotlin")
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("Android"), viewModel.uiState.value.searchHistory)
        org.mockito.kotlin.verify(settingsDataStore).setSearchHistory(listOf("Android"))
    }

    @Test
    fun `clearHistory clears list and updates settingsDataStore`() = runTest {
        `when`(settingsDataStore.searchHistory).thenReturn(kotlinx.coroutines.flow.flowOf(listOf("Kotlin", "Android")))

        createViewModel()
        testScheduler.advanceUntilIdle()
        viewModel.clearHistory()
        testScheduler.advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.uiState.value.searchHistory)
        org.mockito.kotlin.verify(settingsDataStore).setSearchHistory(emptyList())
    }
}
