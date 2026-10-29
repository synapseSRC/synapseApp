package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.OpenSourceLicense
import com.synapse.social.studioasinc.domain.usecase.about.GetOpenSourceLicensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Immutable state and filtering behavior for the open-source library list. */
data class AboutOpenSourceUiState(
    val entries: List<OpenSourceLicense> = emptyList(),
    val query: String = "",
    val sortByAuthor: Boolean = false,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false
)

@HiltViewModel
class AboutOpenSourceViewModel @Inject constructor(
    private val getOpenSourceLicensesUseCase: GetOpenSourceLicensesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AboutOpenSourceUiState())
    val uiState: StateFlow<AboutOpenSourceUiState> = _uiState.asStateFlow()

    init {
        loadLicenses()
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun setSortByAuthor(enabled: Boolean) {
        _uiState.update { it.copy(sortByAuthor = enabled) }
    }

    /** Entries matching the current query, ordered by whichever sort the menu selected. */
    fun visibleEntries(): List<OpenSourceLicense> {
        val state = _uiState.value
        val query = state.query.trim()
        val filtered = if (query.isEmpty()) {
            state.entries
        } else {
            state.entries.filter { entry ->
                entry.name.contains(query, ignoreCase = true) ||
                    entry.author.contains(query, ignoreCase = true) ||
                    entry.licenseId.contains(query, ignoreCase = true) ||
                    entry.licenseNote?.contains(query, ignoreCase = true) == true
            }
        }
        return if (state.sortByAuthor) {
            filtered.sortedWith { left, right -> left.author.compareTo(right.author, ignoreCase = true) }
        } else {
            filtered.sortedWith { left, right -> left.name.compareTo(right.name, ignoreCase = true) }
        }
    }

    private fun loadLicenses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            val result = try {
                getOpenSourceLicensesUseCase()
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
            result.fold(
                onSuccess = { licenses ->
                    _uiState.update { it.copy(entries = licenses, isLoading = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(entries = emptyList(), isLoading = false, loadFailed = true) }
                }
            )
        }
    }
}
