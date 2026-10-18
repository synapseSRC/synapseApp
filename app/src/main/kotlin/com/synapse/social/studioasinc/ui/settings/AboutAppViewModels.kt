package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Immutable presentation state for the What's New screen. */
data class WhatsNewUiState(val expandedLatest: Boolean = false)

@HiltViewModel
class WhatsNewViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(WhatsNewUiState())
    val uiState: StateFlow<WhatsNewUiState> = _uiState.asStateFlow()

    fun toggleLatestRelease() {
        _uiState.update { state -> state.copy(expandedLatest = !state.expandedLatest) }
    }
}

/** Searchable license row data rendered with localized labels by the UI. */
internal data class AboutLicenseEntry(
    val name: String,
    val licenseAndAuthor: String,
    val author: String,
    val repositoryUrl: String
)

/** Immutable state and filtering behavior for the open-source library list. */
data class AboutOpenSourceUiState(
    val query: String = "",
    val sortByAuthor: Boolean = false
)

@HiltViewModel
class AboutOpenSourceViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AboutOpenSourceUiState())
    val uiState: StateFlow<AboutOpenSourceUiState> = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun setSortByAuthor(enabled: Boolean) {
        _uiState.update { it.copy(sortByAuthor = enabled) }
    }

    internal fun visibleEntries(entries: List<AboutLicenseEntry>): List<AboutLicenseEntry> {
        val state = _uiState.value
        val filtered = entries.filter { entry ->
            entry.name.contains(state.query, ignoreCase = true) ||
                entry.licenseAndAuthor.contains(state.query, ignoreCase = true)
        }
        return if (state.sortByAuthor) filtered.sortedBy { it.author } else filtered
    }
}
