package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.UiText
import com.synapse.social.studioasinc.domain.model.release.AppRelease
import com.synapse.social.studioasinc.domain.model.release.ReleaseFeedSource
import com.synapse.social.studioasinc.domain.model.release.ReleaseRateLimitedException
import com.synapse.social.studioasinc.domain.model.release.ReleaseVersion
import com.synapse.social.studioasinc.domain.usecase.release.GetReleaseFeedUseCase
import com.synapse.social.studioasinc.domain.usecase.release.MarkReleaseSeenUseCase
import com.synapse.social.studioasinc.shared.domain.exception.NetworkUnavailableException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

/** Immutable presentation state for the What's New screen. */
data class WhatsNewUiState(
    val releases: List<AppRelease> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false,
    val unreadTag: String? = null,
    val expandedTags: Set<String> = emptySet(),
    val errorMessage: UiText? = null
) {
    /** Newest release, shown as the highlight of the screen. */
    val latest: AppRelease? get() = releases.firstOrNull()

    /** Everything older than the highlight, in the order the repository published it. */
    val history: List<AppRelease> get() = releases.drop(1)
}

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val getReleaseFeedUseCase: GetReleaseFeedUseCase,
    private val markReleaseSeenUseCase: MarkReleaseSeenUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(WhatsNewUiState())
    val uiState: StateFlow<WhatsNewUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = false)
    }

    fun refresh() {
        load(forceRefresh = true)
    }

    fun retry() {
        load(forceRefresh = true)
    }

    fun toggleRelease(tag: String) {
        _uiState.update { state ->
            val expanded = if (tag in state.expandedTags) {
                state.expandedTags - tag
            } else {
                state.expandedTags + tag
            }
            state.copy(expandedTags = expanded)
        }
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                if (forceRefresh) it.copy(isRefreshing = true) else it.copy(isLoading = true)
            }
            val result = try {
                getReleaseFeedUseCase(BuildConfig.VERSION_NAME, forceRefresh)
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
            result.fold(
                onSuccess = { feed -> acknowledgeNewest(feed.releases, feed.unreadTag, feed.source) },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = failure.toUiText()
                        )
                    }
                }
            )
        }
    }

    /**
     * Renders the feed and then records what this device has now seen.
     *
     * Acknowledgement happens only for a feed that is actually on screen, so a release that never
     * reached the user keeps its unread flag; the newest version in the list is stored because the
     * screen shows every release down to it, not just the one that carried the badge.
     */
    private suspend fun acknowledgeNewest(
        releases: List<AppRelease>,
        unreadTag: String?,
        source: ReleaseFeedSource
    ) {
        val newestSeen = releases.mapNotNull { release -> ReleaseVersion.of(release.tag) }.maxOrNull()?.raw
        _uiState.update {
            it.copy(
                releases = releases,
                isLoading = false,
                isRefreshing = false,
                isStale = source == ReleaseFeedSource.CacheFallback,
                unreadTag = unreadTag,
                errorMessage = null
            )
        }
        if (unreadTag != null && newestSeen != null) {
            markReleaseSeenUseCase(newestSeen)
            _uiState.update { state -> state.copy(unreadTag = null) }
        }
    }
}

/** Maps a repository failure onto the message the screen shows. */
private fun Throwable.toUiText(): UiText = when (this) {
    is ReleaseRateLimitedException -> UiText.StringResource(R.string.whats_new_error_rate_limited)
    is NetworkUnavailableException -> UiText.StringResource(R.string.whats_new_error_offline)
    is IOException -> UiText.StringResource(R.string.whats_new_error_offline)
    is SerializationException, is IllegalArgumentException ->
        UiText.StringResource(R.string.whats_new_error_unexpected)
    else -> UiText.StringResource(R.string.whats_new_error_unexpected)
}
