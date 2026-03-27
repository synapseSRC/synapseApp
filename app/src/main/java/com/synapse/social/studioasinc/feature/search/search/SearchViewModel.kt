package com.synapse.social.studioasinc.ui.search
import com.synapse.social.studioasinc.data.local.database.SettingsDataStore
import com.synapse.social.studioasinc.feature.shared.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.usecase.profile.FollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.UnfollowUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.shared.domain.model.SearchAccount
import com.synapse.social.studioasinc.shared.domain.model.SearchHashtag
import com.synapse.social.studioasinc.shared.domain.model.SearchNews
import com.synapse.social.studioasinc.shared.domain.usecase.search.GetSuggestedAccountsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchHashtagsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchNewsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchPostsUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchTab(val title: String) {
    FOR_YOU("For you"),
    TRENDING("Trending"),
    NEWS("News"),
    SPORTS("Sports"),
    ENTERTAINMENT("Entertainment"),
    TOP("Top"),
    LATEST("Latest"),
    PEOPLE("People"),
    MEDIA("Media"),
    LISTS("Lists")
}

data class SearchUiState(
    val query: String = "",
    val active: Boolean = false,
    val isLoading: Boolean = false,
    val selectedTab: SearchTab = SearchTab.FOR_YOU,
    val error: String? = null,
    val posts: List<com.synapse.social.studioasinc.domain.model.Post> = emptyList(),
    val hashtags: List<SearchHashtag> = emptyList(),
    val news: List<SearchNews> = emptyList(),
    val accounts: List<SearchAccount> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val lastQuery: String = "",
    val cachedData: Map<SearchTab, Any> = emptyMap(),
    val blockSuccess: Boolean = false,
    val blockError: String? = null,
    val isSummarizing: Boolean = false,
    val postSummary: String? = null,
    val summaryError: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchPostsUseCase: SearchPostsUseCase,
    private val searchHashtagsUseCase: SearchHashtagsUseCase,
    private val searchNewsUseCase: SearchNewsUseCase,
    private val getSuggestedAccountsUseCase: GetSuggestedAccountsUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val authRepository: AuthRepository,
    private val settingsDataStore: com.synapse.social.studioasinc.data.local.database.SettingsDataStore,
    private val reactToPostUseCase: com.synapse.social.studioasinc.domain.usecase.post.ReactToPostUseCase,
    private val bookmarkPostUseCase: com.synapse.social.studioasinc.domain.usecase.post.BookmarkPostUseCase,
    private val votePollUseCase: com.synapse.social.studioasinc.domain.usecase.post.VotePollUseCase,
    private val revokeVoteUseCase: com.synapse.social.studioasinc.domain.usecase.post.RevokeVoteUseCase,
    private val deletePostUseCase: com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase,
    private val togglePostCommentsUseCase: com.synapse.social.studioasinc.shared.domain.usecase.post.TogglePostCommentsUseCase,
    private val reportPostUseCase: com.synapse.social.studioasinc.domain.usecase.post.ReportPostUseCase,
    private val populatePostPollsUseCase: com.synapse.social.studioasinc.domain.usecase.post.PopulatePostPollsUseCase,
    private val populatePostReactionsUseCase: com.synapse.social.studioasinc.domain.usecase.reaction.PopulatePostReactionsUseCase,
    private val summarizePostUseCase: SummarizePostUseCase
) : BaseViewModel<SearchUiState>(SearchUiState()) {
    private var searchJob: Job? = null
    init {
        loadHistory()
        refreshCurrentTab()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            settingsDataStore.searchHistory.collect { history ->
                updateState { it.copy(searchHistory = history) }
            }
        }
    }

    private fun saveHistory(history: List<String>) {
        viewModelScope.launch {
            settingsDataStore.setSearchHistory(history)
        }
    }

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        val currentHistory = currentState.searchHistory.toMutableList()
        currentHistory.remove(query)
        currentHistory.add(0, query)
        if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.lastIndex)
        }
        updateState { it.copy(searchHistory = currentHistory) }
        saveHistory(currentHistory)
    }

    fun removeFromHistory(query: String) {
        val currentHistory = currentState.searchHistory.toMutableList()
        currentHistory.remove(query)
        updateState { it.copy(searchHistory = currentHistory) }
        saveHistory(currentHistory)
    }

    fun clearHistory() {
        updateState { it.copy(searchHistory = emptyList()) }
        saveHistory(emptyList())
    }

    fun onQueryChange(query: String) {
        updateState { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            if (query != currentState.lastQuery) {
                updateState { it.copy(cachedData = emptyMap(), lastQuery = query) }
            }
            performSearch(query)
        }
    }

    fun onActiveChange(active: Boolean) {
        updateState { it.copy(active = active) }
    }

    fun onTabSelected(tab: SearchTab) {
        updateState { it.copy(selectedTab = tab) }
        performSearch(currentState.query)
    }

    fun onSearch(query: String) {
         updateState { it.copy(query = query) }
         addToHistory(query)
         performSearch(query)
    }

    fun clearSearch() {
        updateState { it.copy(query = "") }
        performSearch("")
    }

    private fun performSearch(query: String) {
        refreshCurrentTab(query)
    }

    fun refreshCurrentTab(query: String = currentState.query) {
        val currentTab = currentState.selectedTab
        val cached = currentState.cachedData[currentTab]
        
        if (cached != null && query == currentState.lastQuery) {
            when (currentTab) {
                SearchTab.PEOPLE -> updateState { it.copy(accounts = cached as List<SearchAccount>, isLoading = false) }
                SearchTab.TRENDING, SearchTab.LISTS -> updateState { it.copy(hashtags = cached as List<SearchHashtag>, isLoading = false) }
                SearchTab.NEWS, SearchTab.SPORTS, SearchTab.ENTERTAINMENT -> updateState { it.copy(news = cached as List<SearchNews>, isLoading = false) }
                SearchTab.FOR_YOU, SearchTab.TOP, SearchTab.LATEST, SearchTab.MEDIA -> updateState { it.copy(posts = cached as List<com.synapse.social.studioasinc.domain.model.Post>, isLoading = false) }
            }
            return
        }
        
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            try {
                when (currentTab) {
                    SearchTab.PEOPLE -> {
                        val result = getSuggestedAccountsUseCase(query)
                        val currentUserId = authRepository.getCurrentUserId()
                        result.onSuccess { data ->
                            val filtered = data.filter { it.id != currentUserId }
                            updateState {
                                it.copy(
                                    accounts = filtered, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to filtered)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.TRENDING, SearchTab.LISTS -> {
                        val result = searchHashtagsUseCase(if (currentTab == SearchTab.TRENDING) "" else query)
                        result.onSuccess { data -> 
                            updateState {
                                it.copy(
                                    hashtags = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.NEWS -> {
                        val result = searchNewsUseCase(query)
                        result.onSuccess { data -> 
                            updateState {
                                it.copy(
                                    news = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.SPORTS -> {
                        val result = searchNewsUseCase("$query sports")
                        result.onSuccess { data -> 
                            updateState {
                                it.copy(
                                    news = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.ENTERTAINMENT -> {
                        val result = searchNewsUseCase("$query entertainment")
                        result.onSuccess { data -> 
                            updateState {
                                it.copy(
                                    news = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.FOR_YOU, SearchTab.TOP, SearchTab.LATEST, SearchTab.MEDIA -> {
                        val result = searchPostsUseCase(query)
                        result.onSuccess { data ->
                            val posts = data.map { searchPost ->
                                com.synapse.social.studioasinc.domain.model.Post(
                                    id = searchPost.id,
                                    authorUid = searchPost.authorId,
                                    postText = searchPost.content,
                                    publishDate = searchPost.createdAt,
                                    timestamp = 0L, // Will be parsed if needed, but 0 is safe for now
                                    likesCount = searchPost.likesCount,
                                    replyCount = searchPost.commentsCount,
                                    resharesCount = searchPost.boostCount,
                                    username = searchPost.authorHandle,
                                    avatarUrl = searchPost.authorAvatar,
                                    mediaItems = searchPost.mediaUrls?.map { url ->
                                        com.synapse.social.studioasinc.domain.model.MediaItem(id = url, url = url, type = com.synapse.social.studioasinc.domain.model.MediaType.IMAGE)
                                    }?.toMutableList() ?: mutableListOf()
                                )
                            }
                            
                            val enrichedPosts = populatePostReactionsUseCase(posts)
                            val fullyEnrichedPosts = populatePostPollsUseCase(enrichedPosts)

                            val filtered = when (currentTab) {
                                SearchTab.FOR_YOU, SearchTab.TOP -> fullyEnrichedPosts.sortedByDescending { it.likesCount + it.replyCount + it.resharesCount }
                                SearchTab.LATEST -> fullyEnrichedPosts.sortedByDescending { it.publishDate }
                                SearchTab.MEDIA -> fullyEnrichedPosts.filter { it.mediaItems?.isNotEmpty() == true }
                                else -> fullyEnrichedPosts
                            }
                            updateState {
                                it.copy(
                                    posts = filtered, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to filtered)
                                )
                            }
                        }
                        result.onFailure { err -> updateState { it.copy(error = err.message, isLoading = false) } }
                    }
                }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleFollow(accountId: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            val account = currentState.accounts.find { it.id == accountId } ?: return@launch

            val result = if (account.isFollowing) {
                unfollowUserUseCase(currentUserId, accountId)
            } else {
                followUserUseCase(currentUserId, accountId)
            }

            result.onSuccess {
                updateState { state ->
                    state.copy(
                        accounts = state.accounts.map { acc ->
                            if (acc.id == accountId) acc.copy(isFollowing = !acc.isFollowing) else acc
                        }
                    )
                }
            }.onFailure { err ->
                updateState { it.copy(error = err.message) }
            }
        }
    }


    fun reactToPost(post: com.synapse.social.studioasinc.domain.model.Post, reactionType: com.synapse.social.studioasinc.domain.model.ReactionType) {
        viewModelScope.launch {
            reactToPostUseCase(post, reactionType).collect { result ->
                result.onSuccess { updatedPost ->
                    updateState { state ->
                        state.copy(posts = state.posts.map { if (it.id == updatedPost.id) updatedPost else it })
                    }
                }.onFailure { err -> updateState { it.copy(error = err.message) } }
            }
        }
    }

    fun likePost(post: com.synapse.social.studioasinc.domain.model.Post) {
        reactToPost(post, com.synapse.social.studioasinc.domain.model.ReactionType.LIKE)
    }

    fun bookmarkPost(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            bookmarkPostUseCase(post.id, userId, post.isBookmarked == true).collect { result ->
                result.onFailure { err -> updateState { it.copy(error = err.message) } }
            }
        }
    }

    fun sharePost(post: com.synapse.social.studioasinc.domain.model.Post) {

    }

    fun votePoll(post: com.synapse.social.studioasinc.domain.model.Post, optionIndex: Int) {
        viewModelScope.launch {
            votePollUseCase(post, optionIndex).collect { result ->
                result.onSuccess { updatedPost ->
                    updateState { state ->
                        state.copy(posts = state.posts.map { if (it.id == updatedPost.id) updatedPost else it })
                    }
                }.onFailure { err -> updateState { it.copy(error = err.message) } }
            }
        }
    }

    fun deletePost(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            deletePostUseCase(post.id).collect { result ->
                result.onSuccess {
                    updateState { state -> state.copy(posts = state.posts.filter { it.id != post.id }) }
                }.onFailure { err -> updateState { it.copy(error = err.message) } }
            }
        }
    }

    fun copyPostLink(post: com.synapse.social.studioasinc.domain.model.Post) {

    }

    fun toggleComments(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            togglePostCommentsUseCase(post.id).collect { result ->
                result.onFailure { err -> updateState { it.copy(error = err.message) } }
            }
        }
    }

    fun reportPost(post: com.synapse.social.studioasinc.domain.model.Post) {

    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            updateState { it.copy(blockSuccess = false, blockError = null) }
            
            blockUserUseCase(userId)
                .onSuccess {
                    updateState { it.copy(blockSuccess = true) }
                    performSearch(currentState.query)
                }
                .onFailure { error ->
                    updateState { it.copy(blockError = error.message ?: "Failed to block user") }
                }
        }
    }
    
    fun clearBlockStatus() {
        updateState { it.copy(blockSuccess = false, blockError = null) }
    }

    fun revokeVote(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            revokeVoteUseCase(post).collect { result ->
                result.onSuccess { updatedPost ->
                    updateState { state ->
                        state.copy(posts = state.posts.map { if (it.id == updatedPost.id) updatedPost else it })
                    }
                }.onFailure { err -> updateState { it.copy(error = err.message) } }
            }
        }
    }

    fun isPostOwner(post: com.synapse.social.studioasinc.domain.model.Post): Boolean {
        return authRepository.getCurrentUserId() == post.authorUid
    }

    fun areCommentsDisabled(post: com.synapse.social.studioasinc.domain.model.Post): Boolean {
        return post.postDisableReplies == "true"
    }

    fun summarizePost(post: com.synapse.social.studioasinc.domain.model.Post) {
        val content = post.postText.orEmpty().trim()
        if (content.isBlank()) {
            updateState { it.copy(summaryError = "No text content to summarize.") }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isSummarizing = true, postSummary = null, summaryError = null) }
            summarizePostUseCase(content, emptyList())
                .onSuccess { summary -> updateState { it.copy(isSummarizing = false, postSummary = summary) } }
                .onFailure { e -> updateState { it.copy(isSummarizing = false, summaryError = e.message ?: "Failed to summarize post.") } }
        }
    }

    fun clearPostSummary() {
        updateState { it.copy(postSummary = null, summaryError = null) }
    }
}
