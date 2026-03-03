package com.synapse.social.studioasinc.ui.search

import com.synapse.social.studioasinc.data.repository.ProfileActionRepository
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.usecase.profile.FollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.UnfollowUserUseCase
import com.synapse.social.studioasinc.shared.domain.model.SearchAccount
import com.synapse.social.studioasinc.shared.domain.model.SearchHashtag
import com.synapse.social.studioasinc.shared.domain.model.SearchNews
import com.synapse.social.studioasinc.shared.domain.model.SearchPost
import com.synapse.social.studioasinc.shared.domain.usecase.search.GetSuggestedAccountsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchHashtagsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchNewsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val posts: List<SearchPost> = emptyList(),
    val hashtags: List<SearchHashtag> = emptyList(),
    val news: List<SearchNews> = emptyList(),
    val accounts: List<SearchAccount> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val lastQuery: String = "",
    val cachedData: Map<SearchTab, Any> = emptyMap()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchPostsUseCase: SearchPostsUseCase,
    private val searchHashtagsUseCase: SearchHashtagsUseCase,
    private val searchNewsUseCase: SearchNewsUseCase,
    private val getSuggestedAccountsUseCase: GetSuggestedAccountsUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val authRepository: AuthRepository,
    private val sharedPreferences: SharedPreferences,
    private val postRepository: com.synapse.social.studioasinc.data.repository.PostRepository,
    private val bookmarkRepository: com.synapse.social.studioasinc.data.repository.BookmarkRepository,
    private val pollRepository: com.synapse.social.studioasinc.data.repository.PollRepository,
    private val profileActionRepository: com.synapse.social.studioasinc.data.repository.ProfileActionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val HISTORY_KEY = "search_history"

    init {
        loadHistory()

        refreshCurrentTab()
    }

    private fun loadHistory() {
        val historyString = sharedPreferences.getString(HISTORY_KEY, "") ?: ""
        val historyList = if (historyString.isNotEmpty()) {
            historyString.split(",").filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        _uiState.update { it.copy(searchHistory = historyList) }
    }

    private fun saveHistory(history: List<String>) {
        val historyString = history.joinToString(",")
        sharedPreferences.edit().putString(HISTORY_KEY, historyString).apply()
    }

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        val currentHistory = uiState.value.searchHistory.toMutableList()
        currentHistory.remove(query)
        currentHistory.add(0, query)
        if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.lastIndex)
        }
        _uiState.update { it.copy(searchHistory = currentHistory) }
        saveHistory(currentHistory)
    }

    fun removeFromHistory(query: String) {
        val currentHistory = uiState.value.searchHistory.toMutableList()
        currentHistory.remove(query)
        _uiState.update { it.copy(searchHistory = currentHistory) }
        saveHistory(currentHistory)
    }

    fun clearHistory() {
        _uiState.update { it.copy(searchHistory = emptyList()) }
        saveHistory(emptyList())
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            if (query != uiState.value.lastQuery) {
                _uiState.update { it.copy(cachedData = emptyMap(), lastQuery = query) }
            }
            performSearch(query)
        }
    }

    fun onActiveChange(active: Boolean) {
        _uiState.update { it.copy(active = active) }
    }

    fun onTabSelected(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        performSearch(uiState.value.query)
    }

    fun onSearch(query: String) {
         _uiState.update { it.copy(query = query) }
         addToHistory(query)
         performSearch(query)
    }

    fun clearSearch() {
        _uiState.update { it.copy(query = "") }
        performSearch("")
    }

    private fun performSearch(query: String) {
        refreshCurrentTab(query)
    }

    fun refreshCurrentTab(query: String = uiState.value.query) {
        val currentTab = uiState.value.selectedTab
        val cached = uiState.value.cachedData[currentTab]
        
        if (cached != null && query == uiState.value.lastQuery) {
            when (currentTab) {
                SearchTab.FOR_YOU, SearchTab.PEOPLE -> _uiState.update { it.copy(accounts = cached as List<SearchAccount>, isLoading = false) }
                SearchTab.TRENDING, SearchTab.LISTS -> _uiState.update { it.copy(hashtags = cached as List<SearchHashtag>, isLoading = false) }
                SearchTab.NEWS, SearchTab.SPORTS, SearchTab.ENTERTAINMENT -> _uiState.update { it.copy(news = cached as List<SearchNews>, isLoading = false) }
                SearchTab.TOP, SearchTab.LATEST, SearchTab.MEDIA -> _uiState.update { it.copy(posts = cached as List<SearchPost>, isLoading = false) }
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                when (currentTab) {
                    SearchTab.FOR_YOU, SearchTab.PEOPLE -> {
                        val result = getSuggestedAccountsUseCase(query)
                        val currentUserId = authRepository.getCurrentUserId()
                        result.onSuccess { data ->
                            val filtered = data.filter { it.id != currentUserId }
                            _uiState.update { 
                                it.copy(
                                    accounts = filtered, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to filtered)
                                )
                            }
                        }
                        result.onFailure { err -> _uiState.update { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.TRENDING, SearchTab.LISTS -> {
                        val result = searchHashtagsUseCase(if (currentTab == SearchTab.TRENDING) "" else query)
                        result.onSuccess { data -> 
                            _uiState.update { 
                                it.copy(
                                    hashtags = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> _uiState.update { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.NEWS -> {
                        val result = searchNewsUseCase(query)
                        result.onSuccess { data -> 
                            _uiState.update { 
                                it.copy(
                                    news = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> _uiState.update { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.SPORTS -> {
                        val result = searchNewsUseCase("$query sports")
                        result.onSuccess { data -> 
                            _uiState.update { 
                                it.copy(
                                    news = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> _uiState.update { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.ENTERTAINMENT -> {
                        val result = searchNewsUseCase("$query entertainment")
                        result.onSuccess { data -> 
                            _uiState.update { 
                                it.copy(
                                    news = data, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to data)
                                )
                            }
                        }
                        result.onFailure { err -> _uiState.update { it.copy(error = err.message, isLoading = false) } }
                    }
                    SearchTab.TOP, SearchTab.LATEST, SearchTab.MEDIA -> {
                        val result = searchPostsUseCase(query)
                        result.onSuccess { data ->
                            val filtered = when (currentTab) {
                                SearchTab.TOP -> data.sortedByDescending { it.likesCount + it.commentsCount + it.boostCount }
                                SearchTab.LATEST -> data.sortedByDescending { it.createdAt }
                                SearchTab.MEDIA -> data
                                else -> data
                            }
                            _uiState.update { 
                                it.copy(
                                    posts = filtered, 
                                    isLoading = false,
                                    cachedData = it.cachedData + (currentTab to filtered)
                                )
                            }
                        }
                        result.onFailure { err -> _uiState.update { it.copy(error = err.message, isLoading = false) } }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleFollow(accountId: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            val account = uiState.value.accounts.find { it.id == accountId } ?: return@launch

            val result = if (account.isFollowing) {
                unfollowUserUseCase(currentUserId, accountId)
            } else {
                followUserUseCase(currentUserId, accountId)
            }

            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        accounts = state.accounts.map { acc ->
                            if (acc.id == accountId) acc.copy(isFollowing = !acc.isFollowing) else acc
                        }
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message) }
            }
        }
    }


    fun reactToPost(post: com.synapse.social.studioasinc.domain.model.Post, reactionType: com.synapse.social.studioasinc.domain.model.ReactionType) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            postRepository.toggleReaction(post.id, userId, reactionType, post.userReaction, skipCheck = true)
                .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }

    fun likePost(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            postRepository.toggleReaction(post.id, userId, com.synapse.social.studioasinc.domain.model.ReactionType.LIKE, post.userReaction, skipCheck = true)
                .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }

    fun bookmarkPost(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            bookmarkRepository.toggleBookmark(post.id)
                .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }

    fun sharePost(post: com.synapse.social.studioasinc.domain.model.Post) {

    }

    fun votePoll(post: com.synapse.social.studioasinc.domain.model.Post, optionIndex: Int) {
        viewModelScope.launch {
            pollRepository.submitVote(post.id, optionIndex)
                .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }

    fun deletePost(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            postRepository.deletePost(post.id)
                .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }

    fun copyPostLink(post: com.synapse.social.studioasinc.domain.model.Post) {

    }

    fun toggleComments(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            postRepository.toggleComments(post.id)
                .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }

    fun reportPost(post: com.synapse.social.studioasinc.domain.model.Post) {

    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
             val currentUserId = authRepository.getCurrentUserId() ?: return@launch
             profileActionRepository.blockUser(currentUserId, userId)
                 .onSuccess {
                     performSearch(uiState.value.query)
                 }
                 .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }

    fun revokeVote(post: com.synapse.social.studioasinc.domain.model.Post) {
        viewModelScope.launch {
            pollRepository.revokeVote(post.id)
                .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }

    fun isPostOwner(post: com.synapse.social.studioasinc.domain.model.Post): Boolean {
        return authRepository.getCurrentUserId() == post.authorUid
    }

    fun areCommentsDisabled(post: com.synapse.social.studioasinc.domain.model.Post): Boolean {
        return post.postDisableComments == "true"
    }
}
