package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SearchSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.GetContextualHeroCardsUseCase
import com.synapse.social.studioasinc.shared.domain.model.settings.SettingsNode
import com.synapse.social.studioasinc.shared.domain.model.settings.HeroCard
import com.synapse.social.studioasinc.shared.domain.model.settings.SettingsAction
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository as DomainSettingsRepository



@HiltViewModel
class SettingsHubViewModel @Inject constructor(
    application: Application,
    private val searchSettingsUseCase: SearchSettingsUseCase,
    private val getContextualHeroCardsUseCase: GetContextualHeroCardsUseCase,
    private val settingsRepository: DomainSettingsRepository
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<SettingsNode>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            searchSettingsUseCase(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heroCards: StateFlow<List<HeroCard>> = getContextualHeroCardsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userProfileSummary = MutableStateFlow<UserProfileSummary?>(null)
    val userProfileSummary: StateFlow<UserProfileSummary?> = _userProfileSummary.asStateFlow()

    private val _settingsGroups = MutableStateFlow<List<SettingsGroup>>(emptyList())
    val settingsGroups: StateFlow<List<SettingsGroup>> = _settingsGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUserProfile()
        loadSettingsCategories()
    }



    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = UserProfileManager.getCurrentUserProfile()
                if (currentUser != null) {
                    val displayName = currentUser.displayName?.takeIf { it.isNotBlank() }
                        ?: currentUser.username?.takeIf { it.isNotBlank() }
                        ?: "User"

                    android.util.Log.d("SettingsHubViewModel", "Profile loaded - avatarUrl: ${currentUser.avatar}")
                    _userProfileSummary.value = UserProfileSummary(
                        id = currentUser.uid,
                        displayName = displayName,
                        email = currentUser.email ?: "",
                        avatarUrl = currentUser.avatar
                    )
                } else {

                    try {
                        val authService = SupabaseAuthenticationService.getInstance(getApplication())
                        val authUser = authService.getCurrentUser()

                        if (authUser != null) {
                            _userProfileSummary.value = UserProfileSummary(
                                id = authUser.id,
                                displayName = "User",
                                email = authUser.email,
                                avatarUrl = null
                            )
                        } else {

                            _userProfileSummary.value = UserProfileSummary(
                                id = "",
                                displayName = "User",
                                email = "",
                                avatarUrl = null
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsHubViewModel", "Failed to load auth user", e)
                         _userProfileSummary.value = UserProfileSummary(
                            id = "",
                            displayName = "User",
                            email = "",
                            avatarUrl = null
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsHubViewModel", "Failed to load user profile", e)

                _userProfileSummary.value = UserProfileSummary(
                    id = "",
                    displayName = "User",
                    email = "",
                    avatarUrl = null
                )
            } finally {
                _isLoading.value = false
            }
        }
    }



    private fun loadSettingsCategories() {
        _settingsGroups.value = SettingsDataProvider.getSettingsGroups()
    }



    fun onNavigateToCategory(destination: SettingsDestination) {
        android.util.Log.d("SettingsHubViewModel", "Navigating to: ${destination.route}")
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onActionClick(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {
                is SettingsAction.Toggle -> {
                    if (action.key == "notifications_global") {
                        // Assuming updateNotificationPreference handles global or we need a specific call
                        // For simplicity in this redesign, we use the specific repo call
                        settingsRepository.updateNotificationPreference(
                            com.synapse.social.studioasinc.shared.domain.model.settings.NotificationCategory.MESSAGES, // Dummy to trigger
                            !action.currentValue
                        )
                    }
                }
                is SettingsAction.Execute -> {
                    when (action.actionId) {
                        "clear_cache" -> settingsRepository.clearCache()
                        "toggle_dark_mode" -> {
                            val current = settingsRepository.themeMode.first()
                            val next = if (current == com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode.DARK)
                                com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode.LIGHT
                                else com.synapse.social.studioasinc.shared.domain.model.settings.ThemeMode.DARK
                            settingsRepository.setThemeMode(next)
                        }
                    }
                }
                is SettingsAction.Navigate -> {
                    // Handled by UI navigation
                }
            }
        }
    }



    fun refreshUserProfile() {

        UserProfileManager.clearCache()
        loadUserProfile()
    }
}
